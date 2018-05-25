package com.hihua.browseanimate.ffmpeg;

import android.util.Log;

import com.hihua.browseanimate.model.ModelAudio;

/**
 * Created by hihua on 17/11/27.
 */

public abstract class FFmpegBase {
    static {
        System.loadLibrary("ffmpeg");
    }

    protected final ModelAudio mModelAudio;

    public FFmpegBase() {
        mModelAudio = new ModelAudio();
    }

    public interface HandleEncoder {
        public void encoderHeader(byte[] data, int dataSize);
        public void encoderFrame(byte[] data, int dataSize);
        public void encoderTail(byte[] data, int dataSize);
    }

    public interface HandleDecoder {
        public void decoderStart(boolean success);
    }
}
