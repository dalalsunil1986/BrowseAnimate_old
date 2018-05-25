//
// Created by hihua on 17/11/15.
//

#include "ffmpeg_encoder_audio.h"

int ffmpeg_encoder_audio_fifo_write(struct EncoderAudioContext *audio_context, const uint8_t *buf, int nb_samples) {
    int channels = audio_context->context->channels;
    enum AVSampleFormat sample_fmt = audio_context->context->sample_fmt;

    uint8_t *array[] = { NULL, NULL, NULL, NULL };
    av_samples_fill_arrays(array, NULL, buf, channels, nb_samples, sample_fmt, 1);

    return av_audio_fifo_write(audio_context->fifo, (void **)array, nb_samples);
}

bool ffmpeg_encoder_audio_fifo_read(struct EncoderAudioContext *audio_context) {
    AVFrame *frame = audio_context->frame;

    if (av_audio_fifo_size(audio_context->fifo) >= frame->nb_samples) {
        if (av_audio_fifo_read(audio_context->fifo, (void **)frame->data, frame->nb_samples) > 0) {
            return true;
        }
    }

    return false;
}

bool ffmpeg_encoder_audio_init(struct EncoderAudioContext *audio_context, AVFormatContext *format, const char *name) {
    AVCodec *codec = avcodec_find_encoder_by_name(name);
    if (codec != NULL) {
        AVStream *stream = avformat_new_stream(format, codec);
        if (stream != NULL) {
            audio_context->context = avcodec_alloc_context3(codec);
            if (audio_context->context != NULL) {
                audio_context->context->channels = audio_context->channels;
                audio_context->context->channel_layout = av_get_default_channel_layout(audio_context->context->channels);
                audio_context->context->sample_rate = audio_context->samplerate;

                if (codec->sample_fmts != NULL)
                    audio_context->context->sample_fmt = codec->sample_fmts[0];
                else
                    audio_context->context->sample_fmt = AV_SAMPLE_FMT_S16;

                audio_context->context->bit_rate = audio_context->bitrate;
                audio_context->context->time_base = (AVRational) { 1, audio_context->context->sample_rate };

                if (format->oformat->flags & AVFMT_GLOBALHEADER)
                    audio_context->context->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;

                if (avcodec_open2(audio_context->context, codec, NULL) == 0 && avcodec_parameters_from_context(stream->codecpar, audio_context->context) == 0) {
                    audio_context->fifo = av_audio_fifo_alloc(audio_context->context->sample_fmt, audio_context->context->channels, 1);
                    if (audio_context->fifo != NULL) {
                        audio_context->frame = av_frame_alloc();
                        audio_context->frame->nb_samples = audio_context->context->frame_size;
                        audio_context->frame->channels = audio_context->context->channels;
                        audio_context->frame->channel_layout = av_get_default_channel_layout(audio_context->frame->channels);
                        audio_context->frame->sample_rate = audio_context->context->sample_rate;
                        audio_context->frame->format = audio_context->context->sample_fmt;

                        if (av_frame_get_buffer(audio_context->frame, 0) == 0) {
                            return true;
                        }
                    }
                }
            }
        }
    }

    return false;
}

void ffmpeg_encoder_audio_release(struct EncoderAudioContext *audio_context) {
    if (audio_context->context != NULL) {
        avcodec_free_context(&audio_context->context);
    }

    if (audio_context->fifo != NULL) {
        av_audio_fifo_free(audio_context->fifo);
        audio_context->fifo = NULL;
    }

    if (audio_context->frame != NULL) {
        av_frame_free(&audio_context->frame);
    }

    audio_context->stream = -1;
    audio_context->pts = 0;

    struct AudioSwr *swr = &audio_context->swr;
    if (swr->context != NULL) {
        swr_free(&swr->context);
    }

    if (swr->buf != NULL) {
        av_free(swr->buf);
        swr->buf = NULL;
    }

    swr->buf_size = 0;
}

int ffmpeg_encoder_audio_sample_count(int len, int channels) {
    return len / channels / 2;
}

int ffmpeg_encoder_audio_swr(struct EncoderAudioContext *audio_context, const uint8_t *data, int data_len, int in_channels, int in_samplerate, enum AVSampleFormat in_sample_fmt) {
    struct AudioSwr *swr = &audio_context->swr;
    int nb_samples = 0;

    int in_sample_count = ffmpeg_encoder_audio_sample_count(data_len, in_channels);

    int out_channel = audio_context->context->channels;
    uint64_t out_channel_layout = audio_context->context->channel_layout > 0 ? audio_context->context->channel_layout : av_get_default_channel_layout(out_channel);
    enum AVSampleFormat out_sample_fmt = audio_context->context->sample_fmt;
    int out_sample_rate = audio_context->context->sample_rate;

    if (swr->context == NULL) {
        swr->context = swr_alloc_set_opts(NULL, out_channel_layout, out_sample_fmt, out_sample_rate, av_get_default_channel_layout(in_channels), in_sample_fmt, in_samplerate, 0, NULL);
        if (swr_init(swr->context) < 0)
            return nb_samples;
    }

    int out_sample_count = swr_get_out_samples(swr->context, in_sample_count);

    int buf_size = av_samples_get_buffer_size(NULL, out_channel, out_sample_count, out_sample_fmt, 1);
    if (buf_size > 0) {
        if (buf_size > swr->buf_size) {
            if (swr->buf != NULL) {
                av_free(swr->buf);
                swr->buf = NULL;
            }

            swr->buf_size = buf_size;
            swr->buf = (uint8_t *) av_malloc(swr->buf_size);
        }

        uint8_t *in_array[] = { NULL, NULL, NULL, NULL };
        av_samples_fill_arrays(in_array, NULL, data, in_channels, in_samplerate, in_sample_fmt, 1);

        uint8_t *out_array[] = { NULL, NULL, NULL, NULL };
        av_samples_fill_arrays(out_array, NULL, swr->buf, out_channel, out_sample_rate, out_sample_fmt, 1);

        nb_samples = swr_convert(swr->context, out_array, out_sample_count, (const uint8_t **)in_array, in_sample_count);
    }

    return nb_samples;
}

bool ffmpeg_encoder_audio(struct EncoderAudioContext *audio_context, AVPacket *packet) {
    AVFrame *frame = audio_context->frame;
    frame->pts = audio_context->pts;
    audio_context->pts += frame->nb_samples;

    if (avcodec_send_frame(audio_context->context, audio_context->frame) == 0 && avcodec_receive_packet(audio_context->context, packet) == 0)
        return true;
    else
        return false;
}

void ffmpeg_encoder_audio(JNIEnv *env, jobject obj, struct EncoderAudioContext *audio_context, struct EncoderContext *encoder_context, const uint8_t *data, int data_len, int channels, int samplerate, void (*encoder_frame)(JNIEnv *env, jobject obj, uint8_t *buf, size_t buf_size)) {
    int nb_samples = ffmpeg_encoder_audio_swr(audio_context, data, data_len, channels, samplerate, AV_SAMPLE_FMT_S16);
    if (nb_samples > 0) {
        struct AudioSwr *swr = &audio_context->swr;

        ffmpeg_encoder_audio_fifo_write(audio_context, swr->buf, nb_samples);
        while (ffmpeg_encoder_audio_fifo_read(audio_context)) {
            av_packet_unref(encoder_context->packet);

            if (ffmpeg_encoder_audio(audio_context, encoder_context->packet)) {
                AVCodecContext *context = audio_context->context;
                AVPacket *packet = encoder_context->packet;
                AVFormatContext *format = encoder_context->format;
                AVStream *stream = format->streams[audio_context->stream];

                packet->stream_index = audio_context->stream;
                av_packet_rescale_ts(packet, context->time_base, stream->time_base);

                packet->pts = (av_gettime() - encoder_context->timer) / 1000;
                packet->dts = packet->pts;

                ffmpeg_write_frame_buffer(env, obj, format, packet, encoder_frame);
            }
        }
    }
}