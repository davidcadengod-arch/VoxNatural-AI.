package com.voxnatural.app
import org.junit.Test
import org.junit.Assert.*
class ConversionIdentityTest {
 private val ref=Pair(floatArrayOf(.1f,.2f),24000)
 @Test fun sameInputsResume(){assertEquals(ConversionIdentity.key("Hola",ref),ConversionIdentity.key("Hola",Pair(ref.first.copyOf(),24000)))}
 @Test fun changedVoiceInvalidates(){assertNotEquals(ConversionIdentity.key("Hola",ref),ConversionIdentity.key("Hola",Pair(floatArrayOf(.2f,.1f),24000)))}
 @Test fun changedRateInvalidates(){assertNotEquals(ConversionIdentity.key("Hola",ref),ConversionIdentity.key("Hola",Pair(ref.first,16000)))}
 @Test fun changedTextInvalidates(){assertNotEquals(ConversionIdentity.key("Hola",ref),ConversionIdentity.key("Adiós",ref))}
 @Test fun absentReferenceDiffers(){assertNotEquals(ConversionIdentity.key("Hola",ref),ConversionIdentity.key("Hola",null))}
}
