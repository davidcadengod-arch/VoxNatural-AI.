package com.voxnatural.app

import com.k2fsa.sherpa.onnx.OfflineTts
import java.io.File

internal const val USES_ESPEAK_DATA = true

internal class ModelPhonemizer(tokens: File) {
 fun generate(tts: OfflineTts, text: String, sid: Int, speed: Float) =
  tts.generate(text, sid, speed)
}
