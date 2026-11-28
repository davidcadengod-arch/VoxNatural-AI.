package com.voxnatural.app
import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.widget.*
import android.view.ViewGroup
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream
import org.xmlpull.v1.XmlPullParser
import android.util.Xml
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

class MainActivity : Activity() {
 private lateinit var root: LinearLayout
 private lateinit var shell: LinearLayout
 private lateinit var nav: LinearLayout
 private var dark=false
 private fun ink()=Color.parseColor(if(dark)"#F5F1E8" else "#282723")
 private fun paper()=Color.parseColor(if(dark)"#282723" else "#F5F1E8")
 private fun sand()=Color.parseColor(if(dark)"#393630" else "#E5DDD0")
 private val burgundy=Color.parseColor("#743B45")
 private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
 private var player: android.media.MediaPlayer?=null
 private var playingFile:File?=null
 private lateinit var mini:LinearLayout
 private lateinit var miniTitle:TextView
 private lateinit var miniToggle:Button
 private fun titleOf(f:File)=File(f,"title").takeIf{it.isFile}?.readText()?.takeIf{it.isNotBlank()}?:File(f,"name").readText().substringBeforeLast(".")
 private fun authorOf(f:File)=File(f,"author").takeIf{it.isFile}?.readText()?.takeIf{it.isNotBlank()}?:"Autor desconocido"
 private fun editBook(f:File){
  val fields=LinearLayout(this).apply{orientation=1}
  val title=EditText(this).apply{setText(titleOf(f));hint="Título"};fields.addView(title)
  val author=EditText(this).apply{setText(authorOf(f));hint="Autor"};fields.addView(author)
  android.app.AlertDialog.Builder(this).setTitle("Editar libro").setView(fields).setPositiveButton("Guardar"){_,_->File(f,"title").writeText(title.text.toString());File(f,"author").writeText(author.text.toString());render()}.setNegativeButton("Cancelar",null).show()
 }
 private fun playAudio(f:File){
  try{
   savePlayback();player?.release();player=null
   val m=android.media.MediaPlayer();player=m;playingFile=f
   m.setDataSource(f.absolutePath);m.prepare()
   m.seekTo(getPreferences(0).getInt("audio:"+f.absolutePath,0).coerceIn(0,m.duration))
   m.start();updateMini()
  }catch(e:Exception){player?.release();player=null;playingFile=null;toast(e.message?:"No se pudo reproducir");updateMini()}
 }
 private fun savePlayback(){try{val f=playingFile;val m=player;if(f!=null&&m!=null)getPreferences(0).edit().putInt("audio:"+f.absolutePath,m.currentPosition).apply()}catch(_:Exception){}}
 private fun toggleAudio(){try{player?.let{if(it.isPlaying)it.pause() else it.start()};updateMini()}catch(e:Exception){toast("Audio no disponible")}}
 private fun updateMini(){if(!::mini.isInitialized)return;val f=playingFile;val m=player;mini.visibility=if(f==null)android.view.View.GONE else android.view.View.VISIBLE;miniTitle.text=if(f==null)"" else titleOf(f.parentFile!!)+" · "+f.name;miniToggle.text=if(m?.isPlaying==true)"Pausa" else "Reproducir"}
 private fun playerDialog(){
  val m=player?:run{toast("Selecciona un audio de la biblioteca");return}
  val panel=LinearLayout(this).apply{orientation=1;setPadding(dp(16),dp(16),dp(16),dp(16))}
  label(panel,playingFile?.parentFile?.let{titleOf(it)}?:"Audio",23)
  val seek=SeekBar(this).apply{max=m.duration;progress=m.currentPosition;setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar?,p:Int,fromUser:Boolean){if(fromUser)m.seekTo(p)};override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?) {}})};panel.addView(seek)
  button(panel,"Reproducir / pausa"){toggleAudio()}
  button(panel,"Retroceder 15 s"){m.seekTo((m.currentPosition-15000).coerceAtLeast(0));seek.progress=m.currentPosition}
  button(panel,"Avanzar 15 s"){m.seekTo((m.currentPosition+15000).coerceAtMost(m.duration));seek.progress=m.currentPosition}
  android.app.AlertDialog.Builder(this).setTitle("Reproductor").setView(panel).setPositiveButton("Cerrar",null).show()
 }
 private lateinit var status: TextView
 private lateinit var library: LinearLayout
 private lateinit var voice: Spinner
 private lateinit var docs: File
 private var selected: File? = null
 private var exportFile: File? = null
 private val refresh = android.os.Handler(android.os.Looper.getMainLooper())
 private val tick = object : Runnable { override fun run() { showStatus(); refresh.postDelayed(this,1500) } }
 override fun onCreate(b:Bundle?) {
  super.onCreate(b)
  PDFBoxResourceLoader.init(applicationContext)
  docs=File(filesDir,"library").apply { mkdirs() }
  dark=getPreferences(0).getBoolean("dark",false)
  val wide=resources.configuration.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE
  shell=LinearLayout(this).apply{orientation=if(wide)0 else 1;setBackgroundColor(paper())}
  nav=LinearLayout(this).apply{orientation=if(wide)1 else 0;setBackgroundColor(sand());setPadding(dp(8),dp(8),dp(8),dp(8))}
  root=LinearLayout(this).apply { orientation=1; setPadding(dp(20),dp(20),dp(20),dp(20)); setBackgroundColor(paper()) }
  val scroll=ScrollView(this).apply{addView(root)}
  if(wide){shell.addView(nav,LinearLayout.LayoutParams(dp(160),-1));shell.addView(scroll,LinearLayout.LayoutParams(0,-1,1f))}else{shell.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));shell.addView(nav,LinearLayout.LayoutParams(-1,-2))}
  val outer=LinearLayout(this).apply{orientation=1;setBackgroundColor(paper())};outer.addView(shell,LinearLayout.LayoutParams(-1,0,1f))
  mini=LinearLayout(this).apply{orientation=0;setBackgroundColor(sand());gravity=android.view.Gravity.CENTER_VERTICAL};miniTitle=TextView(this).apply{setTextColor(ink());setPadding(dp(12),0,dp(8),0)};mini.addView(miniTitle,LinearLayout.LayoutParams(0,-2,1f));miniToggle=button(mini,"Reproducir"){toggleAudio()};button(mini,"Abrir"){playerDialog()};outer.addView(mini);updateMini();setContentView(outer)
  button(nav,"Biblioteca"){library.requestFocus();scroll.smoothScrollTo(0,library.top)}
  button(nav,"Reproductor"){playerDialog()}
  button(nav,"Conversión"){scroll.smoothScrollTo(0,0)}
  button(nav,"Ajustes"){android.app.AlertDialog.Builder(this).setTitle("Ajustes").setItems(arrayOf(if(dark)"Modo claro" else "Modo oscuro")){_,_->getPreferences(0).edit().putBoolean("dark",!dark).apply();recreate()}.show()}
  label(root,"VoxNatural AI",30)
  label(root,"Biblioteca de voz · Beta 3 en desarrollo",16)
  button(root,"Subir") {
   startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type="*/*"; addCategory(Intent.CATEGORY_OPENABLE); putExtra(Intent.EXTRA_MIME_TYPES,arrayOf("application/pdf","application/vnd.openxmlformats-officedocument.wordprocessingml.document","text/plain")) },10)
  }
  voice=Spinner(this).apply { adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,arrayOf("Dora · Kokoro","Alex · Kokoro","Santa · Kokoro","DaveFX · Piper","Español · motor local de Android","Supertonic 3 · F1","Supertonic 3 · M1")) }
  root.addView(voice)
  status=label(root,"Selecciona un documento",16)
  button(root,"Convertir a audio") {
   val f=selected
   if(f==null) toast("Primero selecciona un documento")
   else startForegroundService(Intent(this,ConvertService::class.java).putExtra("id",f.name).putExtra("sid",intArrayOf(28,29,53,100,-1,200,205)[voice.selectedItemPosition]))
  }
  label(root,"Mi biblioteca",23)
  library=LinearLayout(this).apply { orientation=1 }; root.addView(library)
  render()
  if(android.os.Build.VERSION.SDK_INT>=33) requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"),99)
 }
 private fun label(parent:LinearLayout,s:String,size:Int)=TextView(this).apply { text=s; textSize=size.toFloat(); setTextColor(ink()); if(size>=23)typeface=android.graphics.Typeface.SERIF; setPadding(0,12,0,12); parent.addView(this) }
 private fun button(parent:LinearLayout,s:String,action:()->Unit)=Button(this).apply { text=s; isAllCaps=false; setTextColor(Color.WHITE); backgroundTintList=android.content.res.ColorStateList.valueOf(burgundy); setOnClickListener { action() }; parent.addView(this) }
 private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_LONG).show()
 override fun onResume(){ super.onResume(); refresh.post(tick) }
 override fun onPause(){ savePlayback();refresh.removeCallbacks(tick); super.onPause() }
 override fun onDestroy(){savePlayback();player?.release();player=null;super.onDestroy()}
 private fun showStatus(){ updateMini();savePlayback();status.text=(selected?.let { File(it,"name").readText() }?:"Selecciona un documento")+"\n"+getSharedPreferences("jobs",0).getString("status",""); if(library.tag!=libraryState()) render() }
 private fun libraryState()=docs.listFiles()?.sortedBy{it.name}?.joinToString("|"){ it.name+":"+(it.listFiles()?.filter{a->a.extension in listOf("wav","mp3")}?.sortedBy{a->a.name}?.joinToString(","){a->a.name+":"+a.lastModified()} ?: "") } ?: ""
 private fun render(){
  library.removeAllViews()
  docs.listFiles()?.filter { File(it,"name").isFile }?.sortedByDescending { it.lastModified() }?.forEach { f ->
   val card=LinearLayout(this).apply{orientation=0;setBackgroundColor(sand());setPadding(dp(12),dp(12),dp(12),dp(12))}
   val cover=TextView(this).apply{text=titleOf(f);textSize=16f;setTextColor(Color.WHITE);setBackgroundColor(burgundy);gravity=android.view.Gravity.CENTER;typeface=android.graphics.Typeface.SERIF;setPadding(dp(8),dp(8),dp(8),dp(8))}
   card.addView(cover,LinearLayout.LayoutParams(dp(100),dp(145)))
   val info=LinearLayout(this).apply{orientation=1;setPadding(dp(12),0,0,0)};card.addView(info,LinearLayout.LayoutParams(0,-2,1f))
   label(info,titleOf(f),23);label(info,authorOf(f),14)
   button(info,if(selected==f)"Seleccionado" else "Seleccionar"){selected=f;render();showStatus()}
   button(info,"Editar datos"){editBook(f)}
   library.addView(card)
   val voices=linkedMapOf(28 to "Dora",29 to "Alex",53 to "Santa",100 to "DaveFX",-1 to "Android",200 to "Supertonic F1",205 to "Supertonic M1")
   for((sid,label) in voices) for(ext in listOf("mp3","wav")){
    val audio=File(f,"audio-"+sid+"."+ext)
    if(audio.isFile) button(library,"Escuchar "+audio.name){playAudio(audio)}
    if(audio.isFile) button(library,"Guardar "+label+" · "+ext.uppercase()){
     exportFile=audio
     startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{type=if(ext=="mp3")"audio/mpeg" else "audio/wav";addCategory(Intent.CATEGORY_OPENABLE);putExtra(Intent.EXTRA_TITLE,File(f,"name").readText().substringBeforeLast(".")+"_"+label+"."+ext)},11)
    }
   }
   for(ext in listOf("mp3","wav")){
    val audio=File(f,"audio."+ext)
    if(audio.isFile) button(library,"Escuchar "+audio.name){playAudio(audio)}
    if(audio.isFile) button(library,"Guardar anterior · "+ext.uppercase()){
     exportFile=audio
     startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{type=if(ext=="mp3")"audio/mpeg" else "audio/wav";addCategory(Intent.CATEGORY_OPENABLE);putExtra(Intent.EXTRA_TITLE,File(f,"name").readText().substringBeforeLast(".")+"."+ext)},11)
    }
   }
  }
  library.tag=libraryState()
 }
 override fun onActivityResult(request:Int,result:Int,data:Intent?){
  super.onActivityResult(request,result,data)
  if(result!=RESULT_OK) return
  val uri=data?.data?:return
  if(request==11){ try { contentResolver.openOutputStream(uri)!!.use { out -> exportFile!!.inputStream().use { it.copyTo(out) } }; toast("Audio guardado") } catch(e:Exception){toast(e.message?:"Error")}; return }
  if(request!=10)return
  Thread {
   var folder:File?=null
   try{
    val name=contentResolver.query(uri,null,null,null,null)?.use { c -> if(c.moveToFirst()) c.getString(c.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME)) else null } ?: "Documento.txt"
    val f=File(docs,UUID.randomUUID().toString()).apply{mkdirs()}; folder=f
    val original=File(f,"original"); contentResolver.openInputStream(uri)!!.use { i->original.outputStream().use{i.copyTo(it)} }
    val text=when(name.substringAfterLast(".").lowercase()){
     "pdf"->PDDocument.load(original).use{PDFTextStripper().getText(it)}
     "docx"->readWord(original)
     "txt"->original.readText()
     else->error("Formato no compatible")
    }
    require(text.isNotBlank()){"No se encontró texto digital"}
    File(f,"text").writeText(text); File(f,"name").writeText(name)
    runOnUiThread { selected=f; render(); showStatus() }
   }catch(e:Exception){folder?.deleteRecursively(); runOnUiThread{toast(e.message?:"Error de importación")}}
  }.start()
 }
 private fun readWord(file:File):String {
  ZipInputStream(file.inputStream()).use { z ->
   while(true){ val entry=z.nextEntry?:break
    if(entry.name=="word/document.xml"){
     val p=Xml.newPullParser(); p.setInput(z,"UTF-8"); val b=StringBuilder(); var inText=false
     while(p.eventType!=XmlPullParser.END_DOCUMENT){
      val n=p.name?.substringAfter(":")
      when(p.eventType){
       XmlPullParser.START_TAG->when(n){"t"->inText=true;"tab"->b.append('\t');"br"->b.append('\n')}
       XmlPullParser.TEXT->if(inText)b.append(p.text)
       XmlPullParser.END_TAG->when(n){"t"->inText=false;"p"->b.append('\n')}
      }; p.next()
     }; return b.toString()
    }
   }
  }; error("DOCX inválido")
 }
}
