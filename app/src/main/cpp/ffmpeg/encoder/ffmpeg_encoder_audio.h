//
// Created by hihua on 17/11/15.
//

#ifndef BROWSEANIMATE_FFMPEG_ENCODER_AUDIO_H
#define BROWSEANIMATE_FFMPEG_ENCODER_AUDIO_H

#include "../ffmpeg.h"
#include "ffmpeg_encoder.h"

bool ffmpeg_encoder_audio_init(struct EncoderAudioContext *audio_context, AVFormatContext *format, const char *name);
void ffmpeg_encoder_audio_release(struct EncoderAudioContext *audio_context);
void ffmpeg_encoder_audio(JNIEnv *env, jobject obj, struct EncoderAudioContext *audio_context, struct EncoderContext *encoder_context, const uint8_t *data, int data_len, int channels, int samplerate, void (*encoder_frame)(JNIEnv *env, jobject obj, uint8_t *buf, size_t buf_size));

#endif //BROWSEANIMATE_FFMPEG_ENCODE_AUDIO_H
