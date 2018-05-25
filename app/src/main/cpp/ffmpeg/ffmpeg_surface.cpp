//
// Created by hihua on 17/12/22.
//

#include "ffmpeg_surface.h"
#include <libavutil/imgutils.h>

SurfaceContext surface_context = { NULL, 0, 0, HAL_PIXEL_FORMAT_YV12, 0 };

bool ffmpeg_surface_yuv_init(JNIEnv *env, jobject surface) {
    surface_context.window = ANativeWindow_fromSurface(env, surface);
    if (surface_context.window != NULL) {
        if (native_window_set_scaling_mode(surface_context.window, NATIVE_WINDOW_SCALING_MODE_SCALE_TO_WINDOW) == 0) {
            if (native_window_set_buffers_format(surface_context.window, surface_context.format) == 0) {
                if (ANativeWindow_getFormat(surface_context.window) == surface_context.format)
                    return true;
            }
        }

        ffmpeg_surface_release();
    }

    return false;
}

void ffmpeg_surface_release() {
    if (surface_context.window != NULL) {
        ANativeWindow_release(surface_context.window);
        surface_context.window = NULL;
    }

    surface_context.width = 0;
    surface_context.height = 0;
    surface_context.size = 0;
}

bool ffmpeg_surface_yuv_display(int width, int height, int fmt, const uint8_t *data_ptr[3], const int data_size[3]) {
    if (surface_context.window != NULL) {
        if (surface_context.width != width || surface_context.height != height) {
            native_window_set_buffers_user_dimensions(surface_context.window, width, height);
            //native_window_set_buffers_dimensions(surface_context.window, width, height);
            surface_context.width = width;
            surface_context.height = height;
            surface_context.size = av_image_get_buffer_size((enum AVPixelFormat)fmt, width, height, 16);
        }

        ANativeWindow_Buffer buffer;

        if (ANativeWindow_lock(surface_context.window, &buffer, NULL) == 0) {
            if (buffer.format == HAL_PIXEL_FORMAT_YV12) {
                uint8_t *ptr = (uint8_t *)buffer.bits;

                if (buffer.stride % 32 == 0) {
                    uint8_t *dst_ptr[] = { NULL, NULL, NULL, NULL };
                    int dst_size[] = { buffer.stride, buffer.stride / 2, buffer.stride / 2, 0 };

                    av_image_fill_pointers(dst_ptr, (enum AVPixelFormat)fmt, buffer.height, ptr, dst_size);
                    av_image_copy(dst_ptr, dst_size, data_ptr, data_size, (enum AVPixelFormat)fmt, width, height);
                } else
                    av_image_copy_to_buffer(ptr, surface_context.size, data_ptr, data_size, (enum AVPixelFormat)fmt, buffer.width, buffer.height, 16);
            }

            ANativeWindow_unlockAndPost(surface_context.window);
            return true;
        }
    }

    return false;
}

#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT void JNICALL FUN_FFMPEG_SURFACE(ffmpegSurfaceSet)(JNIEnv *env, jobject obj, jobject surface) {
    ffmpeg_surface_release();
    ffmpeg_surface_yuv_init(env, surface);
}

JNIEXPORT void JNICALL FUN_FFMPEG_SURFACE(ffmpegSurfaceRelease)(JNIEnv *env, jobject obj) {
    ffmpeg_surface_release();
}
#ifdef __cplusplus
}
#endif