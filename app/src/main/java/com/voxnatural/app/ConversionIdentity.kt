package com.voxnatural.app
import java.security.MessageDigest
import java.nio.ByteBuffer
object ConversionIdentity {
 fun key(text:String,reference:Pair<FloatArray,Int>?):String {
  val digest=MessageDigest.getInstance("SHA-256")
  val bytes=text.toByteArray(Charsets.UTF_8)
  digest.update(ByteBuffer.allocate(4).putInt(bytes.size).array())
  digest.update(bytes)
  if(reference!=null){
   val b=ByteBuffer.allocate(4+reference.first.size*4)
   b.putInt(reference.second);reference.first.forEach{b.putFloat(it)}
   digest.update(b.array())
  }
  return digest.digest().joinToString(""){"%02x".format(it.toInt() and 255)}
 }
}
