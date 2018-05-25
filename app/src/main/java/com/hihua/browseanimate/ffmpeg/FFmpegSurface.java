package com.hihua.browseanimate.ffmpeg;

import android.view.Surface;

/**
 * Created by hihua on 17/12/26.
 */

public class FFmpegSurface extends FFmpegBase {

    private native void ffmpegSurfaceSet(Surface surface);
    private native void ffmpegSurfaceRelease();

    public void surfaceSet(Surface surface) {
        ffmpegSurfaceSet(surface);
    }

    public void surfaceRelease() {
        ffmpegSurfaceRelease();
    }
}
