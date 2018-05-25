package com.hihua.browseanimate.wifip2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import com.hihua.browseanimate.util.UtilLog;

import java.net.InetAddress;

import static android.os.Looper.getMainLooper;

/**
 * Created by hihua on 18/3/14.
 */

public abstract class WifiP2PBase {
    private final HandleWifiP2P mHandle;
    protected WifiP2pManager mManager;
    protected WifiP2pManager.Channel mChannel;
    protected IntentFilter mFilter;
    private final BroadcastP2P mBroadcast = new BroadcastP2P();
    protected boolean mPeerQuery = false;
    protected boolean mPeerConnect = false;
    protected boolean mPeerConnected = false;

    public WifiP2PBase(HandleWifiP2P handle) {
        mHandle = handle;
    }

    public void p2pInit(Context context) {
        if (mManager == null)
            mManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);

        if (mChannel == null)
            mChannel = mManager.initialize(context, getMainLooper(), null);

        if (mFilter == null) {
            mFilter = new IntentFilter();
            mFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            mFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            mFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            mFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
            mFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

            context.registerReceiver(mBroadcast, mFilter);
        }

        mManager.cancelConnect(mChannel, null);
        mManager.removeGroup(mChannel, null);
    }

    public void p2pClose(Context context) {
        final WifiP2pManager.ActionListener listener = new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {

            }
        };

        if (mManager != null && mChannel != null) {
            mPeerConnect = false;
            mManager.cancelConnect(mChannel, listener);
        }

        if (mManager != null && mChannel != null) {
            mPeerConnected = false;
            mManager.removeGroup(mChannel, listener);
        }

        if (mManager != null && mChannel != null) {
            mPeerQuery = false;
            mManager.stopPeerDiscovery(mChannel, listener);
        }

        if (mFilter != null) {
            context.unregisterReceiver(mBroadcast);
            mFilter = null;
        }
    }

    public void peerQuery() {
        final WifiP2pManager.ActionListener listener = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                UtilLog.writeDebug(getClass(), "peerQuery onSuccess");
            }

            @Override
            public void onFailure(int reason) {
                UtilLog.writeDebug(getClass(), "peerQuery onFailure " + reason);
            }
        };

        mManager.discoverPeers(mChannel, listener);
    }

    public void connectionInfo() {
        final WifiP2pManager.ConnectionInfoListener listener = new WifiP2pManager.ConnectionInfoListener() {

            @Override
            public void onConnectionInfoAvailable(final WifiP2pInfo info) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        InetAddress address = info.groupOwnerAddress;
                        if (address != null) {
                            UtilLog.writeDebug(getClass(), address.getHostAddress());
                        }
                    }
                });

                thread.start();
            }
        };

        mManager.requestConnectionInfo(mChannel, listener);
    }

    class BroadcastP2P extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)) {
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, WifiP2pManager.WIFI_P2P_STATE_DISABLED);
                    UtilLog.writeDebug(getClass(), "WIFI_P2P_STATE_CHANGED_ACTION " + state);
                    mHandle.onP2PState(state);
                    return;
                }

                if (action.equals(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)) {
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED);
                    UtilLog.writeDebug(getClass(), "WIFI_P2P_DISCOVERY_CHANGED_ACTION " + state);
                    onDiscoveryChanged(state);
                    return;
                }

                if (action.equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)) {
                    UtilLog.writeDebug(getClass(), "WIFI_P2P_PEERS_CHANGED_ACTION");
                    onPeersChangedAction();
                    return;
                }

                if (action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)) {
                    mPeerConnect = false;

                    NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                    UtilLog.writeDebug(getClass(), "WIFI_P2P_CONNECTION_CHANGED_ACTION " + networkInfo.getState().toString());

                    onConnectionChangedAction(networkInfo);
                    return;
                }

                if (action.equals(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)) {
                    UtilLog.writeDebug(getClass(), "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
                    return;
                }
            }
        }
    }

    protected void onDiscoveryChanged(int state) {
        switch (state) {
            case WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED:
                mPeerQuery = false;
                break;

            case WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED:
                mPeerQuery = true;
                break;
        }
    }

    protected void onPeersChangedAction() {

    }

    protected void onConnectionChangedAction(NetworkInfo networkInfo) {

    }

    public interface HandleWifiP2P {
        public void onP2PState(int state);
        public void onP2PConnected(WifiP2pInfo info);
        public void onP2PDisconnect();
    }
}
