package com.voxnatural.app
import java.io.File

object Mp3Encoder {
 init { System.loadLibrary("voxmp3") }
 private external fun encodeNative(input:String,output:String):Int
 fun encode(wav:File,mp3:File){
  val temp=File(mp3.parentFile,"audio.mp3.tmp")
  try {
   require(encodeNative(wav.absolutePath,temp.absolutePath)==0 && temp.length()>0){"No se pudo codificar MP3"}
   require(temp.renameTo(mp3)){"No se pudo guardar MP3"}
  }finally{temp.delete()}
 }
}
