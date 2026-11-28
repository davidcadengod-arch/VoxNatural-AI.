package com.voxnatural.app
import android.app.*
import android.content.Intent
import android.os.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.*
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ConvertService:Service(){
 private var tts:TextToSpeech?=null
 private var busy=false
 private var lock:PowerManager.WakeLock?=null
 override fun onBind(i:Intent?)=null
 private fun report(s:String){getSharedPreferences("jobs",0).edit().putString("status",s).apply()}
 override fun onStartCommand(i:Intent?,flags:Int,startId:Int):Int{
  if(busy)return START_NOT_STICKY
  val id=i?.getStringExtra("id")?:return START_NOT_STICKY
  if(!id.matches(Regex("[a-f0-9-]{36}"))){stopSelf();return START_NOT_STICKY}
  busy=true
  val nm=getSystemService(NotificationManager::class.java)
  nm.createNotificationChannel(NotificationChannel("convert","Conversión de audio",NotificationManager.IMPORTANCE_LOW))
  startForeground(1,Notification.Builder(this,"convert").setContentTitle("VoxNatural AI").setContentText("Convirtiendo documento").setSmallIcon(android.R.drawable.ic_media_play).build())
  lock=getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"VoxNatural:convert").apply{acquire(6*60*60*1000L)}
  val folder=File(filesDir,"library/"+id)
  report("Iniciando motor local")
  val sid=i?.getIntExtra("sid",-1)?:-1
  if(sid>=0){Thread{convertNeural(folder,sid)}.start();return START_NOT_STICKY}
  tts=TextToSpeech(this){status->
   if(status!=TextToSpeech.SUCCESS){report("Motor no disponible");stopSelf()}
   else Thread{convert(folder)}.start()
  }
  return START_NOT_STICKY
 }
 private fun finishAudio(folder:File,sid:Int){
  val wav=File(folder,"audio-"+sid+".wav"); val mp3=File(folder,"audio-"+sid+".mp3")
  mp3.delete()
  report("Codificando MP3")
  try{Mp3Encoder.encode(wav,mp3);report("Conversión completada · MP3 y WAV")}catch(e:Throwable){report("WAV listo; MP3 no disponible: "+e.message)}
 }
 private fun convert(folder:File){
  try{
   val engine=tts?:error("Motor no disponible")
   val v=engine.voices.filter{it.locale.language=="es" && !it.isNetworkConnectionRequired}.maxByOrNull{it.quality}?:error("Instala una voz española sin conexión en Ajustes de síntesis de voz")
   require(engine.setVoice(v)==TextToSpeech.SUCCESS){"No se pudo seleccionar la voz"}
   val text=File(folder,"text").readText()
   val chunks=mutableListOf<String>()
   var pos=0
   val limit=minOf(3000,TextToSpeech.getMaxSpeechInputLength())
   while(pos<text.length){
    var end=minOf(pos+limit,text.length)
    if(end<text.length){val cut=text.lastIndexOf(' ',end);if(cut>pos)end=cut}
    if(end<text.length && Character.isHighSurrogate(text[end-1]))end--
    chunks.add(text.substring(pos,end));pos=end
   }
   val parts=File(folder,"parts").apply{mkdirs()}
   val signature=File(parts,"signature")
   val key=v.name+"|"+text.hashCode()+"|"+limit
   if(!signature.isFile || signature.readText()!=key){parts.listFiles()?.forEach{it.delete()};signature.writeText(key)}
   for((index,chunk) in chunks.withIndex()){
    report("Fragmento "+(index+1)+" de "+chunks.size)
    val file=File(parts,index.toString()+".wav")
    val completed=File(parts,index.toString()+".done")
    if(completed.isFile && file.length()>44)continue
    completed.delete()
    file.delete()
    val latch=CountDownLatch(1);var ok=false
    engine.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
     override fun onStart(id:String?){}
     override fun onDone(id:String?){ok=true;latch.countDown()}
     override fun onError(id:String?){latch.countDown()}
    })
    require(engine.synthesizeToFile(chunk,Bundle(),file,index.toString())==TextToSpeech.SUCCESS){"No se pudo iniciar síntesis"}
    require(latch.await(10,TimeUnit.MINUTES)&&ok && file.length()>44){"Error de síntesis"}
    completed.writeText("ok")
   }
   merge(chunks.indices.map{File(parts,it.toString()+".wav")},File(folder,"audio--1.wav"))
   parts.deleteRecursively()
   finishAudio(folder,-1)
  }catch(e:Exception){report("Error: "+e.message)}
  finally{stopSelf()}
 }
 private fun convertNeural(folder:File,sid:Int){
  try{
   val text=File(folder,"text").readText()
   val normalized=text.replace("\r\n","\n").replace('\r','\n')
   val chunks=mutableListOf<String>()
   val breaks=java.text.BreakIterator.getSentenceInstance(Locale("es"))
   for(paragraph in normalized.split(Regex("\\n\\s*\\n"))){
    val clean=paragraph.replace(Regex("\\s+")," ").trim()
    if(clean.isEmpty())continue
    breaks.setText(clean)
    var begin=breaks.first()
    var finish=breaks.next()
    while(finish!=java.text.BreakIterator.DONE){
     var pos=begin
     while(pos<finish){
      var end=minOf(pos+240,finish)
      if(end<finish){
       val clause=clean.lastIndexOfAny(charArrayOf(';',':',','),end-1)
       val space=clean.lastIndexOf(' ',end-1)
       if(clause>pos+100)end=clause+1 else if(space>pos)end=space+1
       if(Character.isHighSurrogate(clean[end-1]))end--
      }
      val part=clean.substring(pos,end).trim()
      if(part.isNotEmpty())chunks.add(part)
      pos=end
     }
     begin=finish;finish=breaks.next()
    }
   }
   require(chunks.isNotEmpty())
   val parts=File(folder,"neural-parts-"+sid+"-v3").apply{mkdirs()}
   val signature=File(parts,"signature")
   val key=java.security.MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it.toInt() and 255)}
   if(!signature.isFile || signature.readText()!=key){parts.listFiles()?.forEach{it.delete()};signature.writeText(key)}
   NeuralVoice(this,sid).use{voice->
    for((index,chunk) in chunks.withIndex()){
     report((if(sid==100)"Piper: " else "Kokoro: ")+(index+1)+" de "+chunks.size)
     val f=File(parts,index.toString()+".wav")
     val done=File(parts,index.toString()+".done")
     if(done.isFile && f.length()>44)continue
     done.delete();f.delete()
     voice.generate(chunk,f)
     require(f.length()>44);done.writeText("ok")
    }
   }
   merge(chunks.indices.map{File(parts,it.toString()+".wav")},File(folder,"audio-"+sid+".wav"))
   parts.deleteRecursively();finishAudio(folder,sid)
  }catch(e:Throwable){report("Error neuronal: "+e.message)}
  finally{stopSelf()}
 }
 private fun merge(files:List<File>,out:File){
  val tmp=File(out.parentFile,"audio.tmp")
  var format:ByteArray?=null
  var total=0L
  RandomAccessFile(tmp,"rw").use{dst->
   dst.setLength(0);dst.write(ByteArray(44))
   for(f in files)RandomAccessFile(f,"r").use{src->
    fun tag():String{val a=ByteArray(4);src.readFully(a);return String(a,Charsets.US_ASCII)}
    require(tag()=="RIFF");src.skipBytes(4);require(tag()=="WAVE")
    var found=false
    while(src.filePointer+8<=src.length()){
     val type=tag(); val n=Integer.reverseBytes(src.readInt()).toLong() and 0xffffffffL
     val next=src.filePointer+n+(n%2)
     require(src.filePointer+n<=src.length())
     if(type=="fmt "){
      require(n>=16);val a=ByteArray(16);src.readFully(a)
      require(a[0].toInt()==1 && a[1].toInt()==0){"Formato WAV no PCM"}
      if(format==null)format=a else require(format!!.contentEquals(a)){"Formatos diferentes"}
     }else if(type=="data"){
      require(format!=null);val buf=ByteArray(65536);var left=n
      while(left>0){val count=minOf(left,buf.size.toLong()).toInt();src.readFully(buf,0,count);dst.write(buf,0,count);left-=count}
      total+=n;found=true
     };src.seek(next)
    };require(found)
   }
   require(total<=0xffffffffL-36)
   dst.seek(0);dst.writeBytes("RIFF");dst.writeInt(Integer.reverseBytes((total+36).toInt()));dst.writeBytes("WAVEfmt ");dst.writeInt(Integer.reverseBytes(16));dst.write(format!!);dst.writeBytes("data");dst.writeInt(Integer.reverseBytes(total.toInt()))
  }
  require(tmp.renameTo(out)){"No se pudo guardar WAV"}
 }
 override fun onDestroy(){tts?.shutdown();if(lock?.isHeld==true)lock?.release();super.onDestroy()}
}
