package com.voxnatural.app

import java.io.File
import java.util.zip.ZipInputStream
import org.xmlpull.v1.XmlPullParser
import android.util.Xml
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

internal object DocumentText {
 fun read(file: File, displayName: String): String {
  val text = when (displayName.substringAfterLast(".").lowercase()) {
   "pdf" -> PDDocument.load(file).use { PDFTextStripper().getText(it) }
   "docx" -> readWord(file)
   "txt" -> file.readText()
   else -> error("Formato no compatible")
  }
  require(text.isNotBlank()) { "No se encontró texto digital" }
  return text
 }
 private fun readWord(file:File):String {
  ZipInputStream(file.inputStream()).use { z ->
   while(true){ val entry=z.nextEntry?:break
    if(entry.name=="word/document.xml"){
     val p=Xml.newPullParser(); p.setInput(z,"UTF-8"); val b=StringBuilder(); var inText=false
     while(p.eventType!=XmlPullParser.END_DOCUMENT){
      val n=p.name?.substringAfter(":")
      when(p.eventType){
       XmlPullParser.START_TAG->when(n){"t"->inText=true;"tab"->b.append('\t');"br"->b.append('\n')}
       XmlPullParser.TEXT->if(inText)b.append(p.text)
       XmlPullParser.END_TAG->when(n){"t"->inText=false;"p"->b.append('\n')}
      }; p.next()
     }; return b.toString()
    }
   }
  }; error("DOCX inválido")
 }
}
