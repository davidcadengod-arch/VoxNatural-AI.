#!/usr/bin/env bash
set -euo pipefail
ROOT="$PWD"
SDK="${ANDROID_HOME:?ANDROID_HOME required}"
NDK="$SDK/ndk/27.2.12479018"
if [ ! -x "$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android26-clang" ]; then
  # Do not depend on sdkmanager being available on PATH.
  SDKMANAGER="$(command -v sdkmanager || true)"
  if [ -z "$SDKMANAGER" ]; then
    SDKMANAGER="$(find "$SDK/cmdline-tools" -type f -name sdkmanager -print -quit 2>/dev/null || true)"
  fi
  if [ -n "$SDKMANAGER" ]; then
    "$SDKMANAGER" --sdk_root="$SDK" "ndk;27.2.12479018"
  else
    NDK_TMP="$(mktemp -d)"
    curl -fL --retry 3 https://dl.google.com/android/repository/android-ndk-r27c-linux.zip -o "$NDK_TMP/ndk.zip"
    unzip -q "$NDK_TMP/ndk.zip" -d "$NDK_TMP"
    mkdir -p "$SDK/ndk"
    if [ -d "$NDK" ]; then mv "$NDK" "$NDK_TMP/previous-ndk"; fi
    mv "$NDK_TMP/android-ndk-r27c" "$NDK"
    rm -rf "$NDK_TMP"
  fi
fi
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
mkdir -p "$ROOT/compliance/lame"
"$CC" -c -O2 -fPIC -Iinclude "$ROOT/app/src/main/cpp/mp3.c" -o "$ROOT/compliance/lame/mp3.o"
"$CC" -shared "$ROOT/compliance/lame/mp3.o" libmp3lame/.libs/libmp3lame.a -lm -Wl,-z,max-page-size=16384 -o "$ROOT/app/src/main/jniLibs/arm64-v8a/libvoxmp3.so"
cp "$WORK/lame.tar.gz" "$ROOT/compliance/lame/"
cp "$ROOT/app/src/main/cpp/mp3.c" "$ROOT/scripts/build-mp3.sh" COPYING "$ROOT/compliance/lame/"
printf '%s\n' 'Use Android NDK r27c arm64 API 26 clang to build LAME as in build-mp3.sh, then clang -shared mp3.o libmp3lame.a -lm -Wl,-z,max-page-size=16384 -o libvoxmp3.so. Replacement APK must be re-signed by recipient.' > "$ROOT/compliance/lame/RELINK.txt"
cp COPYING "$ROOT/app/src/main/assets/licenses/LAME-LGPL.txt"
