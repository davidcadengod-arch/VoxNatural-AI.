#!/usr/bin/env bash
set -euo pipefail
# Staging only; not bundled until runtime and reference voice are validated.
out=${1:?Specify staging directory}
mkdir -p "$out"
base=https://huggingface.co/alejandrocordon/hushfox-pocket-tts-es/resolve/main
cd "$out"
curl -fL --retry 3 "$base/lm_flow.int8.onnx" -o "lm_flow.int8.onnx.tmp"
printf '%s  %s\n' 'bb929fba3fdeec8dd7532229673273b5ea214c88581164e4587ea9ce38f96065' 'lm_flow.int8.onnx.tmp' | sha256sum -c -
mv 'lm_flow.int8.onnx.tmp' 'lm_flow.int8.onnx'
curl -fL --retry 3 "$base/lm_main.int8.onnx" -o "lm_main.int8.onnx.tmp"
printf '%s  %s\n' '6ac12c7e93729bf63262c4c687cc8f475f7218ab41121575bb92a316f77e84c0' 'lm_main.int8.onnx.tmp' | sha256sum -c -
mv 'lm_main.int8.onnx.tmp' 'lm_main.int8.onnx'
curl -fL --retry 3 "$base/encoder.onnx" -o "encoder.onnx.tmp"
printf '%s  %s\n' '16b8f8c4e809a885188c4d9d6c654086460b19e17e00865ea27b92bec22d69ae' 'encoder.onnx.tmp' | sha256sum -c -
mv 'encoder.onnx.tmp' 'encoder.onnx'
curl -fL --retry 3 "$base/decoder.int8.onnx" -o "decoder.int8.onnx.tmp"
printf '%s  %s\n' '952e3bb179dcfbcaa50f79a7b133cd94b7c06f50aea108eaa4e8d701197b72ff' 'decoder.int8.onnx.tmp' | sha256sum -c -
mv 'decoder.int8.onnx.tmp' 'decoder.int8.onnx'
curl -fL --retry 3 "$base/text_conditioner.onnx" -o "text_conditioner.onnx.tmp"
printf '%s  %s\n' '31279075102a6d4d046ce10da36c5916d7fe76a0fa5e69098ee4accdef8d505e' 'text_conditioner.onnx.tmp' | sha256sum -c -
mv 'text_conditioner.onnx.tmp' 'text_conditioner.onnx'
curl -fL --retry 3 "$base/vocab.json" -o "vocab.json.tmp"
printf '%s  %s\n' 'fff363eaaccbd86d0bdfbddbc5cb37bf2d7a806db9f46b883ab8f17a7ca8f4b2' 'vocab.json.tmp' | sha256sum -c -
mv 'vocab.json.tmp' 'vocab.json'
curl -fL --retry 3 "$base/token_scores.json" -o "token_scores.json.tmp"
printf '%s  %s\n' '77ee74a998f592a9e2e36f9529c29d3f479e4f56999efb29b5200beb47c26877' 'token_scores.json.tmp' | sha256sum -c -
mv 'token_scores.json.tmp' 'token_scores.json'
curl -fL --retry 3 "$base/LICENSE" -o LICENSE
test -s LICENSE
