//
// Created by hihua on 17/12/11.
//

#ifndef BROWSEANIMATE_FFMPEG_PACKET_H
#define BROWSEANIMATE_FFMPEG_PACKET_H

#include "ffmpeg.h"

struct Packets {
    pthread_mutex_t section;
    std::list<AVPacket *> list;
    size_t total;
    bool inited;
};

void ffmpeg_packet_init(struct Packets *packets);
void ffmpeg_packet_release(struct Packets *packets);
void ffmpeg_packet_push(struct Packets *packets, AVPacket *packet);
AVPacket *ffmpeg_packet_pop(struct Packets *packets);

#endif //BROWSEANIMATE_FFMPEG_PACKET_H
