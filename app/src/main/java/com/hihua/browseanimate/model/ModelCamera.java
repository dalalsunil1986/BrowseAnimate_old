package com.hihua.browseanimate.model;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.hihua.browseanimate.config.ConfigRecord;
import com.hihua.browseanimate.util.UtilDateTime;
import com.hihua.browseanimate.util.UtilLog;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by hihua on 17/11/14.
 */

public class ModelCamera {
    private Camera mCamera = null;
    private byte[] mCameraData = null;
    private SurfaceTexture[] mSurfaceTexture = { null, null, null };
    private Surface mSurface;
    private MediaRecorder mMediaRecorder = null;
    private final int mImageFormat = ImageFormat.NV21;
    private final int mVideoFormat = 0x18;
    private final NotifyCamera mNotifyCamera;

    public ModelCamera(HandleCamera handle) {
        mNotifyCamera = new NotifyCamera(handle);
    }

    public boolean startCameraPush(final int width, final int height) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
        }

        mCamera = Camera.open();
        mCamera.setDisplayOrientation(90);

        if (mSurfaceTexture[0] != null)
            mSurfaceTexture[0].release();

        mSurfaceTexture[0] = new SurfaceTexture(10);

        try {
            mCamera.setPreviewTexture(mSurfaceTexture[0]);
        } catch (IOException e) {
            UtilLog.writeError(getClass(),  e);
            closeCameraPush();
            return false;
        }

        Camera.Parameters parameters = mCamera.getParameters();

        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes != null) {
            for (String focusMode : focusModes) {
                if (focusMode.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
        }

        if (parameters.getVideoStabilization())
            parameters.setVideoStabilization(true);

        String antibanding = parameters.getAntibanding();
        if (antibanding != null && antibanding.contains(Camera.Parameters.ANTIBANDING_60HZ))
            parameters.setAntibanding(Camera.Parameters.ANTIBANDING_60HZ);
        else
            parameters.setAntibanding(Camera.Parameters.ANTIBANDING_AUTO);

        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        parameters.setPreviewFormat(mImageFormat);
        parameters.setPreviewSize(width, height);
        mCamera.setParameters(parameters);

        mCameraData = new byte[width * height * ImageFormat.getBitsPerPixel(mImageFormat) / 8];
        mCamera.addCallbackBuffer(mCameraData);

        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (mCamera != null) {
                    Camera.Parameters parameters = mCamera.getParameters();

                    mNotifyCamera.onCameraFrame(data, parameters.getPreviewSize().width, parameters.getPreviewSize().height, mVideoFormat);
                    mCamera.addCallbackBuffer(data);
                }
            }
        });

        mCamera.setErrorCallback(new Camera.ErrorCallback() {
            @Override
            public void onError(int error, Camera camera) {
                UtilLog.writeDebug(getClass(), "camera error: " + error);
            }
        });

        mCamera.startPreview();
        return true;
    }

    public boolean startPreView(Context context, SurfaceView surfaceView) {
        ConfigRecord configRecord = ConfigRecord.getProfile(context);
        if (configRecord == null)
            configRecord = ConfigRecord.initProfile();

        if (configRecord == null)
            return false;

        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
        }

        mCamera = Camera.open();
        mCamera.setDisplayOrientation(90);

        if (surfaceView == null) {
            if (mSurfaceTexture[1] != null)
                mSurfaceTexture[1].release();

            mSurfaceTexture[1] = new SurfaceTexture(10);

            try {
                mCamera.setPreviewTexture(mSurfaceTexture[1]);
            } catch (IOException e) {
                UtilLog.writeError(getClass(), e);
                closePreView();
                return false;
            }

            Camera.Size size = getPreviewSize(mCamera);
            setPreviewParameters(mCamera, size.width, size.height);
        } else {
            SurfaceHolder holder = surfaceView.getHolder();

            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                UtilLog.writeError(getClass(), e);
                closePreView();
                return false;
            }

            setPreviewParameters(mCamera, 0, 0);
        }

        mCamera.startPreview();
        return true;
    }

    public boolean startRecord(Context context, SurfaceView surfaceView) {
        ConfigRecord configRecord = ConfigRecord.getProfile(context);
        if (configRecord == null)
            configRecord = ConfigRecord.initProfile();

        if (configRecord == null)
            return false;

        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
        }

        mMediaRecorder = new MediaRecorder();

        if (surfaceView != null) {
            SurfaceHolder holder = surfaceView.getHolder();
            mMediaRecorder.setPreviewDisplay(holder.getSurface());
        } else {
            if (mSurfaceTexture[2] != null)
                mSurfaceTexture[2].release();

            mSurfaceTexture[2] = new SurfaceTexture(10);

            if (mSurface != null)
                mSurface.release();

            mSurface = new Surface(mSurfaceTexture[2]);

            mMediaRecorder.setPreviewDisplay(mSurface);
        }

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mMediaRecorder.setOutputFormat(configRecord.getFileFormat());
        mMediaRecorder.setAudioEncodingBitRate(configRecord.getAudioBitRate());
        mMediaRecorder.setAudioChannels(configRecord.getAudioChannels());
        mMediaRecorder.setAudioSamplingRate(configRecord.getAudioSampleRate());
        mMediaRecorder.setAudioEncoder(configRecord.getAudioCodec());
        mMediaRecorder.setVideoEncodingBitRate(configRecord.getVideoBitRate());
        mMediaRecorder.setVideoSize(configRecord.getVideoFrameWidth(), configRecord.getVideoFrameHeight());
        mMediaRecorder.setVideoFrameRate(configRecord.getVideoFrameRate());
        mMediaRecorder.setVideoEncoder(configRecord.getVideoCodec());
        mMediaRecorder.setOrientationHint(configRecord.getOrientationHint());

        String outFile = getOutFile();
        mMediaRecorder.setOutputFile(outFile);
        mMediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                //String log = String.format("media record err %d %d", what, extra);
                //UtilLog.writeDebug(getClass(), log);
            }
        });

        mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                //String log = String.format("media record info %d %d", what, extra);
                //UtilLog.writeDebug(getClass(), log);
            }
        });

        try {
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            return true;
        } catch (IOException e) {
            UtilLog.writeError(getClass(), e);
            closeRecord();
            return false;
        }
    }

    private Camera.Size getPreviewSize(Camera camera) {
        Camera.Size cameraSize = null;

        Camera.Parameters parameters = camera.getParameters();
        if (parameters != null) {
            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
            if (sizes != null) {
                for (Camera.Size size : sizes) {
                    if (cameraSize == null || (size.width > cameraSize.width && size.height > cameraSize.height))
                        cameraSize = size;
                }
            }
        }

        return cameraSize;
    }

    private void setPreviewParameters(Camera camera, int width, int height) {
        Camera.Parameters parameters = camera.getParameters();

        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes != null) {
            for (String focusMode : focusModes) {
                if (focusMode.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
        }

        if (parameters.getVideoStabilization())
            parameters.setVideoStabilization(true);

        String antibanding = parameters.getAntibanding();
        if (antibanding != null) {
            if (antibanding.contains(Camera.Parameters.ANTIBANDING_60HZ))
                parameters.setAntibanding(Camera.Parameters.ANTIBANDING_60HZ);
            else {
                if (antibanding.contains(Camera.Parameters.ANTIBANDING_AUTO))
                    parameters.setAntibanding(Camera.Parameters.ANTIBANDING_AUTO);
            }
        }

        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);

        if (width > 0 && height > 0)
            parameters.setPreviewSize(width, height);

        camera.setParameters(parameters);
    }

    private String getOutFile() {
        String date = UtilDateTime.getNow("yyyyMMddHHmmss");

        File root = Environment.getExternalStorageDirectory();
        String dirpath = root.getPath() + "/browseanimate/video/";

        File dir = new File(dirpath);
        if (!dir.exists())
            dir.mkdirs();

        return dirpath + date + ".mp4";
    }

    public void closeCameraPush() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        if (mSurfaceTexture[0] != null) {
            mSurfaceTexture[0].release();
            mSurfaceTexture[0] = null;
        }
    }

    public void closePreView() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        if (mSurfaceTexture[1] != null) {
            mSurfaceTexture[1].release();
            mSurfaceTexture[1] = null;
        }
    }

    public void closeRecord() {
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }

        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }

        if (mSurfaceTexture[2] != null) {
            mSurfaceTexture[2].release();
            mSurfaceTexture[2] = null;
        }
    }

    class NotifyCamera extends Handler {
        private final WeakReference<HandleCamera> mWeakListener;

        public NotifyCamera(HandleCamera handle) {
            mWeakListener = new WeakReference<HandleCamera>(handle);
        }

        public void onCameraFrame(byte[] data, int width, int height, int format) {
            HandleCamera handle = mWeakListener.get();
            if (handle == null)
                return;

            handle.onCameraFrame(data, width, height, format);
        }

        @Override
        public void handleMessage(Message msg) {
            HandleCamera handle = mWeakListener.get();
            if (handle == null)
                return;
        }
    }

    public interface HandleCamera {
        public void onCameraFrame(byte[] data, int width, int height, int format);
    }
}
