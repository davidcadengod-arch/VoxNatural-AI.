package com.voxnatural.app

import java.text.BreakIterator
import java.text.Normalizer
import java.util.Locale

internal data class NeuralSegments(val chunks: List<String>, val pauses: List<Int>)

internal fun segmentNeuralText(text: String): NeuralSegments {
  val normalized=Normalizer.normalize(text,Normalizer.Form.NFC)
   .replace("\r\n","\n").replace('\r','\n').replace("\u00ad","").replace('\u00a0',' ')
  val chunks=mutableListOf<String>()
  val pauses=mutableListOf<Int>()
  val breaks=BreakIterator.getSentenceInstance(Locale("es"))
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
   while(finish!=BreakIterator.DONE){
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
  return NeuralSegments(chunks, pauses)
}
