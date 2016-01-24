These libs were compiled from ffmpeg __v2.8.5__ with all features stripped out, except h264 decoders and hw acceleration.

The following configuration command was used:
```sh
./configure \
--arch=x86_64 --enable-pic --prefix=ffmpeg-dist \
--disable-debug --disable-mips32r5 --disable-mips64r6 --disable-mipsdspr1 --disable-mipsdspr2 --disable-msa --disable-mipsfpu --disable-mmi \
--disable-everything --enable-hwaccel=h264_vda --enable-hwaccel=h264_videotoolbox --enable-videotoolbox --enable-decoder=h264 --enable-decoder=h264_vda \
--disable-iconv --disable-securetransport --disable-xlib --disable-zlib --disable-lzma  --disable-bzlib --disable-postproc --disable-avformat --disable-avfilter --disable-doc --disable-programs \
--enable-version3
```