//
// Created by hihua on 17/11/29.
//

#include "ffmpeg_main.h"
#include "ffmpeg.h"

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    ffmpeg_init();

    return JNI_VERSION_1_4;
}

void JNI_OnUnload(JavaVM vm, void *reserved) {
    ffmpeg_free();
}