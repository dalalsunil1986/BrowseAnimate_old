//
// Created by hihua on 17/11/15.
//

#ifndef BROWSEANIMATE_FFMPEG_ENCODER_H
#define BROWSEANIMATE_FFMPEG_ENCODER_H

#include "../ffmpeg.h"

struct EncoderAudioContext {
    int bitrate;
    int channels;
    int samplerate;

    AVCodecContext *context;
    AVAudioFifo *fifo;
    AVFrame *frame;
    int stream;
    int pts;

    struct AudioSwr swr;
};

struct EncoderVideoContext {
    int bitrate;
    int width;
    int height;
    int framerate;
    enum AVPixelFormat fmt;

    AVCodecContext *context;
    struct SwsContext *sws;
    AVFrame *frame;
    int stream;
    int64_t pts;
};

struct EncoderContext {
    AVFormatContext *format;
    AVPacket *packet;
    int64_t timer;

    struct EncoderAudioContext audio_context;
    struct EncoderVideoContext video_context;
};

#endif //BROWSEANIMATE_FFMPEG_ENCODER_H
