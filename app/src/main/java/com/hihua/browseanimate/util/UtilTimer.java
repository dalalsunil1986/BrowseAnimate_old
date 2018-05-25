package com.hihua.browseanimate.util;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.hihua.browseanimate.ffmpeg.FFmpegBase;
import com.hihua.browseanimate.model.ModelCamera;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by hihua on 18/1/22.
 */

public class UtilTimer {
    private final NotifyTimer mNotify;
    private Timer mTimer;

    public UtilTimer(HandleTimer handle) {
        mNotify = new NotifyTimer(handle);
    }

    public void startTimer(final long delay, final long ms) {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            private long mLastTimer = 0;

            @Override
            public void run() {
                mNotify.onTimer(mLastTimer > 0 ? System.currentTimeMillis() - mLastTimer : ms);
                mLastTimer = System.currentTimeMillis();
            }
        }, delay, ms);
    }

    public void closeTimer() {
        if (mTimer != null)
            mTimer.cancel();
    }

    class NotifyTimer extends Handler {
        private final WeakReference<HandleTimer> mWeakListener;

        private final int MSG_ON_TIMER = 0;

        public NotifyTimer(HandleTimer handle) {
            mWeakListener = new WeakReference<HandleTimer>(handle);
        }

        public void onTimer(long ms) {
            Bundle bundle = new Bundle();
            bundle.putLong("ms", ms);

            Message msg = new Message();
            msg.what = MSG_ON_TIMER;
            msg.setData(bundle);

            sendMessage(msg);
        }

        @Override
        public void handleMessage(Message msg) {
            HandleTimer handle = mWeakListener.get();
            if (handle == null)
                return;

            switch (msg.what) {
                case MSG_ON_TIMER: {
                    Bundle bundle = msg.getData();
                    long ms = bundle.getLong("ms");
                    handle.onTimer(ms);
                }
                break;
            }
        }
    }

    public interface HandleTimer {
        public void onTimer(long ms);
    }
}
