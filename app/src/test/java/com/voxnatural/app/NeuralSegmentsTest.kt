package com.voxnatural.app

import org.junit.Assert.*
import org.junit.Test

class NeuralSegmentsTest {
 @Test fun preservesSpanishPunctuationNumbersAndParagraphPauses() {
  val first = "¿La nin\u0303a recibió 2,5 mg\u00a0de medicación? ¡Sí!"
  val second = "La hipertensión requiere atención."
  val (chunks, pauses) = segmentNeuralText("$first\r\n\r\n$second")
  assertEquals(listOf("¿La niña recibió 2,5 mg de medicación? ¡Sí!", second), chunks)
  assertEquals(listOf(240, 240), pauses)
 }

 @Test fun longBookRetainsEveryWordAndOrder() {
  val paragraph = "El médico explicó la recuperación, la respiración y la dosis de 25 mg."
  val source = List(1200) { paragraph }.joinToString(" ")
  val (chunks, pauses) = segmentNeuralText(source)
  assertEquals(source, chunks.joinToString(" "))
  assertTrue(chunks.all { it.length in 1..260 })
  assertEquals(chunks.size, pauses.size)
  assertEquals(240, pauses.last())
 }

 @Test fun splitDoesNotDamageSurrogatePairs() {
  val source = "a".repeat(259) + "𐐷" + "b".repeat(260)
  val chunks = segmentNeuralText(source).chunks
  assertEquals(source, chunks.joinToString(""))
  assertTrue(chunks.none { Character.isHighSurrogate(it.last()) })
 }

 @Test(expected = IllegalArgumentException::class)
 fun blankDocumentIsRejected() {
  segmentNeuralText(" \r\n\r\n\u00a0")
 }
}
