package com.voxnatural.app
import android.content.Context
import org.json.JSONObject
class Pronunciation(private val context:Context){
 private val prefs=context.getSharedPreferences("pronunciation",0)
 fun entries():JSONObject=JSONObject(prefs.getString("rules","{}")?:"{}")
 fun save(word:String,spoken:String){require(word.isNotBlank());val j=entries();if(spoken.isBlank())j.remove(word) else j.put(word,spoken);prefs.edit().putString("rules",j.toString()).apply()}
 fun apply(text:String):String{
  val j=entries();val keys=j.keys().asSequence().filter{it.isNotBlank()}.sortedByDescending{it.length}.toList()
  if(keys.isEmpty())return text
  val pattern=Regex("(?<![\\p{L}\\p{N}])(?:"+keys.joinToString("|"){Regex.escape(it)}+")(?![\\p{L}\\p{N}])")
  return pattern.replace(text){j.optString(it.value,it.value)}
 }
}
