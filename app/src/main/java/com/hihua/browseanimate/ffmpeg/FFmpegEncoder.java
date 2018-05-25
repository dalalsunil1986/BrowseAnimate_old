package com.hihua.browseanimate.ffmpeg;

/**
 * Created by hihua on 17/11/27.
 */

public class FFmpegEncoder extends FFmpegBase {

    private native int ffmpegEncoderInit(String config);
    private native void ffmpegEncoderRelease();
    private native int ffmpegEncoderWriteHeader();
    private native int ffmpegEncoderWriteTail();
    private native void ffmpegEncoderVideo(byte[] dataArray, int width, int height, int fmt);
    private native void ffmpegEncoderAudio(byte[] dataArray, int channels, int samplerate);

    private final Object mLock = new Object();
    private final HandleEncoder mHandle;

    public FFmpegEncoder(HandleEncoder handle) {
        mHandle = handle;
    }

    public int encoderInit(String config) {
        int ret = ffmpegEncoderInit(config);
        if (ret > -1) {
            if (ffmpegEncoderWriteHeader() > 0)
                return ret;
            else
                return -1;
        }

        return ret;
    }

    public void encoderRelease() {
        ffmpegEncoderRelease();
    }

    public void encoderVideo(byte[] dataArray, int width, int height, int fmt) {
        synchronized (mLock) {
            ffmpegEncoderVideo(dataArray, width, height, fmt);
        }
    }

    public void encoderAudio(byte[] dataArray, int channels, int samplerate) {
        synchronized (mLock) {
            ffmpegEncoderAudio(dataArray, channels, samplerate);
        }
    }

    public void encoderHeader(byte[] data, int dataSize) {
        mHandle.encoderHeader(data, dataSize);
    }

    public void encoderFrame(byte[] data, int dataSize) {
        mHandle.encoderFrame(data, dataSize);
    }

    public void encoderTail(byte[] data, int dataSize) {
        mHandle.encoderTail(data, dataSize);
    }
}
