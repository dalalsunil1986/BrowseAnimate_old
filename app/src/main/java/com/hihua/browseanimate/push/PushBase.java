package com.hihua.browseanimate.push;

import android.content.Context;
import android.media.AudioFormat;
import android.media.CamcorderProfile;
import android.util.Log;
import android.widget.Toast;

import com.hihua.browseanimate.R;
import com.hihua.browseanimate.config.ConfigPush;
import com.hihua.browseanimate.ffmpeg.FFmpegEncoder;
import com.hihua.browseanimate.socket.SocketBuffer;
import com.hihua.browseanimate.socket.SocketServer;

/**
 * Created by hihua on 17/10/26.
 */

public abstract class PushBase {
    protected final Context mContext;

    public PushBase(Context context) {
        mContext = context;
    }

    protected void showTips(int resId) {
        Toast toast = Toast.makeText(mContext, resId, Toast.LENGTH_LONG);
        toast.show();
    }

    protected void showTips(int resId, Object... formatArgs) {
        String content = mContext.getString(resId, formatArgs);
        Toast toast = Toast.makeText(mContext, content, Toast.LENGTH_LONG);
        toast.show();
    }

    public enum Error {
        NO_ERROR,                   //正常
        ERROR_WIFIP2P,              //WIFIP2P 不支持
        ERROR_DEIVCE,               //设备名不能为空
        ERROR_NO_FOUND_DEVICE,      //没找到设备
        ERROR_ENCODER,              //编码失败
        ERROR_DECODER,              //解码失败
        ERROR_LISTEN                //监听端口错误
    }
}
