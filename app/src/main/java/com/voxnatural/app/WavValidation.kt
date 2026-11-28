package com.voxnatural.app

import java.io.File
import java.io.RandomAccessFile

// Validate cached PCM before reuse; reject truncated or empty data chunks.
internal fun validNeuralWav(file:File):Boolean = try {
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
