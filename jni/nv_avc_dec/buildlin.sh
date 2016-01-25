rm *.o libnv_avc_dec.so
gcc -I /usr/lib/jvm/java-7-openjdk-amd64/include/ -I ./include -fPIC -L. -c *.c
gcc -shared -Wl,-soname,libnv_avc_dec.so -Wl,--no-undefined -Wl,-Bsymbolic -o libnv_avc_dec.so *.o -L. -lavcodec -lavutil -lswresample -lswscale -lm -pthread
rm *.o

