package com.voxnatural.app

/*
 * Copyright 2017 Carlos Daniel Hernandez Mena
 * SPDX-License-Identifier: Apache-2.0
 *
 * Kotlin adaptation of the IPA29, T29, TT, TT_INV and div_sil modules
 * of UNAM-G2P 0.1.0. Ordered replacements preserve the upstream behavior.
 */
internal object UnamG2p {
 private const val VOWELS = "aeiouAEIOU1W"
 private const val WEAK_VOWELS = "iu1W"
 private val whitespace = Regex("\\s+")

 fun transcribeWord(word: String): List<String> {
  val prepared = ("#" + word.applyRules(wordRules))
   .replace("#r", "R").replace("#desh", "des").replace("#", "")
  val finalGlide = (prepared + "#").replace("w#", "u").replace("#", "")
  val mexbet = forward(syllabify(finalGlide)).applyRules(mexbetRules)
  val tokens = (mexbet + "#").replace("T#", " tl ").replace("#", "").replace("T", " t l ")
   .trim().replace(whitespace, " ").replace(" _7", "_7")
  return (if (tokens == "i") "i_7" else tokens)
   .split(' ').filter { it.isNotEmpty() }.map { ipa[it] ?: it }
 }

 private fun forward(word: String): String = "#$word#".applyRules(forwardRules)

 private fun syllabify(word: String): String {
  val transformed = forward(word)
  val pattern = buildString {
   for (letter in transformed) {
    append(when (letter) {
     in WEAK_VOWELS -> 'v'
     in VOWELS -> 'V'
     else -> 'C'
    })
   }
  }
  val boundaries = "#$pattern#".applyRules(clusterRules)
  val divided = buildString {
   var index = 0
   for (symbol in boundaries) {
    append(if (symbol == '.') '.' else transformed[index++])
   }
  }
  return ("#$divided#".applyRules(inverseRules).applyRules(liquidRules) + "#")
   .replace("tl#", ".tl").replace("#", "")
 }

 private fun String.applyRules(rules: List<Pair<String, String>>): String {
  var result = this
  for ((pattern, replacement) in rules) {
   result = result.replace(pattern, replacement)
  }
  return result
 }

 private val wordRules = listOf(
  "zz" to "ts",
  "xa" to "ksa",
  "xe" to "kse",
  "xi" to "ksi",
  "xo" to "kso",
  "xu" to "ksu",
  "xá" to "ksA",
  "xé" to "ksE",
  "xí" to "ksI",
  "xó" to "ksO",
  "xú" to "ksU",
  "xü" to "ksW",
  "xÁ" to "ksA",
  "xÉ" to "ksE",
  "xÍ" to "ksI",
  "xÓ" to "ksO",
  "xÚ" to "ksU",
  "xÜ" to "ksW",
  "xA" to "ksA",
  "xE" to "ksE",
  "xI" to "ksI",
  "xO" to "ksO",
  "xU" to "ksU",
  "xW" to "ksW",
  "xc" to "ks",
  "xs" to "ks",
  "sce" to "se",
  "sci" to "si",
  "scE" to "sE",
  "scI" to "sI",
  "x" to "K",
  "\$" to "s",
  "J" to "j",
  "S" to "8",
  "bv" to "b",
  "v" to "b",
  "z" to "s",
 )

 private val forwardRules = listOf(
  "á" to "A",
  "é" to "E",
  "í" to "I",
  "ó" to "O",
  "ú" to "U",
  "Á" to "A",
  "É" to "E",
  "Í" to "I",
  "Ó" to "O",
  "Ú" to "U",
  "ü" to "W",
  "Ü" to "W",
  "ñ" to "N",
  "Ñ" to "N",
  "ch" to "H",
  "ll" to "L",
  "rr" to "R",
  "tl" to "T",
  "zz" to "Z",
  "ts" to "2",
  "tz" to "3",
  "sh" to "8",
  "#ps" to "P",
  "gue" to "Ge",
  "gui" to "Gi",
  "guE" to "GE",
  "guI" to "GI",
  "que" to "Qe",
  "qui" to "Qi",
  "quE" to "QE",
  "quI" to "QI",
  "ge" to "Xe",
  "gi" to "Xi",
  "gE" to "XE",
  "gI" to "XI",
  "ce" to "5e",
  "ci" to "5i",
  "cE" to "5E",
  "cI" to "5I",
  "y#" to "1",
  "#" to "",
 )

 private val inverseRules = listOf(
  "W" to "ü",
  "N" to "ñ",
  "H" to "ch",
  "L" to "ll",
  "R" to "rr",
  "T" to "tl",
  "Z" to "zz",
  "2" to "ts",
  "3" to "tz",
  "8" to "sh",
  "#P" to "ps",
  "Ge" to "gue",
  "Gi" to "gui",
  "GE" to "guE",
  "GI" to "guI",
  "Qe" to "que",
  "Qi" to "qui",
  "QE" to "quE",
  "QI" to "quI",
  "Xe" to "ge",
  "Xi" to "gi",
  "XE" to "gE",
  "XI" to "gI",
  "5e" to "ce",
  "5i" to "ci",
  "5E" to "cE",
  "5I" to "cI",
  "1#" to "y",
  "A" to "á",
  "E" to "é",
  "I" to "í",
  "O" to "ó",
  "U" to "ú",
  "#" to "",
 )

 private val clusterRules = listOf(
  "#CCCC" to "#kkkk",
  "#CCC" to "#kkk",
  "#CC" to "#kk",
  "#C" to "#k",
  "CCCC#" to "kkkk#",
  "CCC#" to "kkk#",
  "CC#" to "kk#",
  "C#" to "k#",
  "CCCC" to "kk.kk",
  "CCC" to "k.kk",
  "CC" to "k.k",
  "Cv" to ".kv",
  "CV" to ".kV",
  "VV" to "V.V",
  "VV" to "V.V",
  "..." to ".",
  ".." to ".",
  "#." to "",
  ".#" to "",
  "#" to "",
 )

 private val liquidRules = listOf(
  "p.r" to ".pr",
  "p.l" to ".pl",
  "b.r" to ".br",
  "b.l" to ".bl",
  "t.r" to ".tr",
  "t.l" to ".tl",
  "d.r" to ".dr",
  "k.r" to ".kr",
  "k.l" to ".kl",
  "g.r" to ".gr",
  "g.l" to ".gl",
  "c.r" to ".cr",
  "c.l" to ".cl",
  "f.r" to ".fr",
  "f.l" to ".fl",
  ".tsa" to "t.sa",
  ".tse" to "t.se",
  ".tsi" to "t.si",
  ".tso" to "t.so",
  ".tsu" to "t.su",
  ".tza" to "t.za",
  ".tze" to "t.ze",
  ".tzi" to "t.zi",
  ".tzo" to "t.zo",
  ".tzu" to "t.zu",
  ".tsá" to "t.sá",
  ".tsé" to "t.sé",
  ".tsí" to "t.sí",
  ".tsó" to "t.só",
  ".tsú" to "t.sú",
  ".tzá" to "t.zá",
  ".tzé" to "t.zé",
  ".tzí" to "t.zí",
  ".tzó" to "t.zó",
  ".tzú" to "t.zú",
  "n.st" to "ns.t",
  "b.st" to "bs.t",
  "c.st" to "cs.t",
  "k.st" to "ks.t",
 )

 private val mexbetRules = listOf(
  "n.r" to "n.R",
  "l.r" to "l.R",
  "s.r" to "s.R",
  "K" to " k s ",
  "2" to " t s ",
  "h" to "",
  "S" to "8",
  "p" to " p ",
  "b" to " b ",
  "t" to " t ",
  "d" to " d ",
  "k" to " k ",
  "c" to " k ",
  "Q" to " k ",
  "q" to " k ",
  "g" to " g ",
  "G" to " g ",
  "H" to " tS ",
  "f" to " f ",
  "s" to " s ",
  "5" to " s ",
  "P" to " s ",
  "\$" to " s ",
  "8" to " S ",
  "j" to " x ",
  "X" to " x ",
  "J" to " x ",
  "y" to " Z ",
  "L" to " Z ",
  "m" to " m ",
  "n" to " n ",
  ".~" to "~.",
  "n ~" to " n~ ",
  "N" to " n~ ",
  "r" to " r( ",
  "R" to " r ",
  "l" to " l ",
  "a" to " a ",
  "e" to " e ",
  "i" to " i ",
  "o" to " o ",
  "u" to " u ",
  "A" to " a_7 ",
  "E" to " e_7 ",
  "I" to " i_7 ",
  "O" to " o_7 ",
  "U" to " u_7 ",
  "1" to " i ",
  "W" to " u ",
  ".Z" to " t . s ",
  "w u" to " g u ",
  "w" to " g u ",
 )

 private val ipa = mapOf(
  "p" to "p",
  "b" to "b",
  "t" to "t",
  "d" to "d",
  "k" to "k",
  "g" to "g",
  "tS" to "ʧ",
  "f" to "f",
  "s" to "s",
  "S" to "ʃ",
  "x" to "x",
  "Z" to "ʝ",
  "m" to "m",
  "n" to "n",
  "n~" to "ɲ",
  "l" to "l",
  "tl" to "T",
  "r(" to "ɾ",
  "r" to "r",
  "a" to "a",
  "e" to "e",
  "i" to "i",
  "o" to "o",
  "u" to "u",
  "a_7" to "á",
  "e_7" to "é",
  "i_7" to "í",
  "o_7" to "ó",
  "u_7" to "ú",
  "." to ".",
 )
}
