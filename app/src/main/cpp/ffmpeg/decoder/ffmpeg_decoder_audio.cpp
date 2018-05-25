//
// Created by hihua on 17/11/24.
//

#include "ffmpeg_decoder_audio.h"

bool ffmpeg_decoder_audio_init(struct DecoderAudioContext *audio_context, AVStream *stream) {
    AVCodecParameters *codecpar = stream->codecpar;

    AVCodec *codec = avcodec_find_decoder(codecpar->codec_id);
    if (codec != NULL) {
        audio_context->context = avcodec_alloc_context3(codec);
        if (audio_context->context != NULL) {
            if (avcodec_parameters_to_context(audio_context->context, codecpar) >= 0) {
                if (avcodec_open2(audio_context->context, codec, NULL) == 0) {
                    audio_context->frame = av_frame_alloc();
                    if (audio_context->frame != NULL) {
                        ffmpeg_packet_init(&audio_context->packets);
                        audio_context->stream = stream;
                        return true;
                    }
                }
            }
        }
    }

    return false;
}

void ffmpeg_decoder_audio_release(struct DecoderAudioContext *audio_context) {
    if (audio_context->context != NULL) {
        avcodec_free_context(&audio_context->context);
    }

    audio_context->stream = NULL;

    if (audio_context->frame != NULL) {
        av_frame_free(&audio_context->frame);
    }

    struct AudioSwr *swr = &audio_context->swr;
    if (swr->context != NULL) {
        swr_free(&swr->context);
    }

    if (swr->buf != NULL) {
        av_free(swr->buf);
        swr->buf = NULL;
    }

    swr->buf_size = 0;

    ffmpeg_packet_release(&audio_context->packets);
}

int ffmpeg_decoder_audio_swr(struct DecoderAudioContext *audio_context) {
    struct AudioSwr *swr = &audio_context->swr;
    AVFrame *frame = audio_context->frame;
    int nb_samples = frame->nb_samples;

    int in_channel = frame->channels;
    int64_t in_channel_layout = av_get_default_channel_layout(in_channel);
    enum AVSampleFormat in_sample_fmt = (enum AVSampleFormat)frame->format;
    int in_sample_rate = frame->sample_rate;

    int out_channel = frame->channels;
    int64_t out_channel_layout = av_get_default_channel_layout(out_channel);
    enum AVSampleFormat out_sample_fmt = AV_SAMPLE_FMT_S16;
    int out_sample_rate = frame->sample_rate;

    int buf_size = av_samples_get_buffer_size(NULL, out_channel, nb_samples, out_sample_fmt, 1);
    if (buf_size > swr->buf_size) {
        if (swr->buf != NULL) {
            av_free(swr->buf);
            swr->buf = NULL;
        }

        swr->buf_size = buf_size;
        swr->buf = (uint8_t *) av_malloc(swr->buf_size);
    }

    uint8_t *out_array[] = { NULL, NULL, NULL, NULL };
    av_samples_fill_arrays(out_array, NULL, swr->buf, out_channel, out_sample_rate, out_sample_fmt, 1);

    if (swr->context == NULL) {
        swr->context = swr_alloc_set_opts(NULL, out_channel_layout, out_sample_fmt, out_sample_rate, in_channel_layout, in_sample_fmt, in_sample_rate, 0, NULL);
        if (swr->context != NULL && swr_init(swr->context) >= 0)
            nb_samples = swr_convert(swr->context, out_array, nb_samples, (const uint8_t **)frame->data, nb_samples);
        else
            nb_samples = 0;
    } else
        nb_samples = swr_convert(swr->context, out_array, nb_samples, (const uint8_t **)frame->data, nb_samples);

    return nb_samples > 0 ? buf_size : 0;
}

void ffmpeg_decoder_audio(JNIEnv *env, jobject obj, struct DecoderAudioContext *audio_context, struct DecoderContext *decoder_context, void (*decoder_frame)(JNIEnv *env, jobject obj, uint8_t *buf, size_t buf_size)) {
    AVPacket *packet = ffmpeg_packet_pop(&audio_context->packets);
    if (packet != NULL) {
        if (avcodec_send_packet(audio_context->context, packet) == 0) {
            av_frame_unref(audio_context->frame);

            AVStream *stream = audio_context->stream;
            double audio_timer = av_q2d(stream->time_base) * packet->pts;

            if (avcodec_receive_frame(audio_context->context, audio_context->frame) == 0) {
                int buf_size = ffmpeg_decoder_audio_swr(audio_context);
                if (buf_size > 0) {
                    uint8_t *ptr = audio_context->swr.buf;
                    int samplerate = audio_context->context->sample_rate;
                    int channels = audio_context->context->channels;

                    if (decoder_frame != NULL)
                        decoder_frame(env, obj, ptr, buf_size);

                    audio_timer += (double) buf_size / ((double) samplerate * (double) channels * 2.0);
                    decoder_context->audio_timer = audio_timer;
                }
            }
        }

        av_packet_free(&packet);
    }
}