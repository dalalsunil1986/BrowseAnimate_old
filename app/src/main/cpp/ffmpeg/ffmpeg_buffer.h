//
// Created by hihua on 17/11/23.
//

#ifndef BROWSEANIMATE_FFMPEG_BUFFER_H
#define BROWSEANIMATE_FFMPEG_BUFFER_H

#include "ffmpeg.h"

struct Buffer {
    int buf_size;
    int data_size;
    int read_index;
    int write_index;
    uint8_t *buf;
};

struct Buffers {
    pthread_mutex_t section;
    std::list<struct Buffer*> list;
    size_t total;
    bool inited;
};

void ffmpeg_buffers_init(struct Buffers *buffers);
void ffmpeg_buffers_release(struct Buffers *buffers);
struct Buffer *ffmpeg_buffers_first(struct Buffers *buffers);
void ffmpeg_buffers_push(struct Buffers *buffers, struct Buffer *buffer);
void ffmpeg_buffers_remove(struct Buffers *buffers, struct Buffer *buffer);

struct Buffer *ffmpeg_buffer_new(int buf_size);
void ffmpeg_buffer_init(struct Buffer *buffer, int buf_size);
int ffmpeg_buffer_write(struct Buffer *buffer, const void *data, int data_size);
int ffmpeg_buffer_read(struct Buffer *buffer, void *data, int data_size);
void ffmpeg_buffer_release(struct Buffer *buffer);

#endif //BROWSEANIMATE_FFMPEG_BUFFER_H
