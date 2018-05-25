//
// Created by hihua on 17/11/24.
//

#include "ffmpeg_decoder_video.h"

void ffmpeg_decoder_video_frame_init(struct Frames *frames) {
    pthread_mutex_init(&frames->section, NULL);
    frames->write_index = 0;
    frames->read_index = 0;
    frames->frame_total = 0;
    frames->inited = true;

    for (size_t i = 0;i < FRAME_MAX_VIDEO;i++) {
        struct Frame *frame = &frames->frames[i];
        frame->frame = av_frame_alloc();
        frame->pts = 0.0;
        frame->duration = 0.0;
    }
}

void ffmpeg_decoder_video_frame_release(struct Frames *frames) {
    if (frames->inited)
        pthread_mutex_lock(&frames->section);

    for (size_t i = 0;i < FRAME_MAX_VIDEO;i++) {
        struct Frame *frame = &frames->frames[i];
        av_frame_free(&frame->frame);
        frame->pts = 0.0;
        frame->duration = 0.0;
    }

    frames->read_index = 0;
    frames->write_index = 0;
    frames->frame_total = 0;

    if (frames->inited) {
        pthread_mutex_unlock(&frames->section);
        pthread_mutex_destroy(&frames->section);

        frames->inited = false;
    }
}

bool ffmpeg_decoder_video_init(struct DecoderVideoContext *video_context, AVStream *stream) {
    AVCodecParameters *codecpar = stream->codecpar;

    AVCodec *codec = avcodec_find_decoder(codecpar->codec_id);
    if (codec != NULL) {
        video_context->context = avcodec_alloc_context3(codec);
        if (video_context->context != NULL) {
            if (avcodec_parameters_to_context(video_context->context, codecpar) >= 0) {
                if (avcodec_open2(video_context->context, codec, NULL) == 0) {
                    ffmpeg_decoder_video_frame_init(&video_context->frames);
                    ffmpeg_packet_init(&video_context->packets);
                    video_context->stream = stream;
                    return true;
                }
            }
        }
    }

    return false;
}

void ffmpeg_decoder_video_release(struct DecoderVideoContext *video_context) {
    if (video_context->context != NULL) {
        avcodec_free_context(&video_context->context);
    }

    video_context->stream = NULL;

    ffmpeg_packet_release(&video_context->packets);
    ffmpeg_decoder_video_frame_release(&video_context->frames);
}

void ffmpeg_decoder_video_read_next(struct Frames *frames) {
    frames->read_index++;
    frames->read_index %= FRAME_MAX_VIDEO;
    if (frames->frame_total > 0)
        frames->frame_total--;
}

void ffmpeg_decoder_video_write_next(struct Frames *frames) {
    frames->write_index++;
    frames->write_index %= FRAME_MAX_VIDEO;
    frames->frame_total++;
}

void ffmpeg_decoder_video(struct DecoderVideoContext *video_context, AVFormatContext *format) {
    struct Frames *frames = &video_context->frames;
    struct Frame *frame = &frames->frames[frames->write_index];
    AVStream *stream = video_context->stream;

    pthread_mutex_lock(&frames->section);

    if (frames->frame_total < FRAME_MAX_VIDEO) {
        AVPacket *packet = ffmpeg_packet_pop(&video_context->packets);
        if (packet != NULL) {
            if (avcodec_send_packet(video_context->context, packet) == 0) {
                av_frame_unref(frame->frame);

                if (avcodec_receive_frame(video_context->context, frame->frame) == 0) {
                    int64_t timestamp = frame->frame->best_effort_timestamp;
                    int64_t duration = frame->frame->pkt_duration;

                    if (duration > 0)
                        frame->duration = duration * av_q2d(stream->time_base);
                    else
                        frame->duration = 0;

                    if (timestamp > 0)
                        frame->pts = timestamp * av_q2d(stream->time_base);
                    else {
                        AVRational rational = av_guess_frame_rate(format, stream, frame->frame);
                        if (frame->duration == 0 && rational.den > 0)
                            frame->duration = 1 / av_q2d(rational);

                        frame->pts = (frame->frame->coded_picture_number + 1) * (frame->duration > 0 ? frame->duration : 0.04);
                    }

                    ffmpeg_decoder_video_write_next(frames);
                }
            }

            av_packet_free(&packet);
        }
    }

    pthread_mutex_unlock(&frames->section);
}

double ffmpeg_decoder_video_duration(struct Frames *frames) {
    struct Frame *frame_current = &frames->frames[frames->read_index];
    struct Frame *frame_last = frames->read_index > 0 ? &frames->frames[frames->read_index - 1] : &frames->frames[FRAME_MAX_VIDEO - 1];

    if (frame_current->pts > frame_last->pts && frame_last->pts > 0)
        return frame_current->pts - frame_last->pts;
    else
        return frame_current->duration;
}

double ffmpeg_decoder_video_delay(struct DecoderContext *decoder_context, double duration) {
    double delay = duration;

    if (decoder_context->audio_stream > -1) {
        double diff = decoder_context->video_timer - decoder_context->audio_timer;

        if (diff > 0) { //v fast
            if (diff > 1.0)
                return 1.0;

            if (diff > duration * 5)
                return duration * 3;

            if (diff > duration * 3)
                return duration * 2;

            if (diff > duration * 2)
                return duration + duration / 2;

            if (diff > duration)
                return duration + duration / 4;
        } else {  // v slow
            if (-diff > 1.0)
                return -1.0;

            if (-diff > duration * 5)
                return 0;

            if (-diff > duration * 2)
                return duration - duration / 2;

            if (-diff > duration)
                return duration - duration / 4;
        }
    }

    return delay;
}

void ffmpeg_decoder_video_display(JNIEnv *env, jobject obj, struct DecoderVideoContext *video_context, struct DecoderContext *decoder_context, void (*display_frame)(AVFrame *frame)) {
    struct Frames *frames = &video_context->frames;
    struct Frame *frame = &frames->frames[frames->read_index];

    //LOG_FFMPEG_I("display %d,%f,%f,%f", frames->frame_total, decoder_context->remaining, decoder_context->video_timer, decoder_context->audio_timer);

    if (decoder_context->remaining > 0.0)
        av_usleep(decoder_context->remaining * 1000000.0);

    decoder_context->remaining = 0.01;

    pthread_mutex_lock(&frames->section);

    if (frames->frame_total > 0) {
        decoder_context->video_timer = frame->pts;

        double duration = ffmpeg_decoder_video_duration(frames);
        double delay = ffmpeg_decoder_video_delay(decoder_context, duration);

        if (delay >= 0 && display_frame != NULL)
            display_frame(frame->frame);

        if (delay < 1.0)
            ffmpeg_decoder_video_read_next(frames);

        decoder_context->remaining = delay > 0 ? delay >= 1.0 ? duration : delay : 0;
    }

    pthread_mutex_unlock(&frames->section);
}