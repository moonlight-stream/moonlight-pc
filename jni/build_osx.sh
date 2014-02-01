#!/bin/sh

echo "Building Gamepad JNI"
(cd gamepad_jni && sh buildosx.sh)

echo "Building NV AVC Decoder"
(cd nv_avc_dec && sh buildosx.sh)

echo "Building NV OPUS Decoder"
(cd nv_opus_dec && sh buildosx.sh)

echo "Linking to jni/"

ln -s gamepad_jni/libgamepad_jni.dylib libgamepad_jni.dylib 
ln -s nv_avc_dec/libnv_avc_dec.dylib libnv_avc_dec.dylib
ln -s nv_opus_dec/libnv_opus_dec.dylib libnv_opus_dec.dylib 
