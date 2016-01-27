echo "================= nv_avc_dec.dll 32-bit build ================="
$pathbkp=$env:path
$env:path="$env:MINGW_32_HOME/bin;$env:path"
rm *.o
rm nv_avc_dec.dll
& gcc -O2 -I $Env:JAVA_HOME/include/ -I $Env:JAVA_HOME/include/win32/ -I ./include/ -c *.c
& gcc -shared -o nv_avc_dec.dll *.o "-Wl,--kill-at" -L./lib_win32 -lavcodec -lavutil -lswresample -lswscale -lpthreadVC2

cp nv_avc_dec.dll ../../libs/win32/
rm *.o
$env:path=$pathbkp