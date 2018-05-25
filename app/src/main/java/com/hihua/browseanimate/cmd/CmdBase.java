package com.hihua.browseanimate.cmd;

import android.content.Context;
import android.util.Log;

/**
 * Created by hihua on 18/3/15.
 */

public abstract class CmdBase {
    protected final Context mContext;

    protected CmdBase(Context context) {
        mContext = context;
    }

    public enum ACTION {
        START_RECORD,
        STOP_RECORD
    }
}
