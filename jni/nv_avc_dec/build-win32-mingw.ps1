echo "================= nv_avc_dec.dll 32-bit build ================="
$cc="$env:MINGW_32_HOME/bin/gcc"
rm *.o
rmdir -Recurse win32
& $cc -I $Env:JAVA_HOME/include/ -I $Env:JAVA_HOME/include/win32/ -I ./include/ -I ./include_win/ -c *.c
mkdir win32 | Out-Null
& $cc -shared -o win32/nv_avc_dec.dll *.o -L./lib_win32 -lavcodec -lavdevice -lavfilter -lavformat -lavutil -lswresample -lswscale -lpthreadVC2

cp win32/nv_avc_dec.dll ../../libs/win32/
rm *.o
