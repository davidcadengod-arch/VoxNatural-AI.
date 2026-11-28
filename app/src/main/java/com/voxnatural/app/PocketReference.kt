package com.voxnatural.app
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
object PocketReference {
 fun read(file:File):Pair<FloatArray,Int>{
  require(file.isFile && file.length() in 44..4000000){"Importa una referencia WAV autorizada"}
  val b=ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
  fun tag():String {val a=ByteArray(4);b.get(a);return String(a,Charsets.US_ASCII)}
  require(tag()=="RIFF");require((b.int.toLong() and 0xffffffffL)+8==file.length());require(tag()=="WAVE")
  var rate=0;var samples:FloatArray?=null
  while(b.remaining()>=8){val id=tag();val size=b.int;require(size>=0 && size<=b.remaining());val end=b.position()+size
   if(id=="fmt "){require(size>=16);require(b.short.toInt()==1 && b.short.toInt()==1){"WAV PCM mono requerido"};rate=b.int;b.int;b.short;require(b.short.toInt()==16){"WAV de 16 bits requerido"}}
   if(id=="data"){require(rate>0 && samples==null && size%2==0);samples=FloatArray(size/2){b.short/32768f}}
   b.position(end);if(size%2==1 && b.hasRemaining())b.get()
  }
  require(!b.hasRemaining());val a=requireNotNull(samples);require(rate in 16000..48000 && a.size in rate*3..rate*20 && a.any{kotlin.math.abs(it)>0.01f}){"Referencia audible de 3 a 20 segundos requerida"}
  return Pair(a,rate)
 }
}
