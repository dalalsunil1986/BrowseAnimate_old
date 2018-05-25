package com.hihua.browseanimate.wifip2p;

import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import com.hihua.browseanimate.util.UtilLog;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by hihua on 18/3/14.
 */

public class WifiP2PServer extends WifiP2PBase {
    private final HandleWifiP2PServer mHandle;

    public WifiP2PServer(HandleWifiP2PServer handle) {
        super(handle);
        mHandle = handle;
    }

    private void peerRequest() {
        final WifiP2pManager.PeerListListener listener = new WifiP2pManager.PeerListListener() {

            @Override
            public void onPeersAvailable(WifiP2pDeviceList peers) {
                Collection<WifiP2pDevice> devices = peers.getDeviceList();
                if (devices != null && devices.size() > 0) {
                    ArrayList<WifiP2pDevice> list = new ArrayList<WifiP2pDevice>();

                    for (WifiP2pDevice device : devices) {
                        StringBuilder log = new StringBuilder();
                        log.append(device.deviceName);
                        log.append(",");
                        log.append(device.deviceAddress);
                        log.append(",");

                        String status = "";

                        switch (device.status) {
                            case WifiP2pDevice.CONNECTED: {
                                status = "CONNECTED";
                            }
                            break;

                            case WifiP2pDevice.INVITED: {
                                status = "INVITED";
                            }
                            break;

                            case WifiP2pDevice.FAILED: {
                                status = "FAILED";
                            }
                            break;

                            case WifiP2pDevice.AVAILABLE: {
                                status = "AVAILABLE";
                            }
                            break;

                            case WifiP2pDevice.UNAVAILABLE: {
                                status = "UNAVAILABLE";
                            }
                            break;
                        }

                        log.append(status);

                        UtilLog.writeDebug(getClass(), log.toString());
                        list.add(device);
                    }

                    mHandle.onP2PDevices(list);
                } else
                    mHandle.onP2PDevices(null);
            }
        };

        mManager.requestPeers(mChannel, listener);
    }

    public void peerConnect(WifiP2pDevice device) {
        final WifiP2pManager.ActionListener listener = new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                UtilLog.writeDebug(getClass(), "peerConnect onSuccess");
            }

            @Override
            public void onFailure(int reason) {
                mPeerConnect = false;
                UtilLog.writeDebug(getClass(), "peerConnect onFailure " + reason);
            }
        };

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 15;

        mManager.connect(mChannel, config, listener);

        mPeerConnect = true;
    }

    @Override
    protected void onDiscoveryChanged(int state) {
        switch (state) {
            case WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED: {
                if (mPeerQuery && !mPeerConnect)
                    peerQuery();
            }
            break;

            case WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED: {
                mPeerQuery = true;
            }
            break;
        }
    }

    @Override
    protected void onPeersChangedAction() {
        if (!mPeerConnect && !mPeerConnected)
            peerRequest();
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

    public interface HandleWifiP2PServer extends HandleWifiP2P {
        public void onP2PDevices(ArrayList<WifiP2pDevice> devices);
    }
}
