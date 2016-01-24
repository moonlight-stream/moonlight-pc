# alias gcc=gcc-5
rm *.o libnv_avc_dec.dylib
gcc -O3 -I ${JAVA_HOME}/include/ -I ${JAVA_HOME}/include/darwin/ -I ./include -fPIC -c *.c
gcc -shared -Wl,-install_name,libnv_avc_dec.dylib \
  -o libnv_avc_dec.dylib *.o \
  -framework CoreFoundation -framework CoreMedia -framework CoreVideo -framework VideoToolbox -framework VideoDecodeAcceleration \
  -lpthread -L./lib_osx -lavcodec -lavutil -lswscale
rm *.o
