//
// Created by hihua on 17/11/24.
//

#ifndef BROWSEANIMATE_FFMPEG_DECODER_VIDEO_H
#define BROWSEANIMATE_FFMPEG_DECODER_VIDEO_H

#include "ffmpeg_decoder.h"

bool ffmpeg_decoder_video_init(struct DecoderVideoContext *video_context, AVStream *stream);
void ffmpeg_decoder_video_release(struct DecoderVideoContext *video_context);
void ffmpeg_decoder_video(struct DecoderVideoContext *video_context, AVFormatContext *format);
void ffmpeg_decoder_video_display(JNIEnv *env, jobject obj, struct DecoderVideoContext *video_context, struct DecoderContext *decoder_context, void (*display_frame)(AVFrame *frame));

#endif //BROWSEANIMATE_FFMPEG_DECODER_VIDEO_H
