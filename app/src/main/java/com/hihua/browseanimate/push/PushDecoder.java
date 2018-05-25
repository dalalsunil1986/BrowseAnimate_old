package com.hihua.browseanimate.push;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.view.Surface;

import com.hihua.browseanimate.config.ConfigPush;
import com.hihua.browseanimate.ffmpeg.FFmpegBase;
import com.hihua.browseanimate.ffmpeg.FFmpegDecoder;
import com.hihua.browseanimate.ffmpeg.FFmpegSurface;
import com.hihua.browseanimate.socket.SocketBuffer;
import com.hihua.browseanimate.socket.SocketClient;
import com.hihua.browseanimate.util.UtilLog;
import com.hihua.browseanimate.util.UtilTimer;
import com.hihua.browseanimate.wifip2p.WifiP2PClient;

import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Created by hihua on 17/11/27.
 */

public class PushDecoder extends PushBase {
    private final SocketClient mSocketClient;
    private final WifiP2PClient mWifiP2PClient;
    private final FFmpegDecoder mFFmpegDecoder;
    private final FFmpegSurface mFFmpegSurface;
    private final HandleDecoder mHandle;
    private final UtilTimer mTimer;
    private long mPacket = 0, mTimers = 0, mSpeed = 0;
    private String mAddress;
    private boolean mConnect = false;

    public PushDecoder(Context context, HandleDecoder handle) {
        super(context);

        mSocketClient = new SocketClient(mHandleClientSocket);
        mFFmpegDecoder = new FFmpegDecoder(mHandleDecoder);
        mFFmpegSurface = new FFmpegSurface();
        mTimer = new UtilTimer(mHandleTimer);
        mWifiP2PClient = new WifiP2PClient(mHandleWifiP2PClient);
        mHandle = handle;
    }

    public String getAddress() {
        return mAddress;
    }

    public void startQuery() {
        if (mAddress != null)
            startConnect(mAddress);
        else {
            mWifiP2PClient.p2pInit(mContext);
            mWifiP2PClient.peerQuery();
        }
    }

    public void startConnect(String address) {
        ConfigPush configPush = ConfigPush.getProfile(mContext);
        int portData = configPush.getPortData();

        mSocketClient.connect(address, portData, 5000);
        mConnect = true;
    }

    public void closeConnect() {
        mSocketClient.close();
    }

    public void close() {
        mSocketClient.close();
        mWifiP2PClient.p2pClose(mContext);
    }

    public void release() {
        close();
        mFFmpegSurface.surfaceRelease();
    }

    public void surfaceSet(Surface surface) {
        mFFmpegSurface.surfaceSet(surface);
    }

    private SocketClient.HandleClientSocket mHandleClientSocket = new SocketClient.HandleClientSocket() {
        @Override
        public void onConnect(String addr, int port) {
            UtilLog.writeDebug(getClass(), String.format("%s:%d connect success", addr, port));

            mFFmpegDecoder.decoderStart();
            mTimer.startTimer(0, 1000);

            mHandle.onClientConnect(addr, port);
        }

        @Override
        public void onDisconnect(String addr, int port) {
            UtilLog.writeDebug(getClass(), String.format("%s:%d disconnect", addr, port));

            mFFmpegDecoder.decoderRelease();
            mTimer.closeTimer();
            mPacket = 0;
            mTimers = 0;
            mSpeed = 0;
            mConnect = false;

            mHandle.onClientDisconnect(addr, port);
        }

        @Override
        public void onSend(boolean success, String content) {

        }

        @Override
        public void onReceive(SocketBuffer socketBuffer) {
            mPacket += socketBuffer.getDataLength();
            mSpeed += socketBuffer.getDataLength();
            mFFmpegDecoder.decoderBufferPush(socketBuffer.getBuffer(), socketBuffer.getDataLength());
        }
    };

    private FFmpegDecoder.HandleDecoder mHandleDecoder = new FFmpegBase.HandleDecoder() {
        @Override
        public void decoderStart(boolean success) {
            if (!success) {
                mHandle.onClientError(Error.ERROR_DECODER);
            }
        }
    };

    private final UtilTimer.HandleTimer mHandleTimer = new UtilTimer.HandleTimer() {
        @Override
        public void onTimer(long ms) {
            mHandle.onClientPacket(mPacket);
            mHandle.onClientSpeed(mSpeed, ms);

            mSpeed = 0;
            mTimers += ms;
            mHandle.onClientTimer(mTimers);
        }
    };

    private final WifiP2PClient.HandleWifiP2PClient mHandleWifiP2PClient = new WifiP2PClient.HandleWifiP2PClient() {
        @Override
        public void onP2PState(int state) {
            if (state != WifiP2pManager.WIFI_P2P_STATE_ENABLED)
                mHandle.onClientError(Error.ERROR_WIFIP2P);
        }

        @Override
        public void onP2PConnected(final WifiP2pInfo info) {
            mHandle.onClientP2PConnected(info);

            if (!mConnect) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        InetAddress address = info.groupOwnerAddress;
                        if (address != null) {
                            mAddress = address.getHostAddress();
                            startConnect(address.getHostAddress());
                        }
                    }
                });

                thread.start();
            }

        }

        @Override
        public void onP2PDisconnect() {

        }
    };

    public interface HandleDecoder {
        public void onClientConnect(String addr, int port);
        public void onClientDisconnect(String addr, int port);
        public void onClientError(Error error);
        public void onClientPacket(long packet);
        public void onClientSpeed(long speed, long ms);
        public void onClientTimer(long timer);
        public void onClientP2PQuery(boolean stop);
        public void onClientP2PDevices(ArrayList<WifiP2pDevice> devices);
        public void onClientP2PConnected(WifiP2pInfo info);
    }
}
