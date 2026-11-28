package com.voxnatural.app

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Test

class WavValidationTest {
 private fun wav(frames: Int = 4, channels: Int = 1): ByteArray {
  val bytes = frames * channels * 2
  return ByteBuffer.allocate(44 + bytes).order(ByteOrder.LITTLE_ENDIAN).apply {
   put("RIFF".toByteArray())
   putInt(36 + bytes)
   put("WAVEfmt ".toByteArray())
   putInt(16)
   putShort(1)
   putShort(channels.toShort())
   putInt(24000)
   putInt(24000 * channels * 2)
   putShort((channels * 2).toShort())
   putShort(16)
   put("data".toByteArray())
   putInt(bytes)
  }.array()
 }

 private fun validate(bytes: ByteArray): Boolean {
  val file = File.createTempFile("fragment-", ".wav")
  return try {
   file.writeBytes(bytes)
   validNeuralWav(file)
  } finally {
   file.delete()
  }
 }

 @Test fun validMonoPcmCanBeReused() = assertTrue(validate(wav()))

 @Test fun corruptFragmentsMustBeSynthesizedAgain() {
  val valid = wav()
  assertFalse(validate(valid.copyOf(valid.size - 1)))
  assertFalse(validate(valid.copyOf(valid.size + 1)))
  assertFalse(validate(wav(frames = 0)))
  assertFalse(validate(wav(channels = 2)))
 }
}
