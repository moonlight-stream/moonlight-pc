# OSX Native Build Instructions

## Dependencies

### Gamepad JNI

You shouldn't need anything special to build this.

### NV AVC Decoder

To build the NV AVC Decoder you'll need ffmpeg, which will bring along libavcodec and its friends.

### NV Opus Decoder

To build the NV Opus Decoder you'll need opus (libopus). 

## Building Natively

To automatically build the 3 native libraries, run 

    build_osx.sh

from the `jni` directory.

This will build the osx versions each of the libraries, and then symlink them conveniently into the top level directory. 

## Including the Binaries

First, create a file called `platform` in `jni`, and make its first line `osx`.
If you aren't sure how best to that, try `echo osx > platform`.

Then, in your Java build environment (e.g. IDE), add the JVM flag:

    -Djava.library.path=[limelight-pc]/jni


