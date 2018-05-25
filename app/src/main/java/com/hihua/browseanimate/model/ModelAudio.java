package com.hihua.browseanimate.model;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;

/**
 * Created by hihua on 17/11/27.
 */

public class ModelAudio {
    public static class AudioRecord {
        private android.media.AudioRecord mAudioRecord;
        private AcousticEchoCanceler mAudioEcho;
        private AutomaticGainControl mAudioGain;
        private NoiseSuppressor mAudioNoise;
        private byte[] mBuffer = null;
        private NotifyAudioRecord mNotify = null;
        private Thread mThread = null;

        public AudioRecord(HandleAudioRecord handle) {
            mNotify = new NotifyAudioRecord(handle);
        }

        public void startRecord(final int sampleRate, final int channelConfig) {
            if (mAudioRecord == null) {
                final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
                final int frameSize = channelConfig == AudioFormat.CHANNEL_IN_STEREO ? 4 : 2;
                final int channels = channelConfig == AudioFormat.CHANNEL_IN_STEREO ? 2 : 1;
                //final int bufferSize = 1024 * frameSize;
                final int bufferSize = android.media.AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

                mBuffer = new byte[bufferSize];
                mAudioRecord = new android.media.AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize);

                if (AcousticEchoCanceler.isAvailable()) {
                    mAudioEcho = AcousticEchoCanceler.create(mAudioRecord.getAudioSessionId());
                    if (mAudioEcho != null)
                        mAudioEcho.setEnabled(true);
                }

                if (AutomaticGainControl.isAvailable()) {
                    mAudioGain = AutomaticGainControl.create(mAudioRecord.getAudioSessionId());
                    if (mAudioGain != null)
                        mAudioGain.setEnabled(true);
                }

                if (NoiseSuppressor.isAvailable()) {
                    mAudioNoise = NoiseSuppressor.create(mAudioRecord.getAudioSessionId());
                    if (mAudioNoise != null)
                        mAudioNoise.setEnabled(true);
                }

                mAudioRecord.startRecording();

                mThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

                        int offset = 0;
                        int length = mBuffer.length;

                        while (mAudioRecord.getRecordingState() == android.media.AudioRecord.RECORDSTATE_RECORDING) {
                            int len = mAudioRecord.read(mBuffer, offset, length - offset);
                            if (len > 0) {
                                offset += len;
                                if (offset == length)
                                    mNotify.onFrame(mBuffer, sampleRate, channels);

                                offset = offset % length;
                            }

                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {

                            }
                        }

                        mAudioRecord.release();
                        mAudioRecord = null;
                    }
                });

                mThread.start();
            }
        }

        public void stopRecord() {
            if (mAudioRecord != null)
                mAudioRecord.stop();

            if (mThread != null) {
                try {
                    mThread.join();
                } catch (InterruptedException e) {

                }

                mThread = null;
            }

            if (mAudioEcho != null) {
                mAudioEcho.setEnabled(false);
                mAudioEcho.release();
                mAudioEcho = null;
            }

            if (mAudioGain != null) {
                mAudioGain.setEnabled(false);
                mAudioGain.release();
                mAudioGain = null;
            }

            if (mAudioNoise != null) {
                mAudioNoise.setEnabled(false);
                mAudioNoise.release();
                mAudioNoise = null;
            }
        }

        private class NotifyAudioRecord extends Handler {
            private final WeakReference<HandleAudioRecord> mWeakListener;

            public NotifyAudioRecord(HandleAudioRecord handle) {
                mWeakListener = new WeakReference<HandleAudioRecord>(handle);
            }

            public void onFrame(byte[] data, int sampleRate, int channels) {
                HandleAudioRecord handle = mWeakListener.get();
                if (handle == null)
                    return;

                handle.onFrame(data, sampleRate, channels);
            }

            @Override
            public void handleMessage(Message msg) {
                HandleAudioRecord handle = mWeakListener.get();
                if (handle == null)
                    return;
            }
        }

        public interface HandleAudioRecord {
            public void onFrame(byte[] data, int sampleRate, int channels);
        }
    }

    public static class AudioPlayer {
        private android.media.AudioTrack mAudioTrack;
        private NotifyAudioPlayer mNotify = null;

        public AudioPlayer(HandleAudioPlayer handle) {
            mNotify = new NotifyAudioPlayer(handle);
        }

        public void startPlayer(int sampleRate, int channels) {
            if (mAudioTrack == null) {
                final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
                final int channelConfig = channels > 1 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
                final int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);

                mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig, audioFormat, minBufferSize, AudioTrack.MODE_STREAM);
                //mAudioTrack.setStereoVolume(0, 0);
                mAudioTrack.play();

                mNotify.onStart(sampleRate, channels);
            }
        }

        public void writeFrame(byte[] data, int dataSize) {
            if (mAudioTrack != null && mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                mAudioTrack.write(data, 0, dataSize);
            }
        }

        public void stopPlayer() {
            if (mAudioTrack != null) {
                mAudioTrack.stop();
                mAudioTrack.release();
                mAudioTrack = null;
            }

            mNotify.onRelease();
        }

        private class NotifyAudioPlayer extends Handler {
            private final WeakReference<HandleAudioPlayer> mWeakListener;

            private final int MSG_START = 0;
            private final int MSG_RELEASE = 1;

            public NotifyAudioPlayer(HandleAudioPlayer handle) {
                mWeakListener = new WeakReference<HandleAudioPlayer>(handle);
            }

            public void onStart(int sampleRate, int channels) {
                Bundle bundle = new Bundle();
                bundle.putInt("samplerate", sampleRate);
                bundle.putInt("channels", channels);

                Message msg = new Message();
                msg.what = MSG_START;
                msg.setData(bundle);

                sendMessage(msg);
            }

            public void onRelease() {
                sendEmptyMessage(MSG_RELEASE);
            }

            @Override
            public void handleMessage(Message msg) {
                HandleAudioPlayer handle = mWeakListener.get();
                if (handle == null)
                    return;

                switch (msg.what) {
                    case MSG_START: {
                        Bundle bundle = msg.getData();
                        int sampleRate = bundle.getInt("samplerate");
                        int channels = bundle.getInt("channels");

                        handle.onStart(sampleRate, channels);
                    }
                    break;

                    case MSG_RELEASE:
                        handle.onRelease();
                        break;
                }
            }
        }

        public interface HandleAudioPlayer {
            public void onStart(int sampleRate, int channels);
            public void onRelease();
        }
    }
}
