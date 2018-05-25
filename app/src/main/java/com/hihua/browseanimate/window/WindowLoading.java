package com.hihua.browseanimate.window;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hihua.browseanimate.R;
import com.hihua.browseanimate.util.UtilTimer;

import org.w3c.dom.Text;

/**
 * Created by hihua on 18/1/24.
 */

public class WindowLoading {
    private static WindowLoading WindowLoading = null;
    private final UtilTimer mTimer;
    private ProgressBar mProgressBar;
    private TextView mTextView;
    private PopupWindow mWindow;
    private int mProgress = 0;

    public synchronized static void startLoading(Context context, int rId) {
        if (WindowLoading != null) {
            WindowLoading.stopLoading();
            WindowLoading = null;
        }

        WindowLoading = new WindowLoading();
        WindowLoading.beginLoading(context, rId);
    }

    public synchronized static void startLoading(Context context, String message) {
        if (WindowLoading != null) {
            WindowLoading.stopLoading();
            WindowLoading = null;
        }

        WindowLoading = new WindowLoading();
        WindowLoading.beginLoading(context, message);
    }

    public synchronized static void closeLoading() {
        if (WindowLoading != null) {
            WindowLoading.stopLoading();
            WindowLoading = null;
        }
    }

    public WindowLoading() {
        mTimer = new UtilTimer(mHandle);
    }

    public void beginLoading(Context context, int rId) {
        beginLoading(context, context.getString(rId));
    }

    public void beginLoading(Context context, String message) {
        LayoutInflater inflater = LayoutInflater.from(context);

        View parent = inflater.inflate(R.layout.window_loading, null, false);
        mProgressBar = (ProgressBar) parent.findViewById(R.id.pb_loading);
        mTextView = (TextView) parent.findViewById(R.id.tv_message);

        if (message != null && message.length() > 0)
            mTextView.setText(message);
        else
            mTextView.setText("");

        mWindow = new PopupWindow(parent, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, true);
        mWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));
        mWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                stopLoading();
            }
        });

        mWindow.update();
        mWindow.showAtLocation(parent, Gravity.CENTER, 0, 0);

        mTimer.startTimer(0, 200);
    }

    public void stopLoading() {
        mTimer.closeTimer();

        if (mWindow != null) {
            if (mWindow.isShowing())
                mWindow.dismiss();

            mWindow = null;
        }
    }

    private final UtilTimer.HandleTimer mHandle = new UtilTimer.HandleTimer() {
        @Override
        public void onTimer(long ms) {
            mProgress = (mProgress + 20) % 100;
            mProgressBar.setProgress(mProgress);
        }
    };
}
