echo "================= nv_avc_dec.dll 64-bit build ================="
$pathbkp=$env:path
$env:path="$env:MINGW_64_HOME/bin;$env:path"
rm *.o
rm nv_avc_dec.dll
& gcc -O2 -I $Env:JAVA_HOME/include/ -I $Env:JAVA_HOME/include/win32/ -I ./include/ -I ./include_win/ -c *.c
# Latest mingw-w64 can include pthreads as well. In that case, we can use '-static -lwinpthread' instead of '-lpthreadVC2'
& gcc -shared -o nv_avc_dec.dll *.o -L./lib_win64 -lavcodec -lavdevice -lavfilter -lavformat -lavutil -lswresample -lswscale -lpthreadVC2

cp nv_avc_dec.dll ../../libs/win64/
rm *.o
$env:path=$pathbkp
