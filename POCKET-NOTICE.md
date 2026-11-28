# PocketTTS Spanish

© Kyutai. VoxNatural uses Kyutai's Spanish Pocket TTS model converted to ONNX and quantized to INT8 by Alejandro Cordón for Hushfox. The tokenizer was converted to JSON. The upstream model and the export are credited under CC BY 4.0.

- [Hushfox export and conversion details](https://huggingface.co/alejandrocordon/hushfox-pocket-tts-es/blob/44c6457360bdc6983d50b35e339b8473f70870f5/README.md)
- [Kyutai source revision](https://huggingface.co/kyutai/pocket-tts/tree/3e82814a68665eec246ff649b14c71331f955c06)
- [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)

The Hushfox card identifies the source as `languages/spanish/model.safetensors` with SHA-256 `ca87ef1aadc9280a97358a5250d8a508e15b5c10d502e8e1c1d4afd4d784de96`. The model repository requires access to independently download the original weights. Executable builds verify the seven export files and the license against the hashes in `scripts/fetch-pocket-es.sh`.

The bundled Alex reference is a synthetic `em_alex` example from lineofflight's CC BY 4.0 collection, based on hexgrad's Kokoro-82M v1.0. The source excerpt is pinned and verified. The script repeats it with a short pause if the original is shorter than three seconds. The output hash and attribution are bundled under `assets/pocket/`. The source collection describes the generation of the samples and use of gated voice-cloning weights for its state files. Review applicable terms for the reference path before public redistribution.

- [Alex source WAV](https://huggingface.co/lineofflight/pocket-tts-kokoro-voices/blob/e430d39794bfb25f8f7ad099d3c9f594947ff00e/samples/em_alex.wav)
- [Alex collection methodology and attribution](https://huggingface.co/lineofflight/pocket-tts-kokoro-voices/blob/e430d39794bfb25f8f7ad099d3c9f594947ff00e/README.md)
- [Kokoro-82M v1.0 source](https://huggingface.co/hexgrad/Kokoro-82M/tree/f3ff3571791e39611d31c381e3a41a3af07b4987)
