#!/usr/bin/env bash
set -euo pipefail
ROOT="$PWD"
SDK="${ANDROID_HOME:?ANDROID_HOME required}"
NDK="$SDK/ndk/27.2.12479018"
if [ ! -d "$NDK" ]; then sdkmanager "ndk;27.2.12479018"; fi
TOOL="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
curl -fL --retry 3 https://downloads.sourceforge.net/lame/lame-3.100.tar.gz -o "$WORK/lame.tar.gz"
tar -xzf "$WORK/lame.tar.gz" -C "$WORK"
cd "$WORK/lame-3.100"
export CC="$TOOL/aarch64-linux-android26-clang" AR="$TOOL/llvm-ar" RANLIB="$TOOL/llvm-ranlib" STRIP="$TOOL/llvm-strip"
export CFLAGS="-O2 -fPIC -Wno-error=implicit-function-declaration"
./configure --host=aarch64-linux-android --disable-frontend --disable-decoder --disable-shared --enable-static --with-pic
make -C libmp3lame -j2
mkdir -p "$ROOT/app/src/main/jniLibs/arm64-v8a" "$ROOT/app/src/main/assets/licenses"
"$CC" -shared -O2 -Iinclude "$ROOT/app/src/main/cpp/mp3.c" libmp3lame/.libs/libmp3lame.a -lm -Wl,-z,max-page-size=16384 -o "$ROOT/app/src/main/jniLibs/arm64-v8a/libvoxmp3.so"
cp COPYING "$ROOT/app/src/main/assets/licenses/LAME-LGPL.txt"
