package com.hihua.browseanimate.push;

import android.content.Context;
import android.media.AudioFormat;
import android.media.CamcorderProfile;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import com.hihua.browseanimate.R;
import com.hihua.browseanimate.config.ConfigPush;
import com.hihua.browseanimate.ffmpeg.FFmpegEncoder;
import com.hihua.browseanimate.model.ModelAudio;
import com.hihua.browseanimate.model.ModelCamera;
import com.hihua.browseanimate.socket.SocketBuffer;
import com.hihua.browseanimate.socket.SocketServer;
import com.hihua.browseanimate.util.UtilLog;
import com.hihua.browseanimate.util.UtilTimer;
import com.hihua.browseanimate.wifip2p.WifiP2PServer;

import java.util.ArrayList;

/**
 * Created by hihua on 17/11/27.
 */

public class PushEncoder extends PushBase {
    private final SocketServer mSocketServer;
    private final WifiP2PServer mWifiP2PServer;
    private final FFmpegEncoder mFFmpegEncoder;
    private final ModelCamera mModelCamera;
    private final ModelAudio.AudioRecord mAudioRecord;
    private final PushEncoder.HandleSocket mHandle;
    private boolean mInitedEncoder = false;
    private final UtilTimer mUtilTimer;

    public PushEncoder(Context context, PushEncoder.HandleSocket handle) {
        super(context);

        mSocketServer = new SocketServer(mHandleServer);
        mFFmpegEncoder = new FFmpegEncoder(mHandleEncoder);
        mModelCamera = new ModelCamera(mHandleCamera);
        mAudioRecord = new ModelAudio.AudioRecord(mHandleAudioRecord);
        mWifiP2PServer = new WifiP2PServer(mHandleWifiP2PServer);
        mUtilTimer = new UtilTimer(mHandleTimer);
        mHandle = handle;
    }

    public boolean startQuery() {
        ConfigPush configPush = ConfigPush.getProfile(mContext);
        String address = configPush.getAddress();
        if (address != null && address.length() > 0) {
            mWifiP2PServer.p2pInit(mContext);
            mWifiP2PServer.peerQuery();

            return true;
        } else
            return false;
    }

    public boolean startListen() {
        ConfigPush configPush = ConfigPush.getProfile(mContext);
        int portData = configPush.getPortData();

        return mSocketServer.startListen(portData);
    }

    public void close() {
        mSocketServer.close();
        mWifiP2PServer.p2pClose(mContext);
        mUtilTimer.closeTimer();
    }

    private int encoderInit() {
        ConfigPush configPush = ConfigPush.getProfile(mContext);

        String config = ConfigPush.parse(configPush);

        return mFFmpegEncoder.encoderInit(config);
    }

    private SocketServer.HandleServerSocket mHandleServer = new SocketServer.HandleServerSocket() {
        @Override
        public void onListen(String address, int port) {
            mHandle.onServerListen(address, port);
        }

        @Override
        public void onAccept(String address) {
            UtilLog.writeDebug(getClass(), mContext.getString(R.string.socket_server_client_accept, address));

            mHandle.onServerAccept(address);

            if (!mInitedEncoder) {
                mInitedEncoder = true;

                int ret = encoderInit();

                if (ret > -1) {
                    if ((ret & 1) > 0)
                        mAudioRecord.startRecord(44100, AudioFormat.CHANNEL_IN_STEREO);

                    if ((ret & 2) > 0) {
                        CamcorderProfile camcorderProfile = null;

                        if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P))
                            camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
                        else
                            camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);

                        mModelCamera.startCameraPush(camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight);
                    }
                } else
                    mHandle.onServerError(Error.ERROR_ENCODER);

                mUtilTimer.startTimer(10000, 60 * 1000);
            }
        }

        @Override
        public void onClose() {
            mModelCamera.closeCameraPush();

            mAudioRecord.stopRecord();

            mFFmpegEncoder.encoderRelease();
            mInitedEncoder = false;
        }

        @Override
        public void onDisconnect(String address, boolean empty) {
            mHandle.onServerDisconnect(address);

            if (empty) {
                mModelCamera.closeCameraPush();

                mAudioRecord.stopRecord();

                mFFmpegEncoder.encoderRelease();
                mInitedEncoder = false;
            }
        }

        @Override
        public void onReceive(SocketServer.ServerClient serverClient, SocketBuffer socketBuffer) {

        }
    };

    private FFmpegEncoder.HandleEncoder mHandleEncoder = new FFmpegEncoder.HandleEncoder() {
        @Override
        public void encoderHeader(byte[] data, int dataSize) {
            mSocketServer.putHeader(data, dataSize);
        }

        @Override
        public void encoderFrame(byte[] data, int dataSize) {
            mSocketServer.putData(data, dataSize);
        }

        @Override
        public void encoderTail(byte[] data, int dataSize) {

        }
    };

    private ModelCamera.HandleCamera mHandleCamera = new ModelCamera.HandleCamera() {

        @Override
        public void onCameraFrame(byte[] data, int width, int height, int format) {
            //UtilLog.writeDebug(getClass(), "video frame: " + width + "*" + height + " " + data.length);
            mFFmpegEncoder.encoderVideo(data, width, height, format);
        }
    };

    private ModelAudio.AudioRecord.HandleAudioRecord mHandleAudioRecord = new ModelAudio.AudioRecord.HandleAudioRecord() {

        @Override
        public void onFrame(byte[] data, int sampleRate, int channels) {
            //UtilLog.writeDebug(getClass(), "audio frame: " + sampleRate + " " + channels + " " + data.length);
            mFFmpegEncoder.encoderAudio(data, channels, sampleRate);
        }
    };

    private final WifiP2PServer.HandleWifiP2PServer mHandleWifiP2PServer = new WifiP2PServer.HandleWifiP2PServer() {
        @Override
        public void onP2PDevices(ArrayList<WifiP2pDevice> devices) {
            ConfigPush configPush = ConfigPush.getProfile(mContext);
            String address = configPush.getAddress();

            if (devices != null) {
                for (final WifiP2pDevice device : devices) {
                    if (address != null && address.equalsIgnoreCase(device.deviceAddress)) {
                        if (device.status == WifiP2pDevice.AVAILABLE || device.status == WifiP2pDevice.FAILED)
                            mWifiP2PServer.peerConnect(device);

                        return;
                    }
                }
            }
        }

        @Override
        public void onP2PState(int state) {
            if (state != WifiP2pManager.WIFI_P2P_STATE_ENABLED)
                mHandle.onServerError(Error.ERROR_WIFIP2P);
        }

        @Override
        public void onP2PConnected(WifiP2pInfo info) {
            mHandle.onServerP2PConnected(info);
        }

        @Override
        public void onP2PDisconnect() {
            mSocketServer.closeClient();
            mUtilTimer.closeTimer();
            mWifiP2PServer.peerQuery();
        }
    };

    private UtilTimer.HandleTimer mHandleTimer = new UtilTimer.HandleTimer() {
        @Override
        public void onTimer(long ms) {
            mWifiP2PServer.connectionInfo();
        }
    };

    public interface HandleSocket {
        public void onServerListen(String address, int port);
        public void onServerAccept(String address);
        public void onServerDisconnect(String address);
        public void onServerP2PQuery(boolean stop);
        public void onServerP2PDevices(ArrayList<WifiP2pDevice> devices, String address);
        public void onServerP2PConnected(WifiP2pInfo info);
        public void onServerError(Error error);
    }
}
