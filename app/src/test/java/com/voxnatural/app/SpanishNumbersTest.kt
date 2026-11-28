package com.voxnatural.app

import org.junit.Assert.assertEquals
import org.junit.Test

class SpanishNumbersTest {
 @Test fun readsDecimalDosesAndSigns() {
  assertEquals("dos coma cinco", SpanishNumbers.spell("2,5"))
  assertEquals("menos cero coma cero seis", SpanishNumbers.spell("−0,06"))
  assertEquals("dos coma cinco", SpanishNumbers.spell("2.5"))
 }

 @Test fun readsThousandsAndMillions() {
  assertEquals("mil doscientos cincuenta", SpanishNumbers.spell("1.250"))
  assertEquals("un millón veintiún mil veintiuno", SpanishNumbers.spell("1.021.021"))
  assertEquals("mil millones", SpanishNumbers.spell("1000000000"))
  assertEquals("cien", SpanishNumbers.spell("100"))
 }

 @Test fun preservesLeadingZerosAndSentencePunctuation() {
  val text = "Dosis 007. ¿Repetir 2,5? Sí."
  assertEquals("Dosis cero cero siete. ¿Repetir dos coma cinco? Sí.", SpanishNumbers.numericToken.replace(text) {
   SpanishNumbers.spell(it.value)
  })
 }
}
