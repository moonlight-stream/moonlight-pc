echo "================= nv_avc_dec.dll 64-bit build ================="
$cc="$env:MINGW_64_HOME/bin/gcc"
rm *.o
rmdir -Recurse win64
& $cc -I $Env:JAVA_HOME/include/ -I $Env:JAVA_HOME/include/win32/ -I ./include/ -I ./include_win/ -c *.c
mkdir win64 | Out-Null
# Latest mingw-w64 can include pthreads as well. In that case, we can use '-static -lwinpthread' instead of '-lpthreadVC2'
& $cc -shared -o win64/nv_avc_dec.dll *.o -L./lib_win64 -lavcodec -lavdevice -lavfilter -lavformat -lavutil -lswresample -lswscale -lpthreadVC2

cp win64/nv_avc_dec.dll ../../libs/win64/
rm *.o
