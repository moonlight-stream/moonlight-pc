rm *.o libnv_opus_dec.dylib
gcc -O3 -I include -I ${JAVA_HOME}/include/ -I ${JAVA_HOME}/include/darwin/ -I ./inc -fPIC -c *.c
gcc -shared -Wl,-install_name,libnv_opus_dec.dylib \
  -o libnv_opus_dec.dylib *.o -L./osx -lopus
rm *.o

