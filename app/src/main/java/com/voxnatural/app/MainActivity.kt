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
 private val sleepAction=Runnable{try{player?.pause();updateMini();savePlayback()}catch(_:Exception){}}
 private var player: android.media.MediaPlayer?=null
 private var playingFile:File?=null
 private lateinit var mini:LinearLayout
 private lateinit var miniTitle:TextView
 private lateinit var miniToggle:Button
 private fun titleOf(f:File)=File(f,"title").takeIf{it.isFile}?.readText()?.takeIf{it.isNotBlank()}?:File(f,"name").readText().substringBeforeLast(".")
 private fun authorOf(f:File)=File(f,"author").takeIf{it.isFile}?.readText()?.takeIf{it.isNotBlank()}?:"Autor desconocido"
 private fun readPdf(f:File){
  try{
   val renderer=android.graphics.pdf.PdfRenderer(android.os.ParcelFileDescriptor.open(File(f,"original"),android.os.ParcelFileDescriptor.MODE_READ_ONLY))
   val prefs=getPreferences(0);var page=prefs.getInt("read:"+f.name,0).coerceIn(0,renderer.pageCount-1)
   var dual=false
   val panel=LinearLayout(this).apply{orientation=1;setBackgroundColor(paper())}
   val stats=label(panel,"",16)
   val spread=LinearLayout(this).apply{orientation=0};panel.addView(spread,LinearLayout.LayoutParams(-1,0,1f))
   val images=List(2){ImageView(this).apply{adjustViewBounds=true;scaleType=ImageView.ScaleType.FIT_CENTER;spread.addView(this,LinearLayout.LayoutParams(0,-1,1f))}}
   fun update(){
    for(i in images.indices){val v=images[i];v.setImageDrawable(null);val n=page+i;v.visibility=if((i==0||dual)&&n<renderer.pageCount)android.view.View.VISIBLE else android.view.View.GONE
     if(v.visibility==android.view.View.VISIBLE)renderer.openPage(n).use{p->val scale=minOf(2f,1600f/maxOf(p.width,p.height));val bmp=android.graphics.Bitmap.createBitmap((p.width*scale).toInt().coerceAtLeast(1),(p.height*scale).toInt().coerceAtLeast(1),android.graphics.Bitmap.Config.ARGB_8888);bmp.eraseColor(Color.WHITE);p.render(bmp,null,null,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);v.setImageBitmap(bmp)}
    }
    stats.text="Página ${page+1} de ${renderer.pageCount}";prefs.edit().putInt("read:"+f.name,page).putInt("pages:"+f.name,renderer.pageCount).apply()
   }
   val controls=LinearLayout(this).apply{orientation=0};panel.addView(controls)
   button(controls,"Anterior"){page=(page-if(dual)2 else 1).coerceAtLeast(0);update()}
   button(controls,"Siguiente"){page=(page+if(dual)2 else 1).coerceAtMost(renderer.pageCount-1);update()}
   button(panel,"Una / dos páginas"){dual=!dual;update()}
   button(panel,"Guardar marcador"){prefs.edit().putInt("mark:"+f.name,page).apply();toast("Marcador guardado")}
   button(panel,"Ir al marcador"){page=prefs.getInt("mark:"+f.name,page).coerceIn(0,renderer.pageCount-1);update()}
   val dialog=android.app.Dialog(this);dialog.setContentView(panel);dialog.setOnDismissListener{renderer.close();render()};update();dialog.show();dialog.window?.setLayout(-1,-1)
  }catch(e:Exception){toast(e.message?:"No se pudo abrir PDF")}
 }
 private fun readingProgress(f:File):String {
  val total=File(f,"text").length()
  val pos=getPreferences(0).getInt("read:"+f.name,0)
  val pages=getPreferences(0).getInt("pages:"+f.name,0)
  return if(pages>0)"Página ${pos+1} de $pages · ${((pos+1)*100L/pages).coerceAtMost(100)}%" else if(total>0)"Sin iniciar lectura" else "Sin texto"
 }
 private fun readBook(f:File){
  if(File(f,"name").readText().endsWith(".pdf",true)){readPdf(f);return}
  try{
   val source=File(f,"text").readText()
   val pages=mutableListOf<String>();var start=0
   while(start<source.length){var end=(start+1800).coerceAtMost(source.length);if(end<source.length){val space=source.lastIndexOf(' ',end);if(space>start+900)end=space};pages.add(source.substring(start,end));start=end}
   if(pages.isEmpty()){toast("Sin texto");return}
   val prefs=getPreferences(0);var index=prefs.getInt("read:"+f.name,0).coerceIn(0,pages.lastIndex)
   var doublePage=false
   val panel=LinearLayout(this).apply{orientation=1;setBackgroundColor(paper());setPadding(dp(16),dp(12),dp(16),dp(12))}
   val stats=label(panel,"",16)
   val bar=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=pages.size};panel.addView(bar)
   val spread=LinearLayout(this).apply{orientation=0};panel.addView(spread,LinearLayout.LayoutParams(-1,0,1f))
   fun pageView():TextView=TextView(this).apply{textSize=18f;setTextColor(ink());typeface=android.graphics.Typeface.SERIF;setPadding(dp(10),dp(10),dp(10),dp(10));setTextIsSelectable(true)}
   val left=pageView();val right=pageView()
   val ls=ScrollView(this).apply{addView(left)};val rs=ScrollView(this).apply{addView(right)}
   spread.addView(ls,LinearLayout.LayoutParams(0,-1,1f));spread.addView(rs,LinearLayout.LayoutParams(0,-1,1f))
   fun update(){left.text=pages[index];right.text=pages.getOrNull(index+1)?:"";rs.visibility=if(doublePage)android.view.View.VISIBLE else android.view.View.GONE;ls.scrollTo(0,0);rs.scrollTo(0,0);stats.text="Página ${index+1} de ${pages.size} · ${pages.size-index-1} restantes";bar.progress=index+1;prefs.edit().putInt("read:"+f.name,index).putInt("pages:"+f.name,pages.size).apply()}
   val controls=LinearLayout(this).apply{orientation=0};panel.addView(controls)
   button(controls,"Anterior"){index=(index-if(doublePage)2 else 1).coerceAtLeast(0);update()}
   button(controls,"Siguiente"){index=(index+if(doublePage)2 else 1).coerceAtMost(pages.lastIndex);update()}
   button(panel,"Una / dos páginas"){doublePage=!doublePage;update()}
   button(panel,"Buscar texto"){val q=EditText(this).apply{hint="Palabra o frase"};android.app.AlertDialog.Builder(this).setTitle("Buscar en libro").setView(q).setPositiveButton("Buscar"){_,_->val term=q.text.toString().trim();val found=if(term.isEmpty())-1 else pages.indices.firstOrNull{pages[it].contains(term,true)}?:-1;if(found>=0){index=found;update()}else toast("Sin coincidencias")}.setNegativeButton("Cancelar",null).show()}
   button(panel,"Nota de página"){val key="note:"+f.name+":"+index;val note=EditText(this).apply{setText(prefs.getString(key,""))};android.app.AlertDialog.Builder(this).setTitle("Anotación").setView(note).setPositiveButton("Guardar"){_,_->prefs.edit().putString(key,note.text.toString()).apply()}.setNegativeButton("Cancelar",null).show()}
   button(panel,"Guardar marcador"){prefs.edit().putInt("mark:"+f.name,index).apply();toast("Marcador guardado")}
   button(panel,"Ir al marcador"){index=prefs.getInt("mark:"+f.name,index).coerceIn(0,pages.lastIndex);update()}
   update()
   val dialog=android.app.Dialog(this);dialog.setContentView(panel);dialog.setOnDismissListener{render()};dialog.show();dialog.window?.setLayout(-1,-1)
  }catch(e:Exception){toast(e.message?:"No se pudo abrir el libro")}
 }
 private var previewBusy=false
 private fun compareVoices(){
  val panel=LinearLayout(this).apply{orientation=1;setPadding(dp(16),dp(8),dp(16),dp(8))}
  val sample=EditText(this).apply{setText("La mañana llegó lentamente. ¿Qué encontraríamos al otro lado? Nadie conocía la respuesta.");maxLines=5};panel.addView(sample)
  val result=label(panel,"Selecciona una voz para generar una muestra local.",14)
  val choices=linkedMapOf(28 to "Dora · Kokoro",29 to "Alex · Kokoro",53 to "Santa · Kokoro",100 to "DaveFX · Piper",200 to "Supertonic F1",205 to "Supertonic M1",300 to "PocketTTS")
  for((sid,name) in choices)button(panel,name){
   if(previewBusy){toast("Espera la muestra actual");return@button}
   val input=sample.text.toString().trim()
   if(input.isEmpty()||input.length>260){toast("Escribe entre 1 y 260 caracteres");return@button}
   previewBusy=true;result.text="Generando "+name
   Thread{
    try{
     val folder=File(cacheDir,"voice-preview").apply{mkdirs()}
     val file=File(folder,java.util.UUID.randomUUID().toString()+".wav")
     val start=android.os.SystemClock.elapsedRealtime()
     NeuralVoice(this,sid).use{it.generate(Pronunciation(this).apply(input),file)}
     require(file.length()>44)
     val elapsed=android.os.SystemClock.elapsedRealtime()-start
     runOnUiThread{previewBusy=false;if(!isDestroyed){result.text=name+" · "+elapsed+" ms · "+file.length()+" bytes";playPreview(file)}}
    }catch(e:Throwable){runOnUiThread{previewBusy=false;result.text="Error: "+e.message}}
   }.start()
  }
  android.app.AlertDialog.Builder(this).setTitle("Comparar voces").setView(ScrollView(this).apply{addView(panel)}).setPositiveButton("Cerrar",null).show()
 }
 private var previewPlayer:android.media.MediaPlayer?=null
 private fun playPreview(file:File){
  try{player?.pause();updateMini();previewPlayer?.release();val m=android.media.MediaPlayer();previewPlayer=m;m.setDataSource(file.absolutePath);m.prepare();m.setOnCompletionListener{it.release();if(previewPlayer===it)previewPlayer=null;file.delete()};m.start()}catch(e:Exception){previewPlayer?.release();previewPlayer=null;toast(e.message?:"Error de muestra")}
 }
 private fun regenerate(f:File,sid:Int){
  val parts=File(f,"neural-parts-"+sid+"-v7")
  val index=File(parts,"chunks.json")
  if(!index.isFile){toast("Convierte primero con esta versión para conservar los fragmentos");return}
  try{
   val j=org.json.JSONArray(index.readText())
   val items=Array(j.length()){n->"${n+1}. "+j.getString(n)}
   android.app.AlertDialog.Builder(this).setTitle("Regenerar fragmento").setItems(items){_,n->
    startForegroundService(Intent(this,ConvertService::class.java).putExtra("id",f.name).putExtra("sid",sid).putExtra("fragment",n))
   }.setNegativeButton("Cancelar",null).show()
  }catch(e:Exception){toast(e.message?:"No se pudieron abrir los fragmentos")}
 }
 private fun dictionary(){
  val dict=Pronunciation(this);val panel=LinearLayout(this).apply{orientation=1;setPadding(dp(16),dp(8),dp(16),dp(8))}
  val word=EditText(this).apply{hint="Palabra original"};panel.addView(word)
  val spoken=EditText(this).apply{hint="Cómo debe sonar (vacío para eliminar)"};panel.addView(spoken)
  val j=dict.entries();val entries=j.keys().asSequence().sorted().joinToString("\n"){it+" → "+j.optString(it)}
  label(panel,entries.ifBlank{"Sin reglas personalizadas"},14)
  android.app.AlertDialog.Builder(this).setTitle("Diccionario de pronunciación").setView(ScrollView(this).apply{addView(panel)}).setPositiveButton("Guardar"){_,_->if(word.text.toString().trim().isNotEmpty()){dict.save(word.text.toString().trim(),spoken.text.toString().trim());toast("Regla guardada")}else toast("Escribe una palabra")}.setNegativeButton("Cerrar",null).show()
 }
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
  val speeds=arrayOf("0.75×","1×","1.25×","1.5×","2×");val rates=floatArrayOf(.75f,1f,1.25f,1.5f,2f)
  button(panel,"Velocidad"){android.app.AlertDialog.Builder(this).setTitle("Velocidad de reproducción").setItems(speeds){_,n->try{m.playbackParams=m.playbackParams.setSpeed(rates[n])}catch(e:Exception){toast("Velocidad no disponible")}}.show()}
  button(panel,"Temporizador"){android.app.AlertDialog.Builder(this).setTitle("Detener reproducción").setItems(arrayOf("Desactivar","15 minutos","30 minutos","60 minutos")){_,n->refresh.removeCallbacks(sleepAction);if(n>0)refresh.postDelayed(sleepAction,longArrayOf(0,15,30,60)[n]*60000);toast(if(n==0)"Temporizador desactivado" else "Temporizador activado")}.show()}
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
  button(nav,"Ajustes"){android.app.AlertDialog.Builder(this).setTitle("Ajustes").setItems(arrayOf(if(dark)"Modo claro" else "Modo oscuro","Diccionario de pronunciación")){_,which->if(which==1)dictionary() else {getPreferences(0).edit().putBoolean("dark",!dark).apply();recreate()}}.show()}
  label(root,"VoxNatural AI",30)
  label(root,"Biblioteca de voz · Beta 3 en desarrollo",16)
  button(root,"Subir") {
   startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type="*/*"; addCategory(Intent.CATEGORY_OPENABLE); putExtra(Intent.EXTRA_MIME_TYPES,arrayOf("application/pdf","application/vnd.openxmlformats-officedocument.wordprocessingml.document","text/plain")) },10)
  }
  voice=Spinner(this).apply { adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,arrayOf("Dora · Kokoro","Alex · Kokoro","Santa · Kokoro","DaveFX · Piper","Español · motor local de Android","Supertonic 3 · F1","Supertonic 3 · M1","PocketTTS · referencia propia")) }
  root.addView(voice)
  button(root,"Importar referencia PocketTTS (WAV)"){android.app.AlertDialog.Builder(this).setTitle("Referencia autorizada").setMessage("Usa únicamente tu propia voz o una grabación con permiso explícito para síntesis. WAV PCM mono de 3 a 20 segundos.").setNegativeButton("Cancelar",null).setPositiveButton("Tengo autorización"){_,_->startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("audio/*").addCategory(Intent.CATEGORY_OPENABLE),12)}.show()}
  button(root,"Comparar voces"){compareVoices()}
  status=label(root,"Selecciona un documento",16)
  button(root,"Convertir a audio") {
   val f=selected
   if(f==null) toast("Primero selecciona un documento")
   else startForegroundService(Intent(this,ConvertService::class.java).putExtra("id",f.name).putExtra("sid",intArrayOf(28,29,53,100,-1,200,205,300)[voice.selectedItemPosition]))
  }
  button(root,"Regenerar fragmento"){val f=selected;val sid=intArrayOf(28,29,53,100,-1,200,205,300)[voice.selectedItemPosition];if(f==null||sid<0)toast("Selecciona un libro y una voz neuronal") else regenerate(f,sid)}
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
 override fun onDestroy(){refresh.removeCallbacks(sleepAction);previewPlayer?.release();previewPlayer=null;savePlayback();player?.release();player=null;super.onDestroy()}
 private fun showStatus(){ updateMini();savePlayback();status.text=(selected?.let { File(it,"name").readText() }?:"Selecciona un documento")+"\n"+getSharedPreferences("jobs",0).getString("status",""); if(library.tag!=libraryState()) render() }
 private fun libraryState()=docs.listFiles()?.sortedBy{it.name}?.joinToString("|"){ it.name+":"+(it.listFiles()?.filter{a->a.extension in listOf("wav","mp3")}?.sortedBy{a->a.name}?.joinToString(","){a->a.name+":"+a.lastModified()} ?: "") } ?: ""
 private fun render(){
  library.removeAllViews()
  docs.listFiles()?.filter { File(it,"name").isFile }?.sortedByDescending { it.lastModified() }?.forEach { f ->
   val card=LinearLayout(this).apply{orientation=0;setBackgroundColor(sand());setPadding(dp(12),dp(12),dp(12),dp(12))}
   val cover=TextView(this).apply{text=titleOf(f);textSize=16f;setTextColor(Color.WHITE);setBackgroundColor(burgundy);gravity=android.view.Gravity.CENTER;typeface=android.graphics.Typeface.SERIF;setPadding(dp(8),dp(8),dp(8),dp(8))}
   val originalCover=File(f,"cover.png");if(originalCover.isFile){val image=ImageView(this).apply{setImageBitmap(android.graphics.BitmapFactory.decodeFile(originalCover.absolutePath));scaleType=ImageView.ScaleType.FIT_CENTER};card.addView(image,LinearLayout.LayoutParams(dp(100),dp(145)))}else card.addView(cover,LinearLayout.LayoutParams(dp(100),dp(145)))
   val info=LinearLayout(this).apply{orientation=1;setPadding(dp(12),0,0,0)};card.addView(info,LinearLayout.LayoutParams(0,-2,1f))
   label(info,titleOf(f),23);label(info,authorOf(f),14);label(info,readingProgress(f),14)
   button(info,"Leer"){readBook(f)}
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
  if(request==12){try{val tmp=File(filesDir,"pocket-reference.tmp");contentResolver.openInputStream(uri)!!.use{input->tmp.outputStream().use{out->val buf=ByteArray(8192);var total=0;while(true){val count=input.read(buf);if(count<0)break;total+=count;require(total<=4000000){"Referencia demasiado grande"};out.write(buf,0,count)}}};PocketReference.read(tmp);require(tmp.renameTo(File(filesDir,"pocket-reference.wav"))){"No se pudo guardar referencia"};toast("Referencia autorizada guardada")}catch(e:Exception){toast(e.message?:"Referencia inválida")};return}
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
    if(name.endsWith(".pdf",true))try{
     android.graphics.pdf.PdfRenderer(android.os.ParcelFileDescriptor.open(original,android.os.ParcelFileDescriptor.MODE_READ_ONLY)).use{r->r.openPage(0).use{p->val bmp=android.graphics.Bitmap.createBitmap(240,(240f*p.height/p.width).toInt().coerceIn(1,600),android.graphics.Bitmap.Config.ARGB_8888);bmp.eraseColor(Color.WHITE);p.render(bmp,null,null,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);File(f,"cover.png").outputStream().use{bmp.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};bmp.recycle()}}
    }catch(_:Exception){}
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
