rm *.o libnv_opus_dec.dylib
gcc -I include -I ${JAVA_HOME}/include/ -I ./inc -fPIC -c *.c
gcc -shared -Wl,-install_name,libnv_opus_dec.dylib -Wl,-undefined,dynamic_lookup -o libnv_opus_dec.dylib *.o -L. -lopus
rm *.o

