rm *.o libjnienet.dylib
gcc -O3 -I ${JAVA_HOME}/include/ -I ${JAVA_HOME}/include/darwin/ -I ./enet/include -fPIC -c *.c enet/*.c
gcc -shared -Wl,-install_name,libjnienet.dylib -o libjnienet.dylib *.o
rm *.o

