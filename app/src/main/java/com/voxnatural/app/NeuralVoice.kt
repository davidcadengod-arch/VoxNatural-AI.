package com.voxnatural.app
import android.content.Context
import com.k2fsa.sherpa.onnx.*
import java.io.File

class NeuralVoice(context:Context, private val sid:Int):AutoCloseable {
 companion object {
  fun loadReference(context:Context):Pair<FloatArray,Int>{
   val custom=File(context.filesDir,"pocket-reference.wav")
   if(custom.isFile)return PocketReference.read(custom)
   val tmp=File.createTempFile("pocket-ref-",".wav",context.cacheDir)
   try{context.assets.open("pocket/reference.wav").use{i->tmp.outputStream().use{i.copyTo(it)}};return PocketReference.read(tmp)}finally{tmp.delete()}
  }
 }
 private val appContext=context.applicationContext
 private val pocket=sid==300
 private val piper=sid==100
 private val supertonic=sid in 200..209
 private val asset=if(pocket)"pocket" else if(supertonic)"supertonic" else if(piper)"piper" else "kokoro"
 private val dir=File(context.filesDir,asset+(if(pocket)"-v2" else "-v1"))
 private val tts:OfflineTts
 init {
  synchronized(NeuralVoice::class.java) {
   if(!File(dir,"ready").isFile){
    fun copy(asset:String,dest:File){
     val children=context.assets.list(asset)?:emptyArray()
     if(children.isNotEmpty()){dest.mkdirs();children.forEach{copy("$asset/$it",File(dest,it))}}
     else {dest.parentFile?.mkdirs();context.assets.open(asset).use{i->dest.outputStream().use{i.copyTo(it)}}}
    }
    copy(asset,dir)
    File(dir,"ready").writeText("1")
   }
  }
  val config=if(pocket) OfflineTtsConfig(model=OfflineTtsModelConfig(pocket=OfflineTtsPocketModelConfig(lmFlow=File(dir,"lm_flow.int8.onnx").path,lmMain=File(dir,"lm_main.int8.onnx").path,encoder=File(dir,"encoder.onnx").path,decoder=File(dir,"decoder.int8.onnx").path,textConditioner=File(dir,"text_conditioner.onnx").path,vocabJson=File(dir,"vocab.json").path,tokenScoresJson=File(dir,"token_scores.json").path),numThreads=4,provider="cpu"),maxNumSentences=1) else if(supertonic) OfflineTtsConfig(model=OfflineTtsModelConfig(supertonic=OfflineTtsSupertonicModelConfig(
   durationPredictor=File(dir,"duration_predictor.int8.onnx").path,
   textEncoder=File(dir,"text_encoder.int8.onnx").path,
   vectorEstimator=File(dir,"vector_estimator.int8.onnx").path,
   vocoder=File(dir,"vocoder.int8.onnx").path,
   ttsJson=File(dir,"tts.json").path,
   unicodeIndexer=File(dir,"unicode_indexer.bin").path,
   voiceStyle=File(dir,"voice.bin").path),numThreads=4,provider="cpu"),maxNumSentences=1) else if(piper) OfflineTtsConfig(model=OfflineTtsModelConfig(vits=OfflineTtsVitsModelConfig(model=File(dir,"model.onnx").path,tokens=File(dir,"tokens.txt").path,dataDir=File(dir,"espeak-ng-data").path),numThreads=4,provider="cpu"),maxNumSentences=1) else OfflineTtsConfig(model=OfflineTtsModelConfig(
   kokoro=OfflineTtsKokoroModelConfig(model=File(dir,"model.onnx").path,voices=File(dir,"voices.bin").path,tokens=File(dir,"tokens.txt").path,dataDir=File(dir,"espeak-ng-data").path,lang="es"),numThreads=4,provider="cpu"),maxNumSentences=1)
  tts=OfflineTts(config=config)
  if(!pocket && !piper && (if(supertonic)sid-200 else sid) !in 0 until tts.numSpeakers()){tts.release();error("Voz no disponible en el modelo")}
 }
 fun generate(text:String,out:File,referenceAudio:FloatArray?=null,referenceSampleRate:Int=0){
  val expressive=text.count{it=='!' || it=='¡'}
  val reflective=text.contains('…') || text.contains("...")
  val speed=when{
   reflective -> 0.92f
   expressive>0 -> 1.02f
   text.endsWith("?") -> 0.98f
   else -> 0.97f
  }
  val audio=if(pocket){val ref=if(referenceAudio!=null) Pair(referenceAudio,referenceSampleRate) else loadReference(appContext);tts.generateWithConfig(text,GenerationConfig(referenceAudio=ref.first,referenceSampleRate=ref.second,speed=speed,numSteps=10))} else if(supertonic) tts.generateWithConfig(text,GenerationConfig(sid=sid-200,speed=speed,numSteps=8,extra=mapOf("lang" to "es"))) else tts.generate(text,if(piper)0 else sid,speed)
  require(audio.samples.isNotEmpty() && audio.samples.all{it.isFinite()}){"Audio neuronal inválido"}
  // Preserve dynamics: attenuate only peaks that risk clipping, with short edge fades.
  val samples=audio.samples
  val peak=samples.maxOf{ kotlin.math.abs(it) }
  require(audio.sampleRate in 8000..192000 && peak>0.0001f){"Audio neuronal silencioso o frecuencia inválida"}
  val peakLimit=0.98f
  val gain=if(peak>peakLimit)peakLimit/peak else 1f
  val fade=minOf(audio.sampleRate/200,samples.size/2)
  for(i in samples.indices){
   val edge=if(fade>0)minOf(1f,i.toFloat()/fade,(samples.lastIndex-i).toFloat()/fade) else 1f
   samples[i]*=gain*edge
  }
  require(audio.save(out.absolutePath)){"No se pudo guardar el fragmento"}
 }
 override fun close(){tts.release()}
}
