package com.voxnatural.app

import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import java.io.File
import java.text.BreakIterator
import java.text.Normalizer
import java.util.Locale

internal const val USES_ESPEAK_DATA = false

internal class ModelPhonemizer(tokens: File) {
 private val vocabulary = tokens.useLines { lines ->
  lines.map { it.substringBeforeLast(' ') }.toSet()
 }
 private val spanish = Locale.forLanguageTag("es-MX")
 private val amountAndUnit = Regex("([0-9])\\s*(mg|ml|kg)\\b", RegexOption.IGNORE_CASE)
 private val units = Regex("[a-záéíóúüñ]+|\\s+|[^a-záéíóúüñ\\s]")
 private val accented = "áéíóú"

 fun generate(tts: OfflineTts, text: String, sid: Int, speed: Float) =
  tts.generateWithConfig(
   text,
   GenerationConfig(sid = sid, speed = speed, phonemeCodepoints = phonemize(text))
  )

 private fun phonemize(text: String): Array<IntArray> {
  val withUnits = amountAndUnit.replace(text.lowercase(spanish)) { match ->
   val unit = when (match.groupValues[2]) {
    "mg" -> "miligramos"
    "ml" -> "mililitros"
    else -> "kilogramos"
   }
   match.groupValues[1] + " " + unit
  }
  val expanded = SpanishNumbers.numericToken.replace(Normalizer.normalize(withUnits, Normalizer.Form.NFC)) {
   SpanishNumbers.spell(it.value)
  }
  val sentences = BreakIterator.getSentenceInstance(spanish)
  sentences.setText(expanded)
  val rendered = mutableListOf<IntArray>()
  var start = sentences.first()
  while (true) {
   val end = sentences.next()
   if (end == BreakIterator.DONE) break
   val phonemes = buildString {
    for (match in units.findAll(expanded.substring(start, end))) {
     val token = match.value
     if (token.first() in 'a'..'z' || token.first() in "áéíóúüñ") {
      append(transcribe(token))
     } else if (token.isBlank()) {
      append(' ')
     } else if (token in vocabulary) {
      append(token)
     }
    }
   }.trim()
   if (phonemes.any { it.isLetter() }) rendered.add(phonemes.codePoints().toArray())
   start = end
  }
  require(rendered.isNotEmpty()) { "No se encontraron fonemas compatibles" }
  return rendered.toTypedArray()
 }

 private fun transcribe(word: String): String {
  val phones = UnamG2p.transcribeWord(word)
  val syllables = mutableListOf(mutableListOf<String>())
  for (phone in phones) {
   if (phone == ".") {
    syllables.add(mutableListOf())
   } else {
    syllables.last().add(phone)
   }
  }
  val explicitStress = syllables.indexOfFirst { syllable ->
   syllable.any { it.length == 1 && it[0] in accented }
  }
  val stress = when {
   explicitStress >= 0 -> explicitStress
   syllables.size <= 1 -> -1
   word.last() in "aeiouüns" -> syllables.lastIndex - 1
   else -> syllables.lastIndex
  }
  return buildString {
   for ((index, syllable) in syllables.withIndex()) {
    val nucleus = syllable.indexOfFirst {
     it.length == 1 && it[0] in "aeoáéíóú"
    }.takeIf { it >= 0 } ?: syllable.indexOfLast { it == "i" || it == "u" }
    for ((position, raw) in syllable.withIndex()) {
     if (index == stress && position == nucleus && "ˈ" in vocabulary) append('ˈ')
     val phone = when (raw) {
      "g" -> "ɡ"
      "ʧ" -> if ("ʨ" in vocabulary) "ʨ" else "tʃ"
      "ʤ" -> "dʒ"
      "T" -> "tl"
      "3" -> "ts"
      else -> Normalizer.normalize(raw, Normalizer.Form.NFD).replace("\u0301", "")
     }
     require(phone.all { it.toString() in vocabulary }) {
      "Fonema experimental incompatible en «$word»: $phone"
     }
     append(phone)
    }
   }
  }
 }
}
