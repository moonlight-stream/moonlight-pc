rm *.o libjnienet.so
gcc -I ${JAVA_HOME}/include/ -I ./enet/include -DHAS_SOCKLEN_T=1 -fPIC -c *.c enet/*.c
gcc -shared -Wl,-soname,libjnienet.so -Wl,--no-undefined -o libjnienet.so *.o
rm *.o

