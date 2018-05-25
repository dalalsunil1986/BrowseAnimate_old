package com.hihua.browseanimate.ffmpeg;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Surface;

import com.hihua.browseanimate.model.ModelAudio;
import com.hihua.browseanimate.model.ModelCamera;
import com.hihua.browseanimate.socket.SocketClient;
import com.hihua.browseanimate.util.UtilLog;

import java.lang.ref.WeakReference;

/**
 * Created by hihua on 17/11/27.
 */

public class FFmpegDecoder extends FFmpegBase {

    private native boolean ffmpegDecoderInit();
    private native void ffmpegDecoderStop();
    private native void ffmpegDecoderRelease();
    private native void ffmpegDecoderReadFrame();
    private native void ffmpegDecoderAudio();
    private native void ffmpegDecoderVideo();
    private native void ffmpegDecoderVideoDisplay();
    private native void ffmpegDecoderBufferInit();
    private native void ffmpegDecoderBufferPush(byte[] dataArray, int dataSize);

    private Thread mThreadReadFrame = null;
    private Thread mThreadDecoderAudio = null;
    private Thread mThreadDecoderVideo = null;
    private Thread mThreadVideoDisplay = null;

    private final NotifyDecoder mNotify;
    private final ModelAudio.AudioPlayer mAudioPlayer;

    public FFmpegDecoder(HandleDecoder handle) {
        mNotify = new NotifyDecoder(handle);
        mAudioPlayer = new ModelAudio.AudioPlayer(mHandleAudioPlayer);
    }

    public void decoderStart() {
        ffmpegDecoderBufferInit();

        mThreadReadFrame = new Thread(new Runnable() {
            @Override
            public void run() {
                UtilLog.writeDebug(getClass(), "ffmpegDecoderBufferInit");

                if (ffmpegDecoderInit()) {
                    mNotify.decoderStart(true);

                    while (!mThreadReadFrame.isInterrupted()) {
                        ffmpegDecoderReadFrame();

                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                } else
                    mNotify.decoderStart(false);
            }
        });

        mThreadReadFrame.start();
    }

    public boolean decoderAudioCreate(int samplerate, int channels) {
        mAudioPlayer.startPlayer(samplerate, channels);

        mThreadDecoderAudio = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!mThreadDecoderAudio.isInterrupted()) {
                    ffmpegDecoderAudio();

                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });

        mThreadDecoderAudio.start();

        return true;
    }

    public boolean decoderVideoCreate(int width, int height, int fmt) {
        UtilLog.writeDebug(getClass(), "decoderVideoCreate " + width + "x" + height + " " + fmt + " format");

        mThreadDecoderVideo = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!mThreadDecoderVideo.isInterrupted()) {
                    ffmpegDecoderVideo();

                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });

        mThreadVideoDisplay = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!mThreadVideoDisplay.isInterrupted()) {
                    ffmpegDecoderVideoDisplay();

                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });

        mThreadDecoderVideo.start();
        mThreadVideoDisplay.start();

        return true;
    }

    public void decoderAudioFrame(byte[] data, int dataSize) {
        mAudioPlayer.writeFrame(data, dataSize);
    }

    public void decoderRelease() {
        ffmpegDecoderStop();

        if (mThreadReadFrame != null) {
            mThreadReadFrame.interrupt();

            try {
                mThreadReadFrame.join();
            } catch (InterruptedException e) {
                mThreadReadFrame.interrupt();
            }

            mThreadReadFrame = null;
        }

        UtilLog.writeDebug(getClass(), "ThreadReadFrame finish");

        if (mThreadDecoderAudio != null) {
            mThreadDecoderAudio.interrupt();

            try {
                mThreadDecoderAudio.join();
            } catch (InterruptedException e) {
                mThreadDecoderAudio.interrupt();
            }

            mThreadDecoderAudio = null;
        }

        UtilLog.writeDebug(getClass(), "ThreadDecoderAudio finish");

        mAudioPlayer.stopPlayer();

        UtilLog.writeDebug(getClass(), "stopPlayer finish");

        if (mThreadDecoderVideo != null) {
            mThreadDecoderVideo.interrupt();

            try {
                mThreadDecoderVideo.join();
            } catch (InterruptedException e) {
                mThreadDecoderVideo.interrupt();
            }

            mThreadDecoderVideo = null;
        }

        UtilLog.writeDebug(getClass(), "ThreadDecoderVideo finish");

        if (mThreadVideoDisplay != null) {
            mThreadVideoDisplay.interrupt();

            try {
                mThreadVideoDisplay.join();
            } catch (InterruptedException e) {
                mThreadVideoDisplay.interrupt();
            }

            mThreadVideoDisplay = null;
        }

        UtilLog.writeDebug(getClass(), "ThreadVideoDisplay finish");

        ffmpegDecoderRelease();

        UtilLog.writeDebug(getClass(), "decoderRelease finished");
    }

    public void decoderBufferPush(byte[] dataArray, int dataSize) {
        ffmpegDecoderBufferPush(dataArray, dataSize);
    }

    private ModelAudio.AudioPlayer.HandleAudioPlayer mHandleAudioPlayer = new ModelAudio.AudioPlayer.HandleAudioPlayer() {
        @Override
        public void onStart(int sampleRate, int channels) {

        }

        @Override
        public void onRelease() {

        }
    };

    class NotifyDecoder extends Handler {
        private final WeakReference<HandleDecoder> mWeakListener;

        private final int MSG_ON_START = 0;

        public NotifyDecoder(HandleDecoder handle) {
            mWeakListener = new WeakReference<HandleDecoder>(handle);
        }

        public void decoderStart(boolean success) {
            Bundle bundle = new Bundle();
            bundle.putBoolean("success", success);

            Message msg = new Message();
            msg.what = MSG_ON_START;
            msg.setData(bundle);

            sendMessage(msg);
        }

        @Override
        public void handleMessage(Message msg) {
            HandleDecoder handle = mWeakListener.get();
            if (handle == null)
                return;

            switch (msg.what) {
                case MSG_ON_START: {
                    Bundle bundle = msg.getData();
                    boolean success = bundle.getBoolean("success");

                    handle.decoderStart(success);
                }
                break;
            }
        }
    }
}
