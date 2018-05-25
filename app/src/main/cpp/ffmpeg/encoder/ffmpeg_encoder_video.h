//
// Created by hihua on 17/11/15.
//

#ifndef BROWSEANIMATE_FFMPEG_ENCODER_VIDEO_H
#define BROWSEANIMATE_FFMPEG_ENCODER_VIDEO_H

#include "../ffmpeg.h"
#include "ffmpeg_encoder.h"

bool ffmpeg_encoder_video_init(struct EncoderVideoContext *video_context, AVFormatContext *format, const char *name, const AVDictionary *priv);
void ffmpeg_encoder_video_release(struct EncoderVideoContext *video_context);
void ffmpeg_encoder_video(JNIEnv *env, jobject obj, struct EncoderVideoContext *video_context, struct EncoderContext *encoder_context, const uint8_t *data, int data_len, int width, int height, enum AVPixelFormat fmt, void (*encoder_frame)(JNIEnv *env, jobject obj, uint8_t *buf, size_t buf_size));

#endif //BROWSEANIMATE_FFMPEG_ENCODE_VIDEO_H
