package com.hihua.browseanimate.cmd;

import android.content.Context;

import com.hihua.browseanimate.R;
import com.hihua.browseanimate.config.ConfigPush;
import com.hihua.browseanimate.socket.SocketBuffer;
import com.hihua.browseanimate.socket.SocketServer;

import org.json.JSONObject;

/**
 * Created by hihua on 18/3/15.
 */

public class CmdServer extends CmdBase {
    private final SocketServer mSocketServer;

    public CmdServer(Context context, SocketServer.HandleServerSocket handle) {
        super(context);

        mSocketServer = new SocketServer(handle);
    }

    public boolean startListen() {
        ConfigPush configPush = ConfigPush.getProfile(mContext);
        int portCmd = configPush.getPortCmd();

        return mSocketServer.startListen(portCmd);
    }

    public void closeListen() {
        mSocketServer.close();
    }

    /*
    private SocketServer.HandleServerSocket mHandleServer = new SocketServer.HandleServerSocket() {

        @Override
        public void onListen(String address, int port) {
            mHandle.onListen(address, port);
        }

        @Override
        public void onAccept(String address) {
            mHandle.onAccept(address);
        }

        @Override
        public void onClose() {
            mHandle.onClose();
        }

        @Override
        public void onDisconnect(String address, boolean empty) {
            mHandle.onDisconnect(address);
        }

        @Override
        public void onReceive(SocketBuffer socketBuffer) {
            String content = socketBuffer.toString();

            try {
                JSONObject json = new JSONObject(content);

                int action = json.getInt("action");
                JSONObject args = json.getJSONObject("args");

                mHandle.onReceive(action, args);
            } catch (Exception e) {
                writeLogError("onReceive", e);
            }
        }
    };

    public interface HandleServerSocket {
        public void onListen(String address, int port);
        public void onAccept(String address);
        public void onClose();
        public void onDisconnect(String address);
        public void onReceive(int action, JSONObject args);
    }
    */
}
