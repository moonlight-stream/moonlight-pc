#!/bin/sh

echo "Building Gamepad JNI"
(cd gamepad_jni && sh buildosx.sh)

echo "Building NV AVC Decoder"
(cd nv_avc_dec && sh buildosx.sh)

echo "Building NV OPUS Decoder"
(cd nv_opus_dec && sh buildosx.sh)

echo "Copying to libs/osx/"

cp gamepad_jni/libgamepad_jni.dylib ../libs/osx/libgamepad_jni.dylib 
cp nv_avc_dec/libnv_avc_dec.dylib ../libs/osx/libnv_avc_dec.dylib
cp nv_opus_dec/libnv_opus_dec.dylib ../libs/osx/libnv_opus_dec.dylib 
