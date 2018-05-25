//
// Created by hihua on 17/11/24.
//

#ifndef BROWSEANIMATE_FFMPEG_DECODER_AUDIO_H
#define BROWSEANIMATE_FFMPEG_DECODER_AUDIO_H

#include "ffmpeg_decoder.h"

bool ffmpeg_decoder_audio_init(struct DecoderAudioContext *audio_context, AVStream *stream);
void ffmpeg_decoder_audio_release(struct DecoderAudioContext *audio_context);
void ffmpeg_decoder_audio(JNIEnv *env, jobject obj, struct DecoderAudioContext *audio_context, struct DecoderContext *decoder_context, void (*decoder_frame)(JNIEnv *env, jobject obj, uint8_t *buf, size_t buf_size));
void ffmpeg_decoder_audio_play(JNIEnv *env, jobject obj, struct DecoderAudioContext *audio_context, struct DecoderContext *decoder_context, void (*decoder_frame)(JNIEnv *env, jobject obj, uint8_t *buf, size_t buf_size));

#endif //BROWSEANIMATE_FFMPEG_DECODER_AUDIO_H
