//
// Created by hihua on 17/11/15.
//

#include <json/json.h>
#include "ffmpeg_encoder.h"
#include "ffmpeg_encoder_video.h"
#include "ffmpeg_encoder_audio.h"

struct EncoderContext encoder_context;

void ffmpeg_encoder_header(JNIEnv *env, jobject obj, uint8_t *buf, size_t buf_size) {
    jbyteArray buffer_array = env->NewByteArray(buf_size);
    env->SetByteArrayRegion(buffer_array, 0, buf_size, (jbyte *)buf);

    jclass class_obj = env->GetObjectClass(obj);
    jmethodID method = env->GetMethodID(class_obj, "encoderHeader", "([BI)V");
    env->CallVoidMethod(obj, method, buffer_array, buf_size);

    env->DeleteLocalRef(class_obj);
    env->DeleteLocalRef(buffer_array);
}

void ffmpeg_encoder_frame(JNIEnv *env, jobject obj, uint8_t *buf, size_t buf_size) {
    jbyteArray buffer_array = env->NewByteArray(buf_size);
    env->SetByteArrayRegion(buffer_array, 0, buf_size, (jbyte *)buf);

    jclass class_obj = env->GetObjectClass(obj);
    jmethodID method = env->GetMethodID(class_obj, "encoderFrame", "([BI)V");
    env->CallVoidMethod(obj, method, buffer_array, buf_size);

    env->DeleteLocalRef(class_obj);
    env->DeleteLocalRef(buffer_array);
}

void ffmpeg_encoder_tail(JNIEnv *env, jobject obj, uint8_t *buf, size_t buf_size) {
    jbyteArray buffer_array = env->NewByteArray(buf_size);
    env->SetByteArrayRegion(buffer_array, 0, buf_size, (jbyte *)buf);

    jclass class_obj = env->GetObjectClass(obj);
    jmethodID method = env->GetMethodID(class_obj, "encoderTail", "([BI)V");
    env->CallVoidMethod(obj, method, buffer_array, buf_size);

    env->DeleteLocalRef(class_obj);
    env->DeleteLocalRef(buffer_array);
}

//{"format":"flv","video":{"name":"h264","bitrate":0,"width":720,"height":480,"framerate":20,"priv":{"tune":"zerolatency","preset":"veryfast"}},"audio":{"name":"libfdk_aac","bitrate":64,"channels":2,"samplerate":44100}}
int ffmpeg_encoder_init(struct EncoderContext *encoder_context, const char *config) {
    int ret = -1;

    if (config != NULL) {
        Json::CharReaderBuilder builder;
        Json::CharReader *reader(builder.newCharReader());
        JSONCPP_STRING errs;
        Json::Value root;

        if (reader->parse(config, config + strlen(config), &root, &errs)) {
            if (root.isMember("format")) {
                std::string format = root["format"].asString();
                if (!format.empty()) {
                    if (avformat_alloc_output_context2(&encoder_context->format, NULL, format.c_str(), NULL) >= 0) {
                        encoder_context->packet = av_packet_alloc();
                        if (encoder_context->packet != NULL) {
                            bool has_audio = false, has_video = false;
                            int stream = 0;

                            if (root.isMember("audio")) {
                                Json::Value audio = root["audio"];

                                std::string name = audio["name"].asString();
                                int bitrate = audio["bitrate"].asInt();
                                int channels = audio["channels"].asInt();
                                int samplerate = audio["samplerate"].asInt();

                                struct EncoderAudioContext *audio_context = &encoder_context->audio_context;
                                audio_context->bitrate = bitrate;
                                audio_context->channels = channels;
                                audio_context->samplerate = samplerate;

                                if (ffmpeg_encoder_audio_init(&encoder_context->audio_context, encoder_context->format, name.c_str())) {
                                    audio_context->stream = stream++;
                                    has_audio = true;
                                }
                            }

                            if (root.isMember("video")) {
                                Json::Value video = root["video"];

                                std::string name = video["name"].asString();
                                int bitrate = video["bitrate"].asInt();
                                int width = video["width"].asInt();
                                int height = video["height"].asInt();
                                int framerate = video["framerate"].asInt();
                                Json::Value priv = video["priv"];

                                AVDictionary *video_dict = NULL;

                                Json::Value::Members privs = priv.getMemberNames();
                                if (privs.size() > 0) {
                                    for (Json::Value::Members::iterator it = privs.begin(); it != privs.end(); it++) {
                                        std::string key = *it;
                                        if (!key.empty() && priv[key].type() == Json::stringValue) {
                                            std::string value = priv[key].asString();

                                            av_dict_set(&video_dict, key.c_str(), value.c_str(), 0);
                                        }
                                    }
                                }

                                struct EncoderVideoContext *video_context = &encoder_context->video_context;
                                video_context->bitrate = bitrate;
                                video_context->width = width;
                                video_context->height = height;
                                video_context->framerate = framerate;

                                if (ffmpeg_encoder_video_init(&encoder_context->video_context, encoder_context->format, name.c_str(), video_dict)) {
                                    video_context->stream = stream++;
                                    has_video = true;
                                }

                                if (video_dict != NULL)
                                    av_dict_free(&video_dict);
                            }

                            if (stream > 0) {
                                encoder_context->timer = av_gettime();

                                ret = 0;

                                if (has_audio)
                                    ret |= 1;

                                if (has_video)
                                    ret |= 2;
                            }
                        }
                    }
                }
            }
        }

        delete reader;
    }

    return ret;
}

void ffmpeg_encoder_release(struct EncoderContext *encoder_context) {
    ffmpeg_encoder_audio_release(&encoder_context->audio_context);
    ffmpeg_encoder_video_release(&encoder_context->video_context);

    if (encoder_context->packet != NULL)
        av_packet_free(&encoder_context->packet);

    if (encoder_context->format != NULL) {
        if (encoder_context->format->pb != NULL)
            avio_closep(&encoder_context->format->pb);

        avformat_free_context(encoder_context->format);
        encoder_context->format = NULL;
    }

    encoder_context->timer = 0;

    LOG_FFMPEG_I("ffmpeg_encoder_release finish");
}

#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT int JNICALL FUN_FFMPEG_ENCODER(ffmpegEncoderInit)(JNIEnv *env, jobject obj, jstring jConfig) {
    const char *config = jConfig != NULL ? env->GetStringUTFChars(jConfig, 0) : NULL;

    if (config != NULL) {
        int ret = ffmpeg_encoder_init(&encoder_context, config);

        env->ReleaseStringUTFChars(jConfig, config);

        return ret;
    }

    return -1;
}

JNIEXPORT void JNICALL FUN_FFMPEG_ENCODER(ffmpegEncoderRelease)(JNIEnv *env, jobject obj) {
    ffmpeg_encoder_release(&encoder_context);
}

JNIEXPORT int JNICALL FUN_FFMPEG_ENCODER(ffmpegEncoderWriteHeader)(JNIEnv *env, jobject obj) {
    if (avio_open_dyn_buf(&encoder_context.format->pb) == 0) {
        int ret = avformat_write_header(encoder_context.format, NULL);

        uint8_t *buffer = NULL;
        int buffer_size = avio_close_dyn_buf(encoder_context.format->pb, &buffer);

        if (ret == 0 && buffer_size > 0)
            ffmpeg_encoder_header(env, obj, buffer, buffer_size);

        if (buffer != NULL) {
            av_free(buffer);
            buffer = NULL;
        }

        encoder_context.format->pb = NULL;

        return buffer_size;
    }

    return 0;
}

JNIEXPORT int JNICALL FUN_FFMPEG_ENCODER(ffmpegEncoderWriteTail)(JNIEnv *env, jobject obj) {
    if (avio_open_dyn_buf(&encoder_context.format->pb) == 0) {
        int ret = av_write_trailer(encoder_context.format);

        uint8_t *buffer = NULL;
        int buffer_size = avio_close_dyn_buf(encoder_context.format->pb, &buffer);

        if (ret == 0 && buffer_size > 0)
            ffmpeg_encoder_tail(env, obj, buffer, buffer_size);

        if (buffer != NULL) {
            av_free(buffer);
            buffer = NULL;
        }

        return buffer_size;
    }

    return 0;
}

JNIEXPORT void JNICALL FUN_FFMPEG_ENCODER(ffmpegEncoderVideo)(JNIEnv *env, jobject obj, jbyteArray data_array, int width, int height, int fmt) {
    jbyte *data = data_array != NULL ? env->GetByteArrayElements(data_array, JNI_FALSE) : NULL;
    if (data != NULL) {
        int data_len = env->GetArrayLength(data_array);

        ffmpeg_encoder_video(env, obj, &encoder_context.video_context, &encoder_context, (const uint8_t *) data, data_len, width, height, (enum AVPixelFormat) fmt, ffmpeg_encoder_frame);

        env->ReleaseByteArrayElements(data_array, data, 0);
    }
}

JNIEXPORT void JNICALL FUN_FFMPEG_ENCODER(ffmpegEncoderAudio)(JNIEnv *env, jobject obj, jbyteArray data_array, int channels, int samplerate) {
    jbyte *data = data_array != NULL ? env->GetByteArrayElements(data_array, JNI_FALSE) : NULL;
    if (data != NULL) {
        int data_len = env->GetArrayLength(data_array);

        ffmpeg_encoder_audio(env, obj, &encoder_context.audio_context, &encoder_context, (const uint8_t *)data, data_len, channels, samplerate, ffmpeg_encoder_frame);

        env->ReleaseByteArrayElements(data_array, data, 0);
    }
}
#ifdef __cplusplus
}
#endif