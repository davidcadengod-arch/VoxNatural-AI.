package com.voxnatural.app
import android.content.Context
import com.k2fsa.sherpa.onnx.*
import java.io.File

class NeuralVoice(context:Context, private val sid:Int):AutoCloseable {
 private val dir=File(context.filesDir,"kokoro-v1")
 private val tts:OfflineTts
 init {
  synchronized(NeuralVoice::class.java) {
   if(!File(dir,"ready").isFile){
    fun copy(asset:String,dest:File){
     val children=context.assets.list(asset)?:emptyArray()
     if(children.isNotEmpty()){dest.mkdirs();children.forEach{copy("$asset/$it",File(dest,it))}}
     else {dest.parentFile?.mkdirs();context.assets.open(asset).use{i->dest.outputStream().use{i.copyTo(it)}}}
    }
    copy("kokoro",dir)
    File(dir,"ready").writeText("1")
   }
  }
  val config=OfflineTtsConfig(model=OfflineTtsModelConfig(
   kokoro=OfflineTtsKokoroModelConfig(model=File(dir,"model.onnx").path,voices=File(dir,"voices.bin").path,tokens=File(dir,"tokens.txt").path,dataDir=File(dir,"espeak-ng-data").path,lang="es"),numThreads=4,provider="cpu"),maxNumSentences=1)
  tts=OfflineTts(config=config)
  if(sid !in 0 until tts.numSpeakers()){tts.release();error("Voz no disponible en el modelo")}
 }
 fun generate(text:String,out:File){
  val audio=tts.generate(text,sid,1.0f)
  require(audio.samples.isNotEmpty() && audio.samples.all{it.isFinite()}){"Audio neuronal inválido"}
  require(audio.save(out.absolutePath)){"No se pudo guardar el fragmento"}
 }
 override fun close(){tts.release()}
}
