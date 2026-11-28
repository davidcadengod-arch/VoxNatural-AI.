"""Check the offline assets and native ABI in a VoxNatural APK."""

import argparse
from pathlib import Path
from zipfile import ZipFile


MODELS = {
    "kokoro": ("model.onnx", "voices.bin", "tokens.txt"),
    "piper": ("model.onnx", "tokens.txt"),
    "pocket": (
        "lm_flow.int8.onnx",
        "lm_main.int8.onnx",
        "encoder.onnx",
        "decoder.int8.onnx",
        "text_conditioner.onnx",
        "vocab.json",
        "token_scores.json",
        "reference.wav",
        "reference.sha256",
        "VOICE-NOTICE.txt",
        "LICENSE",
    ),
    "supertonic": (
        "duration_predictor.int8.onnx",
        "text_encoder.int8.onnx",
        "vector_estimator.int8.onnx",
        "vocoder.int8.onnx",
        "tts.json",
        "unicode_indexer.bin",
        "voice.bin",
        "LICENSE",
    ),
}


def verify(path: Path) -> None:
    required = {
        f"assets/{engine}/{filename}"
        for engine, filenames in MODELS.items()
        for filename in filenames
    }
    required.update(
        (
            "lib/arm64-v8a/libvoxmp3.so",
            "lib/arm64-v8a/libsherpa-onnx-jni.so",
            "assets/licenses/THIRD_PARTY_NOTICES.md",
            "assets/licenses/LAME-LGPL.txt",
        )
    )
    with ZipFile(path) as apk:
        entries = {entry.filename: entry.file_size for entry in apk.infolist()}
        missing = sorted(name for name in required if not entries.get(name))
        if missing:
            raise ValueError("Missing or empty APK entries: " + ", ".join(missing))
        abis = {
            name.split("/")[1]
            for name in entries
            if name.startswith("lib/") and name.endswith(".so")
        }
        if abis != {"arm64-v8a"}:
            raise ValueError(f"Unexpected native ABIs: {sorted(abis)}")
        for engine in ("kokoro", "piper"):
            data = f"assets/{engine}/espeak-ng-data/"
            if not any(name.startswith(data) and size for name, size in entries.items()):
                raise ValueError(f"Missing phonemizer data for {engine}")
        corrupt = apk.testzip()
        if corrupt:
            raise ValueError(f"Corrupt APK entry: {corrupt}")
    print(f"{path}: integrity, ARM64, and all four offline model bundles verified")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    verify(parser.parse_args().apk)
