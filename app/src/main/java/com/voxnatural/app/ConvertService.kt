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
   val normalized=java.text.Normalizer.normalize(text,java.text.Normalizer.Form.NFC)
    .replace("\r\n","\n").replace('\r','\n').replace("\u00ad","").replace('\u00a0',' ')
   val chunks=mutableListOf<String>()
   val pauses=mutableListOf<Int>()
   val breaks=java.text.BreakIterator.getSentenceInstance(Locale("es"))
   fun emit(part:String,pause:Int){
    if(part.isNotBlank()){chunks.add(part.trim());pauses.add(pause)}
   }
   for(paragraph in normalized.split(Regex("\\n\\s*\\n"))){
    val clean=paragraph.replace(Regex("\\s+")," ").trim()
    if(clean.isEmpty())continue
    breaks.setText(clean)
    var begin=breaks.first()
    var finish=breaks.next()
    var pending=""
    while(finish!=java.text.BreakIterator.DONE){
     val sentence=clean.substring(begin,finish).trim()
     if(pending.isNotEmpty() && pending.length+sentence.length+1>260){
      emit(pending,100);pending=""
     }
     pending=(pending+" "+sentence).trim()
     while(pending.length>260){
      val clause=pending.lastIndexOfAny(charArrayOf(';',':',','),259)
      val space=pending.lastIndexOf(' ',259)
      var cut=if(clause>120)clause+1 else if(space>0)space+1 else 260
      if(Character.isHighSurrogate(pending[cut-1]))cut--
      emit(pending.substring(0,cut),if(clause>120)70 else 0)
      pending=pending.substring(cut).trim()
     }
     begin=finish;finish=breaks.next()
    }
    if(pending.isNotEmpty())emit(pending,240)
    else if(pauses.isNotEmpty())pauses[pauses.lastIndex]=240
   }
   require(chunks.isNotEmpty())
   val parts=File(folder,"neural-parts-"+sid+"-v7").apply{mkdirs()}
   val signature=File(parts,"signature")
   val key=java.security.MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it.toInt() and 255)}
   if(!signature.isFile || signature.readText()!=key){parts.listFiles()?.forEach{it.delete()};signature.writeText(key)}
   NeuralVoice(this,sid).use{voice->
    for((index,chunk) in chunks.withIndex()){
     report((when(sid){100->"Piper: ";in 200..209->"Supertonic: ";else->"Kokoro: "})+(index+1)+" de "+chunks.size)
     val f=File(parts,index.toString()+".wav")
     val done=File(parts,index.toString()+".done")
     if(done.isFile && validNeuralWav(f))continue
     done.delete();f.delete()
     voice.generate(chunk,f)
     require(validNeuralWav(f)){"Fragmento WAV inválido"};done.writeText("ok")
    }
   }
   merge(chunks.indices.map{File(parts,it.toString()+".wav")},File(folder,"audio-"+sid+".wav"),pauses)
   parts.deleteRecursively();finishAudio(folder,sid)
  }catch(e:Throwable){report("Error neuronal: "+e.message)}
  finally{stopSelf()}
 }
 // Validate cached PCM before reuse; reject truncated or empty data chunks.
 private fun validNeuralWav(file:File):Boolean = try {
  RandomAccessFile(file,"r").use { src ->
   fun tag():String { val a=ByteArray(4);src.readFully(a);return String(a,Charsets.US_ASCII) }
   fun uint():Long = Integer.reverseBytes(src.readInt()).toLong() and 0xffffffffL
   require(tag()=="RIFF")
   val end=uint()+8
   require(end==src.length() && tag()=="WAVE")
   var align=0
   var data=0L
   while(src.filePointer+8<=end){
    val type=tag();val n=uint();val next=src.filePointer+n+(n%2)
    require(next<=end)
    if(type=="fmt "){
     require(n>=16)
     val a=ByteArray(16);src.readFully(a)
     val fmt=java.nio.ByteBuffer.wrap(a).order(java.nio.ByteOrder.LITTLE_ENDIAN)
     require(fmt.getShort(0).toInt()==1 && fmt.getShort(2).toInt()==1 && fmt.getShort(14).toInt()==16)
     require(fmt.getInt(4) in 8000..192000)
     align=fmt.getShort(12).toInt()
     require(align==2)
    }else if(type=="data"){require(align>0 && n>0 && n%align==0L);data+=n}
    src.seek(next)
   }
   src.filePointer==end && data>0 && align==2
  }
 }catch(e:Exception){false}
 private fun merge(files:List<File>,out:File,pauses:List<Int> = emptyList()){
  val tmp=File(out.parentFile,"audio.tmp")
  var format:ByteArray?=null
  var total=0L
  RandomAccessFile(tmp,"rw").use{dst->
   dst.setLength(0);dst.write(ByteArray(44))
   for((index,f) in files.withIndex())RandomAccessFile(f,"r").use{src->
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
    if(index<files.lastIndex && pauses.isNotEmpty()){
     val fmt=java.nio.ByteBuffer.wrap(format!!).order(java.nio.ByteOrder.LITTLE_ENDIAN)
     val rate=fmt.getInt(4);val align=fmt.getShort(12).toInt() and 65535
     require(rate in 8000..192000 && align in 1..32)
     val count=(rate.toLong()*pauses[index]/1000*align).toInt()
     dst.write(ByteArray(count));total+=count
    }
   }
   require(total<=0xffffffffL-36)
   dst.seek(0);dst.writeBytes("RIFF");dst.writeInt(Integer.reverseBytes((total+36).toInt()));dst.writeBytes("WAVEfmt ");dst.writeInt(Integer.reverseBytes(16));dst.write(format!!);dst.writeBytes("data");dst.writeInt(Integer.reverseBytes(total.toInt()))
  }
  require(tmp.renameTo(out)){"No se pudo guardar WAV"}
 }
 override fun onDestroy(){tts?.shutdown();if(lock?.isHeld==true)lock?.release();super.onDestroy()}
}
