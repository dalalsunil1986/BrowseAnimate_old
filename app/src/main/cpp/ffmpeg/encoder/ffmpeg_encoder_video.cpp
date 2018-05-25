//
// Created by hihua on 17/11/15.
//

#include "ffmpeg_encoder_video.h"

bool ffmpeg_encoder_video_init(struct EncoderVideoContext *video_context, AVFormatContext *format, const char *name, const AVDictionary *priv) {
    AVCodec *codec = avcodec_find_encoder_by_name(name);
    if (codec != NULL) {
        AVStream *stream = avformat_new_stream(format, codec);
        if (stream != NULL) {
            video_context->context = avcodec_alloc_context3(codec);
            if (video_context->context != NULL) {
                video_context->context->width = video_context->width;
                video_context->context->height = video_context->height;

                if (codec->pix_fmts != NULL)
                    video_context->context->pix_fmt = codec->pix_fmts[0];
                else
                    video_context->context->pix_fmt = AV_PIX_FMT_YUV420P;

                if (video_context->bitrate > 0)
                    video_context->context->bit_rate = video_context->bitrate;

                video_context->context->time_base = (AVRational) { 1, video_context->framerate };

                if (priv != NULL) {
                    AVDictionaryEntry *entry = NULL;
                    while (true) {
                        entry = av_dict_get(priv, "", entry, AV_DICT_IGNORE_SUFFIX);
                        if (entry != NULL) {
                            if (entry->key != NULL && entry->value != NULL)
                                av_opt_set(video_context->context->priv_data, entry->key, entry->value, 0);
                        } else
                            break;
                    }
                }

                if (format->oformat->flags & AVFMT_GLOBALHEADER)
                    video_context->context->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;

                if (avcodec_open2(video_context->context, codec, NULL) == 0 && avcodec_parameters_from_context(stream->codecpar, video_context->context) == 0) {
                    video_context->frame = av_frame_alloc();
                    video_context->frame->width = video_context->context->width;
                    video_context->frame->height = video_context->context->height;
                    video_context->frame->format = video_context->context->pix_fmt;

                    if (av_frame_get_buffer(video_context->frame, 0) == 0) {
                        return true;
                    }
                }
            }
        }
    }

    return false;
}

void ffmpeg_encoder_video_release(struct EncoderVideoContext *video_context) {
    if (video_context->context != NULL) {
        avcodec_free_context(&video_context->context);
    }

    if (video_context->sws != NULL) {
        sws_freeContext(video_context->sws);
        video_context->sws = NULL;
    }

    if (video_context->frame != NULL)
        av_frame_free(&video_context->frame);

    video_context->stream = -1;
    video_context->pts = 0;
}

bool ffmpeg_encoder_video_sws(struct EncoderVideoContext *video_context, const uint8_t *data, int data_len, int width, int height, enum AVPixelFormat fmt) {
    AVFrame *frame = video_context->frame;

    video_context->sws = sws_getCachedContext(video_context->sws, width, height, fmt, frame->width, frame->height, (enum AVPixelFormat)frame->format, SWS_BICUBIC, NULL, NULL, NULL);
    if (video_context->sws != NULL) {
        uint8_t *array[] = { NULL, NULL, NULL, NULL };
        int array_linesize[] = { 0, 0, 0, 0 };
        av_image_fill_arrays(array, array_linesize, data, fmt, width, height, 1);

        int sws_size = sws_scale(video_context->sws, (const uint8_t *const *)array, array_linesize, 0, height, frame->data, frame->linesize);
        if (sws_size > 0) {
            return true;
        }
    }

    return false;
}

bool ffmpeg_encoder_video(struct EncoderVideoContext *video_context, AVPacket *packet) {
    AVFrame *frame = video_context->frame;
    frame->pict_type = AV_PICTURE_TYPE_NONE;
    frame->pts = ++video_context->pts;

    if (avcodec_send_frame(video_context->context, frame) == 0 && avcodec_receive_packet(video_context->context, packet) == 0)
        return true;
    else
        return false;
}

void ffmpeg_encoder_video(JNIEnv *env, jobject obj, struct EncoderVideoContext *video_context, struct EncoderContext *encoder_context, const uint8_t *data, int data_len, int width, int height, enum AVPixelFormat fmt, void (*encoder_frame)(JNIEnv *env, jobject obj, uint8_t *buf, size_t buf_size)) {
    if (ffmpeg_encoder_video_sws(video_context, data, data_len, width, height, fmt)) {
        av_packet_unref(encoder_context->packet);

        if (ffmpeg_encoder_video(video_context, encoder_context->packet)) {
            AVCodecContext *context = video_context->context;
            AVPacket *packet = encoder_context->packet;
            AVFormatContext *format = encoder_context->format;
            AVStream *stream = format->streams[video_context->stream];

            packet->stream_index = video_context->stream;
            av_packet_rescale_ts(packet, context->time_base, stream->time_base);

            packet->pts = (av_gettime() - encoder_context->timer) / 1000;
            packet->dts = packet->pts;

            ffmpeg_write_frame_buffer(env, obj, format, packet, encoder_frame);
        }
    }
}