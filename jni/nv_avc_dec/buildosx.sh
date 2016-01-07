rm *.o libnv_avc_dec.dylib
gcc -O3 -I ${JAVA_HOME}/include/ -I ${JAVA_HOME}/include/darwin/ -I ./inc -fPIC -L. -c *.c
gcc -shared -Wl,-install_name,libnv_avc_dec.dylib -Wl,-undefined,dynamic_lookup -o libnv_avc_dec.dylib *.o -L/usr/local/lib/ -L. -lavcodec -lavfilter -lavformat -lavutil -lswresample -lswscale
rm *.o

