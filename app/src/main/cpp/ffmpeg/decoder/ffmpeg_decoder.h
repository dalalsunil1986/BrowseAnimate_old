//
// Created by hihua on 17/11/23.
//

#ifndef BROWSEANIMATE_FFMPEG_DECODER_H
#define BROWSEANIMATE_FFMPEG_DECODER_H

#include "../ffmpeg.h"
#include "../ffmpeg_buffer.h"
#include "../ffmpeg_packet.h"
#include "../ffmpeg_surface.h"

#define FRAME_MAX_VIDEO 3

struct Frame {
    AVFrame *frame;
    double pts;
    double duration;
};

struct Frames {
    size_t read_index;
    size_t write_index;
    size_t frame_total;
    struct Frame frames[FRAME_MAX_VIDEO];
    pthread_mutex_t section;
    bool inited;
};

struct DecoderAudioContext {
    AVCodecContext *context;
    AVStream *stream;
    AVFrame *frame;

    struct AudioSwr swr;
    struct Packets packets;
};

struct DecoderVideoContext {
    AVCodecContext *context;
    AVStream *stream;

    struct Packets packets;
    struct Frames frames;
};

struct DecoderContext {
    AVFormatContext *format;
    int audio_stream;
    int video_stream;
    double audio_timer;
    double video_timer;
    double remaining;
    bool stop;

    struct DecoderAudioContext audio_context;
    struct DecoderVideoContext video_context;
    struct Buffers buffers;
};

#endif //BROWSEANIMATE_FFMPEG_DECODER_H
