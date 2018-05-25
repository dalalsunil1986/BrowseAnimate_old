//
// Created by hihua on 17/12/22.
//

#ifndef BROWSEANIMATE_FFMPEG_SURFACE_H
#define BROWSEANIMATE_FFMPEG_SURFACE_H

#include "ffmpeg.h"
#include <system/window.h>
#include <hardware/gralloc.h>
#include <android/native_window_jni.h>

struct SurfaceContext {
    ANativeWindow *window;
    int width;
    int height;
    int format;
    int size;
};

void ffmpeg_surface_release();

bool ffmpeg_surface_yuv_init(JNIEnv *env, jobject surface);
bool ffmpeg_surface_yuv_display(int width, int height, int fmt, const uint8_t *data_ptr[3], const int data_size[3]);

#endif //BROWSEANIMATE_FFMPEG_SURFACE_H
