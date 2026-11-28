package com.voxnatural.app

internal object SpanishNumbers {
 private val small = listOf(
  "cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve",
  "diez", "once", "doce", "trece", "catorce", "quince", "dieciséis",
  "diecisiete", "dieciocho", "diecinueve", "veinte", "veintiuno", "veintidós",
  "veintitrés", "veinticuatro", "veinticinco", "veintiséis", "veintisiete",
  "veintiocho", "veintinueve"
 )
 private val tens = listOf("", "", "", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa")
 private val hundreds = listOf("", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos", "seiscientos", "setecientos", "ochocientos", "novecientos")
 private val scales = listOf("millón" to "millones", "billón" to "billones", "trillón" to "trillones")
 private val grouped = Regex("[0-9]{1,3}(?:\\.[0-9]{3})+")
 val numericToken = Regex("[+−-]?[0-9]+(?:\\.[0-9]{3})*(?:[,.][0-9]+)?")

 fun spell(token: String): String {
  val negative = token.startsWith('-') || token.startsWith('−')
  val unsigned = token.trimStart('+', '-', '−')
  val parts = when {
   ',' in unsigned -> unsigned.replace(".", "").split(',', limit = 2)
   grouped.matches(unsigned) -> listOf(unsigned.replace(".", ""))
   else -> unsigned.split('.', limit = 2)
  }
  val integer = parts[0]
  val phrase = if (integer.length > 1 && integer.startsWith('0')) {
   spellDigits(integer)
  } else {
   spellInteger(integer)
  }
  val fraction = parts.getOrNull(1)
  val sign = if (negative) "menos " else ""
  return if (fraction == null) "$sign$phrase" else "$sign$phrase coma ${spellDigits(fraction)}"
 }

 private fun spellDigits(digits: String): String =
  digits.map { small[it.digitToInt()] }.joinToString(" ")

 private fun spellInteger(digits: String): String {
  val groups = digits.reversed().chunked(6).map { it.reversed().toInt() }
  if (groups.size > scales.size + 1) return spellDigits(digits)
  if (groups.all { it == 0 }) return "cero"
  return groups.indices.reversed().mapNotNull { index ->
   val value = groups[index]
   when {
    value == 0 -> null
    index == 0 -> underMillion(value)
    value == 1 -> "un ${scales[index - 1].first}"
    else -> "${apocopate(underMillion(value))} ${scales[index - 1].second}"
   }
  }.joinToString(" ")
 }

 private fun underMillion(value: Int): String {
  val thousands = value / 1000
  val rest = value % 1000
  if (thousands == 0) return underThousand(rest)
  val prefix = if (thousands == 1) "mil" else "${apocopate(underThousand(thousands))} mil"
  return if (rest == 0) prefix else "$prefix ${underThousand(rest)}"
 }

 private fun underThousand(value: Int): String {
  if (value == 100) return "cien"
  if (value < 30) return small[value]
  val remainder = value % 100
  val prefix = hundreds[value / 100]
  if (remainder == 0) return prefix
  val belowHundred = when {
   remainder < 30 -> small[remainder]
   remainder % 10 == 0 -> tens[remainder / 10]
   else -> "${tens[remainder / 10]} y ${small[remainder % 10]}"
  }
  return if (prefix.isEmpty()) belowHundred else "$prefix $belowHundred"
 }

 private fun apocopate(text: String): String = when {
  text.endsWith("veintiuno") -> text.removeSuffix("veintiuno") + "veintiún"
  text.endsWith("uno") -> text.removeSuffix("uno") + "un"
  else -> text
 }
}
