//
// Created by hihua on 17/10/27.
//

#include "ffmpeg.h"

#ifdef __cplusplus
extern "C" {
#endif

void ffmpeg_init() {

}

void ffmpeg_free() {

}

int ffmpeg_write_frame_buffer(JNIEnv *env, jobject obj, AVFormatContext *format, AVPacket *packet, void (*encoder_frame)(JNIEnv *env, jobject obj, uint8_t *buf, size_t buf_size)) {
    if (avio_open_dyn_buf(&format->pb) == 0) {
        int ret = av_write_frame(format, packet);

        uint8_t *buffer = NULL;
        int buffer_size = avio_close_dyn_buf(format->pb, &buffer);

        if (ret == 0 && buffer_size > 0 && encoder_frame != NULL)
            encoder_frame(env, obj, buffer, buffer_size);

        if (buffer != NULL) {
            av_free(buffer);
            buffer = NULL;
        }

        format->pb = NULL;

        return buffer_size;
    }

    return 0;
}

#ifdef __cplusplus
}
#endif