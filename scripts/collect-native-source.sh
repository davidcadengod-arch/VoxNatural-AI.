#!/usr/bin/env bash
set -euo pipefail

runtime=${1:?Expected Sherpa version}
out=${2:?Expected output directory}
mkdir -p "$out"
cd "$out"

fetch() {
  local url=$1 name=$2 sha=$3
  curl -fL --retry 3 "$url" -o "$name"
  printf '%s  %s\n' "$sha" "$name" | sha256sum -c -
  printf '%s  %s\n' "$sha" "$name" >> SHA256SUMS
  printf '%s  %s\n' "$name" "$url" >> SOURCES.txt
}

: > SHA256SUMS
: > SOURCES.txt
case "$runtime" in
  1.13.8)
    fetch https://github.com/k2-fsa/sherpa-onnx/archive/11afbd009a7f8c08f4bcf2fc1b265d0df4670fbf.tar.gz \
      sherpa-1.13.8.tar.gz 0a8db6c55dd318f4a688faba85f7760b99a6c92e8ef8864479d418531bee1ac2
    fetch https://github.com/csukuangfj/espeak-ng/archive/ed530aa113046142eb5115cf2fc9157854d0ffe1.zip \
      espeak-ng.zip e4e262cbe34f7fe21f91f1ba3397f2728e1f30eafbae7853f2b753a9ed13f0dd
    fetch https://github.com/csukuangfj/piper-phonemize/archive/f3ff95afc03640bc1399e113e83361192a2fafb4.zip \
      piper-phonemize.zip d9cca4e2bdc7d6dd8dffb96a4668283dbd3f77a9c194a3e530c1e8eba9406a5d
    ;;
  2.0.1)
    fetch https://github.com/k2-fsa/sherpa-onnx/archive/af2d8d81dbfa61c4cea4c568da78f3982edca005.tar.gz \
      sherpa-2.0.1.tar.gz afcafe3c34bc2f15a363ad791fa1644a69bf0977a58c1b4f617468976c2faeb5
    ;;
  *)
    echo "Unsupported Sherpa version: $runtime" >&2
    exit 2
    ;;
esac

cat > README.txt <<'NOTICE'
These snapshots match the source pins in Sherpa's build definitions.
A byte-for-byte reproduction of the prebuilt AAR was not performed.
Consult THIRD_PARTY_NOTICES.md for unresolved dependencies and obligations.
NOTICE
