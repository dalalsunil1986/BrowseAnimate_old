//
// Created by hihua on 17/10/27.
//

#ifndef BROWSEANIMATE_FFMPEG_H
#define BROWSEANIMATE_FFMPEG_H

#include "ffmpeg_main.h"

#ifdef __cplusplus
extern "C"
{
#endif

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswresample/swresample.h>
#include <libswscale/swscale.h>
#include <libavutil/avutil.h>
#include <libavutil/imgutils.h>
#include <libavutil/opt.h>
#include <libavutil/audio_fifo.h>
#include <libavutil/time.h>

struct AudioSwr {
    struct SwrContext *context;
    uint8_t *buf;
    size_t buf_size;
};

void ffmpeg_init();
void ffmpeg_free();

int ffmpeg_write_frame_buffer(JNIEnv *env, jobject obj, AVFormatContext *format, AVPacket *packet, void (*encoder_frame)(JNIEnv *env, jobject obj, uint8_t *buf, size_t buf_size));

#ifdef __cplusplus
}
#endif

#endif //BROWSEANIMATE_FFMPEG_H
