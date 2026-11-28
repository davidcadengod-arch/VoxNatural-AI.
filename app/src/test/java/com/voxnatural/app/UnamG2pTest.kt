package com.voxnatural.app

import org.junit.Assert.assertEquals
import org.junit.Test

class UnamG2pTest {
 @Test fun matchesPythonIpa29Reference() {
  val fixtures = mapOf(
   "casa" to "k a . s a",
   "queso" to "k e . s o",
   "guerra" to "g e . r a",
   "guitarra" to "g i . t a . r a",
   "pingüino" to "p i n . g u i . n o",
   "mañana" to "m a . ɲ a . n a",
   "niño" to "n i . ɲ o",
   "acción" to "a k . s i ó n",
   "corazón" to "k o . ɾ a . s ó n",
   "médico" to "m é . d i . k o",
   "anestesiología" to "a . n e s . t e . s i o . l o . x í . a",
   "hipertensión" to "i . p e ɾ . t e n . s i ó n",
   "diabetes" to "d i a . b e . t e s",
   "rápidamente" to "r á . p i . d a . m e n . t e",
   "camión" to "k a . m i ó n",
   "árbol" to "á ɾ . b o l",
   "lápiz" to "l á . p i s",
   "reloj" to "r e . l o x",
   "ciudad" to "s i u . d a d",
   "país" to "p a . í s",
   "raíz" to "r a . í s",
   "aire" to "a i . ɾ e",
   "agua" to "a . g u a",
   "ruido" to "r u i . d o",
   "buey" to "b u e i",
   "llegar" to "ʝ e . g a ɾ",
   "lluvia" to "ʝ u . b i a",
   "carro" to "k a . r o",
   "caro" to "k a . ɾ o",
   "examen" to "e k . s a . m e n",
   "éxito" to "é k . s i . t o",
   "México" to "M é k . s i . k o",
   "psicología" to "s i . k o . l o . x í . a",
   "recuperación" to "r e . k u . p e . ɾ a . s i ó n",
  )
  for ((word, expected) in fixtures) {
   assertEquals(word, expected, UnamG2p.transcribeWord(word).joinToString(" "))
  }
 }
}
