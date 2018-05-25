package com.hihua.browseanimate.cmd;

import android.content.Context;

import com.hihua.browseanimate.config.ConfigPush;
import com.hihua.browseanimate.socket.SocketClient;

import org.json.JSONObject;

/**
 * Created by hihua on 18/3/15.
 */

public class CmdClient extends CmdBase {
    private final SocketClient mSocketClient;
    private final SocketClient.HandleClientSocket mHandle;

    public CmdClient(Context context, SocketClient.HandleClientSocket handle) {
        super(context);

        mHandle = handle;
        mSocketClient = new SocketClient(handle);
    }

    public void connect(String address) {
        ConfigPush configPush = ConfigPush.getProfile(mContext);
        int portCmd = configPush.getPortCmd();

        mSocketClient.connect(address, portCmd, 5000);
    }

    public boolean sendCmd(ACTION action, JSONObject args) {
        try {
            JSONObject json = new JSONObject();
            json.put("action", action.ordinal());
            json.put("args", args != null ? args : null);

            String content = json.toString();
            mSocketClient.sendContent(content);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void close() {
        mSocketClient.close();
    }
}
