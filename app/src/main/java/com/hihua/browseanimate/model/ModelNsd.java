package com.hihua.browseanimate.model;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import com.hihua.browseanimate.util.UtilLog;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * Created by hihua on 18/4/8.
 */

public class ModelNsd {
    private final String mName = "browseanimate_nsd";
    private final String mType = "_http._tcp.";

    public boolean registerService(Context context, NsdManager.RegistrationListener mListener) {
        InetAddress inetAddress = null;

        try {
            ServerSocket sock = new ServerSocket(0);
            inetAddress = sock.getInetAddress();
            sock.close();
        } catch (IOException e) {

        }

        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(mName);
        serviceInfo.setServiceType(mType);
        serviceInfo.setHost(inetAddress);

        Object object = context.getSystemService(Context.NSD_SERVICE);
        if (object != null) {
            NsdManager manager = (NsdManager) object;
            manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mListener);

            return true;
        }

        return false;
    }

    public void unregisterService(Context context, NsdManager.RegistrationListener mListener) {
        Object object = context.getSystemService(Context.NSD_SERVICE);
        if (object != null) {
            NsdManager manager = (NsdManager) object;
            manager.unregisterService(mListener);
        }
    }

    public boolean discoverService(Context context, NsdManager.DiscoveryListener mListener) {
        Object object = context.getSystemService(Context.NSD_SERVICE);
        if (object != null) {
            NsdManager manager = (NsdManager) object;
            manager.discoverServices(mType, NsdManager.PROTOCOL_DNS_SD, mListener);
            return true;
        }

        return false;
    }

    public void stopDiscovery(Context context, NsdManager.DiscoveryListener mListener) {
        Object object = context.getSystemService(Context.NSD_SERVICE);
        if (object != null) {
            NsdManager manager = (NsdManager) object;
            manager.stopServiceDiscovery(mListener);
        }
    }

    public boolean resolveService(Context context, NsdServiceInfo serviceInfo, NsdManager.ResolveListener mListener) {
        Object object = context.getSystemService(Context.NSD_SERVICE);
        if (object != null) {
            NsdManager manager = (NsdManager) object;
            manager.resolveService(serviceInfo, mListener);
            return true;
        }

        return false;
    }
}
