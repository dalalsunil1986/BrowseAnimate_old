//
// Created by hihua on 17/12/11.
//

#include "ffmpeg_packet.h"

void ffmpeg_packet_init(struct Packets *packets) {
    pthread_mutex_init(&packets->section, NULL);
    packets->list.clear();
    packets->total = 0;
    packets->inited = true;
}

void ffmpeg_packet_release(struct Packets *packets) {
    if (packets->inited)
        pthread_mutex_lock(&packets->section);

    std::list<AVPacket *> *list = &packets->list;

    while (!list->empty()) {
        AVPacket *packet = list->front();
        av_packet_free(&packet);

        list->pop_front();
    }

    packets->total = 0;

    if (packets->inited) {
        pthread_mutex_unlock(&packets->section);
        pthread_mutex_destroy(&packets->section);

        packets->inited = false;
    }
}

void ffmpeg_packet_push(struct Packets *packets, AVPacket *packet) {
    pthread_mutex_lock(&packets->section);

    packets->list.push_back(packet);
    packets->total++;

    pthread_mutex_unlock(&packets->section);
}

AVPacket *ffmpeg_packet_pop(struct Packets *packets) {
    AVPacket *packet = NULL;

    pthread_mutex_lock(&packets->section);

    if (!packets->list.empty()) {
        packet = packets->list.front();
        packets->list.pop_front();

        packets->total--;
    }

    pthread_mutex_unlock(&packets->section);

    return packet;
}