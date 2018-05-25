package com.hihua.browseanimate.wifip2p;

import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import java.util.ArrayList;

/**
 * Created by hihua on 18/3/14.
 */

public class WifiP2PClient extends WifiP2PBase {
    private final HandleWifiP2PClient mHandle;

    public WifiP2PClient(HandleWifiP2PClient handle) {
        super(handle);
        mHandle = handle;
    }

    @Override
    protected void onConnectionChangedAction(NetworkInfo networkInfo) {
        if (networkInfo.isConnected()) {
            mPeerConnected = true;

            final WifiP2pManager.ConnectionInfoListener listener = new WifiP2pManager.ConnectionInfoListener() {

                @Override
                public void onConnectionInfoAvailable(WifiP2pInfo info) {
                    mHandle.onP2PConnected(info);
                }
            };

            mManager.requestConnectionInfo(mChannel, listener);
        } else {
            if (mPeerConnected) {
                mPeerConnected = false;

                mHandle.onP2PDisconnect();
            }
        }
    }

    public interface HandleWifiP2PClient extends HandleWifiP2P {

    }
}
