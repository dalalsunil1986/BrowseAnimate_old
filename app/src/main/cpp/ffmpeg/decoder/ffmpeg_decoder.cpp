//
// Created by hihua on 17/11/23.
//

#include <json/json.h>
#include "ffmpeg_decoder.h"
#include "ffmpeg_decoder_audio.h"
#include "ffmpeg_decoder_video.h"

struct DecoderContext decoder_context;

bool ffmpeg_decoder_audio_create(JNIEnv *env, jobject obj, struct DecoderAudioContext *audio_context) {
    AVCodecContext *context = audio_context->context;

    jclass class_obj = env->GetObjectClass(obj);
    jmethodID method = env->GetMethodID(class_obj, "decoderAudioCreate", "(II)Z");
    jboolean success = env->CallBooleanMethod(obj, method, context->sample_rate, context->channels);

    env->DeleteLocalRef(class_obj);
    return success;
}

bool ffmpeg_decoder_video_create(JNIEnv *env, jobject obj, struct DecoderVideoContext *video_context) {
    AVCodecContext *context = video_context->context;

    jclass class_obj = env->GetObjectClass(obj);
    jmethodID method = env->GetMethodID(class_obj, "decoderVideoCreate", "(III)Z");
    jboolean success = env->CallBooleanMethod(obj, method, context->width, context->height, context->pix_fmt);

    env->DeleteLocalRef(class_obj);
    return success;
}

void ffmpeg_decoder_audio_frame(JNIEnv *env, jobject obj, uint8_t *buf, size_t buf_size) {
    jbyteArray buffer_array = env->NewByteArray(buf_size);
    env->SetByteArrayRegion(buffer_array, 0, buf_size, (const jbyte *)buf);

    jclass class_obj = env->GetObjectClass(obj);
    jmethodID method = env->GetMethodID(class_obj, "decoderAudioFrame", "([BI)V");
    env->CallVoidMethod(obj, method, buffer_array, buf_size);

    env->DeleteLocalRef(class_obj);
    env->DeleteLocalRef(buffer_array);
}

void ffmpeg_decoder_video_display(AVFrame *frame) {
    uint8_t *y_ptr = frame->data[0];
    uint8_t *u_ptr = frame->data[1];
    uint8_t *v_ptr = frame->data[2];

    int y_size = frame->linesize[0];
    int u_size = frame->linesize[1];
    int v_size = frame->linesize[2];

    int width = frame->width;
    int height = frame->height;
    int fmt = frame->format;

    const uint8_t *data_ptr[] = { y_ptr, v_ptr, u_ptr };
    const int data_size[] = { y_size, v_size, u_size };

    ffmpeg_surface_yuv_display(width, height, fmt, data_ptr, data_size);
}

int ffmpeg_decoder_read(void *ptr, uint8_t *buf, int buf_size) {
    struct DecoderContext *decoder_context = (struct DecoderContext *)ptr;
    struct Buffers *buffers = &decoder_context->buffers;
    int total = 0;

    while (buf_size > 0 && !decoder_context->stop) {
        struct Buffer *buffer = ffmpeg_buffers_first(buffers);
        if (buffer != NULL) {
            int size = ffmpeg_buffer_read(buffer, buf + total, buf_size);

            total += size;
            buf_size -= size;

            if (buffer->data_size == 0) {
                ffmpeg_buffers_remove(buffers, buffer);
                ffmpeg_buffer_release(buffer);

                delete buffer;
                buffer = NULL;
            }
        }

        av_usleep(1 * 1000);
    }

    return total;
}

bool ffmpeg_decoder_init(struct DecoderContext *decoder_context) {
    struct DecoderAudioContext *audio_context = &decoder_context->audio_context;
    struct DecoderVideoContext *video_context = &decoder_context->video_context;

    bool success = false;

    decoder_context->stop = false;
    decoder_context->format = avformat_alloc_context();
    if (decoder_context->format != NULL) {
        decoder_context->format->pb = avio_alloc_context(NULL, 0, 0, decoder_context, ffmpeg_decoder_read, NULL, NULL);
        if (avformat_open_input(&decoder_context->format, NULL, NULL, NULL) >= 0) {
            if (avformat_find_stream_info(decoder_context->format, NULL) >= 0) {
                AVFormatContext *format = decoder_context->format;
                for (int i = 0; i < format->nb_streams; i++) {
                    AVStream *stream = format->streams[i];
                    AVCodecParameters *codecpar = stream->codecpar;

                    if (codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
                        success = ffmpeg_decoder_audio_init(audio_context, stream);
                        if (success)
                            decoder_context->audio_stream = i;
                        else
                            break;
                    }

                    if (codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
                        success = ffmpeg_decoder_video_init(video_context, stream);
                        if (success)
                            decoder_context->video_stream = i;
                        else
                            break;
                    }
                }
            }
        }
    }

    return success;
}

void ffmpeg_decoder_release(struct DecoderContext *decoder_context) {
    ffmpeg_decoder_audio_release(&decoder_context->audio_context);
    ffmpeg_decoder_video_release(&decoder_context->video_context);

    if (decoder_context->format != NULL) {
        if (decoder_context->format->pb != NULL)
            avio_context_free(&decoder_context->format->pb);

        avformat_free_context(decoder_context->format);
        decoder_context->format = NULL;
    }

    decoder_context->audio_stream = -1;
    decoder_context->video_stream = -1;
    decoder_context->audio_timer = 0.0;
    decoder_context->video_timer = 0.0;
    decoder_context->remaining = 0.0;

    ffmpeg_buffers_release(&decoder_context->buffers);
}

void ffmpeg_decoder_read_frame(struct DecoderContext *decoder_context) {
    struct DecoderAudioContext *audio_context = &decoder_context->audio_context;
    struct DecoderVideoContext *video_context = &decoder_context->video_context;

    if (audio_context->packets.total + video_context->packets.total < 50) {
        bool push = false;
        AVPacket *packet = av_packet_alloc();

        if (av_read_frame(decoder_context->format, packet) == 0) {
            if (decoder_context->audio_stream > -1 && packet->stream_index == decoder_context->audio_stream) {
                ffmpeg_packet_push(&audio_context->packets, packet);
                push = true;
            }

            if (decoder_context->video_stream > -1 && packet->stream_index == decoder_context->video_stream) {
                ffmpeg_packet_push(&video_context->packets, packet);
                push = true;
            }
        }

        if (!push)
            av_packet_free(&packet);
    }
}

#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT bool JNICALL FUN_FFMPEG_DECODER(ffmpegDecoderInit)(JNIEnv *env, jobject obj) {
    decoder_context.audio_stream = -1;
    decoder_context.video_stream = -1;

    bool success = ffmpeg_decoder_init(&decoder_context);

    if (success) {
        if (decoder_context.audio_stream > -1) {
            if (!ffmpeg_decoder_audio_create(env, obj, &decoder_context.audio_context)) {
                ffmpeg_decoder_audio_release(&decoder_context.audio_context);
                decoder_context.audio_stream = -1;
            }
        }

        if (decoder_context.video_stream > -1) {
            if (!ffmpeg_decoder_video_create(env, obj, &decoder_context.video_context)) {
                ffmpeg_decoder_video_release(&decoder_context.video_context);
                decoder_context.video_stream = -1;
            }
        }
    }

    return success;
}

JNIEXPORT void JNICALL FUN_FFMPEG_DECODER(ffmpegDecoderStop)(JNIEnv *env, jobject obj) {
    decoder_context.stop = true;
}

JNIEXPORT void JNICALL FUN_FFMPEG_DECODER(ffmpegDecoderRelease)(JNIEnv *env, jobject obj) {
    ffmpeg_decoder_release(&decoder_context);
}

JNIEXPORT void JNICALL FUN_FFMPEG_DECODER(ffmpegDecoderReadFrame)(JNIEnv *env, jobject obj) {
    ffmpeg_decoder_read_frame(&decoder_context);
}

JNIEXPORT void JNICALL FUN_FFMPEG_DECODER(ffmpegDecoderAudio)(JNIEnv *env, jobject obj) {
    ffmpeg_decoder_audio(env, obj, &decoder_context.audio_context, &decoder_context, ffmpeg_decoder_audio_frame);
}

JNIEXPORT void JNICALL FUN_FFMPEG_DECODER(ffmpegDecoderVideo)(JNIEnv *env, jobject obj) {
    ffmpeg_decoder_video(&decoder_context.video_context, decoder_context.format);
}

JNIEXPORT void JNICALL FUN_FFMPEG_DECODER(ffmpegDecoderVideoDisplay)(JNIEnv *env, jobject obj) {
    ffmpeg_decoder_video_display(env, obj, &decoder_context.video_context, &decoder_context, ffmpeg_decoder_video_display);
}

JNIEXPORT void JNICALL FUN_FFMPEG_DECODER(ffmpegDecoderBufferInit)(JNIEnv *env, jobject obj) {
    ffmpeg_buffers_init(&decoder_context.buffers);
}

JNIEXPORT void JNICALL FUN_FFMPEG_DECODER(ffmpegDecoderBufferPush)(JNIEnv *env, jobject obj, jbyteArray data_array, int data_size) {
    jbyte *data = data_array != NULL ? env->GetByteArrayElements(data_array, JNI_FALSE) : NULL;
    if (data != NULL) {
        struct Buffer *buffer = ffmpeg_buffer_new(data_size);

        ffmpeg_buffer_write(buffer, (const uint8_t *)data, data_size);
        ffmpeg_buffers_push(&decoder_context.buffers, buffer);

        env->ReleaseByteArrayElements(data_array, data, 0);
    }
}
#ifdef __cplusplus
}
#endif