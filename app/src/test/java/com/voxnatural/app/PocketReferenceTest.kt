package com.voxnatural.app
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
class PocketReferenceTest {
 private fun fixture(seconds:Int=3, silent:Boolean=false):ByteArray {
  val n=16000*seconds*2
  return ByteBuffer.allocate(44+n).order(ByteOrder.LITTLE_ENDIAN).apply {
   put("RIFF".toByteArray());putInt(36+n);put("WAVEfmt ".toByteArray());putInt(16);putShort(1);putShort(1);putInt(16000);putInt(32000);putShort(2);putShort(16);put("data".toByteArray());putInt(n)
   repeat(n/2){putShort(if(silent)0 else 1000)}
  }.array()
 }
 private fun read(bytes:ByteArray):Pair<FloatArray,Int>{val f=File.createTempFile("ref",".wav");try{f.writeBytes(bytes);return PocketReference.read(f)}finally{f.delete()}}
 @Test fun valid(){val r=read(fixture());assertEquals(16000,r.second);assertEquals(48000,r.first.size)}
 @Test fun truncated(){assertThrows(IllegalArgumentException::class.java){read(fixture().dropLast(2).toByteArray())}}
 @Test fun silent(){assertThrows(IllegalArgumentException::class.java){read(fixture(silent=true))}}
 @Test fun short(){assertThrows(IllegalArgumentException::class.java){read(fixture(2))}}
 @Test fun wrongHeader(){val a=fixture();a[0]=0;assertThrows(IllegalArgumentException::class.java){read(a)}}
}
