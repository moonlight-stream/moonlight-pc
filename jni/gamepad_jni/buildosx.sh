rm *.o libgamepad_jni.dylib
gcc -O3 -I ${JAVA_HOME}/include/ -I ${JAVA_HOME}/include/darwin -fPIC -c *.c
gcc -shared -o libgamepad_jni.dylib *.o -L./osx -lstem_gamepad -lpthread -framework IOKit -framework CoreFoundation
rm *.o

