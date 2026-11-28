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
  root=LinearLayout(this).apply { orientation=1; setPadding(28,30,28,30); setBackgroundColor(Color.rgb(17,24,39)) }
  setContentView(ScrollView(this).apply { addView(root) })
  label(root,"VoxNatural AI",30)
  label(root,"Tu biblioteca de voz · 3.0 alpha",16)
  button(root,"Importar PDF, Word o TXT") {
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
 private fun label(parent:LinearLayout,s:String,size:Int)=TextView(this).apply { text=s; textSize=size.toFloat(); setTextColor(Color.WHITE); setPadding(0,12,0,12); parent.addView(this) }
 private fun button(parent:LinearLayout,s:String,action:()->Unit)=Button(this).apply { text=s; isAllCaps=false; setOnClickListener { action() }; parent.addView(this) }
 private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_LONG).show()
 override fun onResume(){ super.onResume(); refresh.post(tick) }
 override fun onPause(){ refresh.removeCallbacks(tick); super.onPause() }
 private fun showStatus(){ status.text=(selected?.let { File(it,"name").readText() }?:"Selecciona un documento")+"\n"+getSharedPreferences("jobs",0).getString("status",""); if(library.tag!=libraryState()) render() }
 private fun libraryState()=docs.listFiles()?.sortedBy{it.name}?.joinToString("|"){ it.name+":"+(it.listFiles()?.filter{a->a.extension in listOf("wav","mp3")}?.sortedBy{a->a.name}?.joinToString(","){a->a.name+":"+a.lastModified()} ?: "") } ?: ""
 private fun render(){
  library.removeAllViews()
  docs.listFiles()?.filter { File(it,"name").isFile }?.sortedByDescending { it.lastModified() }?.forEach { f ->
   button(library,(if(selected==f) "✓ " else "")+File(f,"name").readText()){ selected=f; render(); showStatus() }
   val voices=linkedMapOf(28 to "Dora",29 to "Alex",53 to "Santa",100 to "DaveFX",-1 to "Android",200 to "Supertonic F1",205 to "Supertonic M1")
   for((sid,label) in voices) for(ext in listOf("mp3","wav")){
    val audio=File(f,"audio-"+sid+"."+ext)
    if(audio.isFile) button(library,"Guardar "+label+" · "+ext.uppercase()){
     exportFile=audio
     startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{type=if(ext=="mp3")"audio/mpeg" else "audio/wav";addCategory(Intent.CATEGORY_OPENABLE);putExtra(Intent.EXTRA_TITLE,File(f,"name").readText().substringBeforeLast(".")+"_"+label+"."+ext)},11)
    }
   }
   for(ext in listOf("mp3","wav")){
    val audio=File(f,"audio."+ext)
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
