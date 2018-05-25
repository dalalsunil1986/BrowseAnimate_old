//
// Created by hihua on 17/11/23.
//

#include "ffmpeg_buffer.h"

void ffmpeg_buffer_release(struct Buffer *buffer) {
    if (buffer->buf != NULL) {
        free(buffer->buf);
        buffer->buf = NULL;
    }

    buffer->buf_size = 0;
    buffer->data_size = 0;
    buffer->read_index = 0;
    buffer->write_index = 0;
}

void ffmpeg_buffers_init(struct Buffers *buffers) {
    pthread_mutex_init(&buffers->section, NULL);
    buffers->list.clear();
    buffers->total = 0;
    buffers->inited = true;
}

void ffmpeg_buffers_release(struct Buffers *buffers) {
    if (buffers->inited)
        pthread_mutex_lock(&buffers->section);

    std::list<struct Buffer *> *list = &buffers->list;

    while (!list->empty()) {
        struct Buffer *buffer = list->front();
        ffmpeg_buffer_release(buffer);
        free(buffer);

        list->pop_front();
    }

    buffers->total = 0;

    if (buffers->inited) {
        pthread_mutex_unlock(&buffers->section);
        pthread_mutex_destroy(&buffers->section);

        buffers->inited = false;
    }
}

struct Buffer *ffmpeg_buffer_new(int buf_size) {
    struct Buffer *buffer = (struct Buffer *)malloc(sizeof(struct Buffer));
    buffer->buf_size = buf_size;
    buffer->data_size = 0;
    buffer->read_index = 0;
    buffer->write_index = 0;
    buffer->buf = (uint8_t *)malloc(buffer->buf_size);

    return buffer;
}

void ffmpeg_buffer_init(struct Buffer *buffer, int buf_size) {
    buffer->buf_size = buf_size;
    buffer->data_size = 0;
    buffer->read_index = 0;
    buffer->write_index = 0;
    buffer->buf = (uint8_t *)malloc(buffer->buf_size);
}

int ffmpeg_buffer_write(struct Buffer *buffer, const void *data, int data_size) {
    int size = data_size;
    if (size > buffer->buf_size - buffer->write_index)
        size = buffer->buf_size - buffer->write_index;

    if (size > buffer->buf_size - buffer->data_size)
        size = buffer->buf_size - buffer->data_size;

    if (size > 0) {
        memcpy(buffer->buf + buffer->write_index, data, size);

        buffer->write_index += size;
        buffer->write_index %= buffer->buf_size;
        buffer->data_size += size;
    }

    return size;
}

int ffmpeg_buffer_read(struct Buffer *buffer, void *data, int data_size) {
    int size = data_size;
    if (size > buffer->buf_size - buffer->read_index)
        size = buffer->buf_size - buffer->read_index;

    if (size > buffer->data_size)
        size = buffer->data_size;

    if (size > 0) {
        memcpy(data, buffer->buf + buffer->read_index, size);

        buffer->read_index += size;
        buffer->read_index %= buffer->buf_size;
        buffer->data_size -= size;
    }

    return size;
}

struct Buffer *ffmpeg_buffers_first(struct Buffers *buffers) {
    struct Buffer *buffer = NULL;

    pthread_mutex_lock(&buffers->section);

    std::list<struct Buffer *> *list = &buffers->list;

    if (!list->empty())
        buffer = list->front();

    pthread_mutex_unlock(&buffers->section);

    return buffer;
}

void ffmpeg_buffers_remove(struct Buffers *buffers, struct Buffer *buffer) {
    pthread_mutex_lock(&buffers->section);

    std::list<struct Buffer *> *list = &buffers->list;
    std::list<struct Buffer *>::iterator it;

    if (!list->empty()) {
        for (it = list->begin();it != list->end();it++) {
            if (buffer == *it) {
                list->erase(it);
                buffers->total--;
                break;
            }
        }
    }

    pthread_mutex_unlock(&buffers->section);
}

void ffmpeg_buffers_push(struct Buffers *buffers, struct Buffer *buffer) {
    pthread_mutex_lock(&buffers->section);

    std::list<struct Buffer *> *list = &buffers->list;

    list->push_back(buffer);
    buffers->total++;

    pthread_mutex_unlock(&buffers->section);
}