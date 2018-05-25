//
// Created by hihua on 17/11/29.
//

#ifndef BROWSEANIMATE_FFMPEG_MAIN_H
#define BROWSEANIMATE_FFMPEG_MAIN_H

#include "../main.h"

#define LOG_FFMPEG_TAG		"browseanimate_ffmpeg_jni"

#define LOG_FFMPEG_I(...)	__android_log_print(ANDROID_LOG_INFO, LOG_FFMPEG_TAG, __VA_ARGS__)
#define LOG_FFMPEG_D(...)	__android_log_print(ANDROID_LOG_DEBUG, LOG_FFMPEG_TAG, __VA_ARGS__)
#define LOG_FFMPEG_E(...)	__android_log_print(ANDROID_LOG_ERROR, LOG_FFMPEG_TAG, __VA_ARGS__)

#define FFMPEGENCODER               com_hihua_browseanimate_ffmpeg_FFmpegEncoder
#define FFMPEGDECODER               com_hihua_browseanimate_ffmpeg_FFmpegDecoder
#define FFMPEGSURFACE               com_hihua_browseanimate_ffmpeg_FFmpegSurface

#define FUN_FFMPEG_ENCODER(FUNC) 	FUNCS(FFMPEGENCODER, FUNC)
#define FUN_FFMPEG_DECODER(FUNC) 	FUNCS(FFMPEGDECODER, FUNC)
#define FUN_FFMPEG_SURFACE(FUNC) 	FUNCS(FFMPEGSURFACE, FUNC)

#endif //BROWSEANIMATE_FFMPEG_MAIN_H
