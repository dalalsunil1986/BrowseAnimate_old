package com.hihua.browseanimate.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.hihua.browseanimate.R;
import com.hihua.browseanimate.cmd.CmdBase;
import com.hihua.browseanimate.cmd.CmdServer;
import com.hihua.browseanimate.model.ModelCamera;
import com.hihua.browseanimate.push.PushBase;
import com.hihua.browseanimate.push.PushEncoder;
import com.hihua.browseanimate.socket.SocketBuffer;
import com.hihua.browseanimate.socket.SocketServer;
import com.hihua.browseanimate.util.UtilLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by hihua on 18/3/16.
 */

public class ServiceMain extends Service {
    private BinderMain binderMain;
    private NotifyService mNotify;

    @Override
    public IBinder onBind(Intent intent) {
        return new ServiceBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        binderMain = new BinderMain(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        binderMain.closeAll();
        mNotify.onServiceDestroy();

        super.onDestroy();
    }

    public BinderMain getBinderMain() {
        return binderMain;
    }

    public void setNotifyService(HandleService handle) {
        mNotify = new NotifyService(handle);
    }

    public class ServiceBinder extends Binder {
        public ServiceMain getService() {
            return ServiceMain.this;
        }
    }

    public class BinderMain {
        private final ModelCamera modelCamera;
        private final PushEncoder pushEncoder;
        private final CmdServer cmdServer;
        private final Handler mHandler;
        private long mTimer = 0;
        private boolean mRecording = false;
        private boolean mPushing = false;

        public BinderMain(Context context) {
            mHandler = new Handler();
            modelCamera = new ModelCamera(mHandleCamera);
            pushEncoder = new PushEncoder(context, mHandleSocket);
            cmdServer = new CmdServer(context, mHandleServerSocket);
        }

        public boolean startPushListen() {
            if (pushEncoder.startListen()) {
                if (cmdServer.startListen()) {
                    mPushing = true;
                    return true;
                } else
                    pushEncoder.close();
            }

            return false;
        }

        public boolean startPushQuery() {
            return pushEncoder.startQuery();
        }

        public void closePush() {
            mPushing = false;
            cmdServer.closeListen();
            pushEncoder.close();

            mNotify.onServicePush(mPushing);
        }

        public void closeAll() {
            modelCamera.closePreView();
            modelCamera.closeRecord();
            closePush();
        }

        public boolean startPreView(Context context, SurfaceView surfaceView) {
            return modelCamera.startPreView(context, surfaceView);
        }

        public boolean startRecord(Context context, SurfaceView surfaceView) {
            if (modelCamera.startRecord(context, surfaceView)) {
                mRecording = true;
                mNotify.onServiceRecord(mRecording);
                startTimer();
                return true;
            } else
                return false;
        }

        public void closeRecord() {
            mTimer = 0;
            mRecording = false;
            mNotify.onServiceRecord(mRecording);
            modelCamera.closeRecord();
        }

        public boolean getRecording() {
            return mRecording;
        }

        public boolean getPushing() {
            return mPushing;
        }

        private void onReceive(SocketServer.ServerClient serverClient, int action, JSONObject args, JSONObject returnObject) {
            boolean reply = false;
            Context context = ServiceMain.this;

            if (action == CmdBase.ACTION.START_RECORD.ordinal()) {
                boolean pushClose = false;

                try {
                    if (args.has("push_close"))
                        pushClose = args.getBoolean("push_close");
                } catch (JSONException e) {

                }

                if (pushClose)
                    closePush();
                else
                    reply = true;

                String msg = "";

                if (getRecording())
                    msg = context.getString(R.string.push_record_started);
                else {
                    if (startRecord(context, null))
                        msg = context.getString(R.string.push_record_success);
                    else
                        msg = context.getString(R.string.push_record_fail);
                }

                try {
                    returnObject.put("msg", msg);
                } catch (JSONException e) {

                }
            }

            if (action == CmdBase.ACTION.STOP_RECORD.ordinal()) {
                boolean pushClose = false;

                try {
                    if (args.has("push_close"))
                        pushClose = args.getBoolean("push_close");
                } catch (JSONException e) {

                }

                closeRecord();

                if (pushClose)
                    closePush();
                else
                    reply = true;

                try {
                    returnObject.put("msg", context.getString(R.string.push_record_close));
                } catch (JSONException e) {

                }
            }

            if (reply) {
                String content = returnObject.toString();
                boolean success = serverClient.sendString(content);
            }
        }

        public void startTimer() {
            final long delay = 800;
            mTimer = 0;

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mTimer += delay;

                    mNotify.onServiceTimer(mTimer);

                    if (mRecording)
                        mHandler.postDelayed(this, delay);
                }
            }, delay);
        }

        public void startTip(int resId) {
            Toast toast = Toast.makeText(ServiceMain.this, resId, Toast.LENGTH_LONG);
            toast.show();
        }

        private PushEncoder.HandleSocket mHandleSocket = new PushEncoder.HandleSocket() {
            @Override
            public void onServerListen(String address, int port) {

            }

            @Override
            public void onServerAccept(String address) {

            }

            @Override
            public void onServerDisconnect(String address) {

            }

            @Override
            public void onServerP2PQuery(boolean stop) {

            }

            @Override
            public void onServerP2PDevices(ArrayList<WifiP2pDevice> devices, String address) {

            }

            @Override
            public void onServerP2PConnected(WifiP2pInfo info) {

            }

            @Override
            public void onServerError(PushBase.Error error) {
                switch (error) {
                    case ERROR_WIFIP2P: {
                        closePush();
                        showTips(R.string.push_error_wifip2p);
                        mNotify.onServiceError();
                    }
                    break;

                    case ERROR_DEIVCE: {
                        closePush();
                        showTips(R.string.push_error_device);
                        mNotify.onServiceError();
                    }
                    break;

                    case ERROR_NO_FOUND_DEVICE: {
                        showTips(R.string.push_error_no_found_device);
                    }
                    break;

                    case ERROR_ENCODER: {
                        closePush();
                        showTips(R.string.push_error_encoder);
                        mNotify.onServiceError();
                    }
                    break;

                    case ERROR_LISTEN: {
                        closePush();
                        showTips(R.string.push_error_listen);
                    }
                    break;
                }
            }
        };

        private SocketServer.HandleServerSocket mHandleServerSocket = new SocketServer.HandleServerSocket() {
            @Override
            public void onListen(String address, int port) {

            }

            @Override
            public void onAccept(String address) {

            }

            @Override
            public void onClose() {

            }

            @Override
            public void onDisconnect(String address, boolean empty) {

            }

            @Override
            public void onReceive(SocketServer.ServerClient serverClient, SocketBuffer socketBuffer) {
                JSONObject returnObject = new JSONObject();

                try {
                    returnObject.put("code", 0);
                    returnObject.put("msg", null);
                } catch (JSONException e) {

                }

                String content = socketBuffer.toString();

                UtilLog.writeDebug(getClass(), "onReceive " + content);

                try {
                    JSONObject json = new JSONObject(content);
                    int action = json.getInt("action");
                    JSONObject args = json.getJSONObject("args");

                    BinderMain.this.onReceive(serverClient, action, args, returnObject);
                } catch (JSONException e) {
                    UtilLog.writeError(getClass(), e);
                }
            }
        };

        private ModelCamera.HandleCamera mHandleCamera = new ModelCamera.HandleCamera() {
            @Override
            public void onCameraFrame(byte[] data, int width, int height, int format) {

            }
        };
    }

    private class NotifyService extends Handler {
        private final WeakReference<HandleService> mWeakListener;

        private final int MSG_DESTROY = 0;
        private final int MSG_RECORD = 1;
        private final int MSG_PUSH = 2;
        private final int MSG_TIMER = 3;
        private final int MSG_ERROR = 4;

        private NotifyService(HandleService handle) {
            mWeakListener = new WeakReference<HandleService>(handle);
        }

        public void onServiceDestroy() {
            sendEmptyMessage(MSG_DESTROY);
        }

        public void onServiceRecord(boolean start) {
            Bundle bundle = new Bundle();
            bundle.putBoolean("start", start);

            Message msg = new Message();
            msg.what = MSG_RECORD;
            msg.setData(bundle);

            sendMessage(msg);
        }

        public void onServicePush(boolean start) {
            Bundle bundle = new Bundle();
            bundle.putBoolean("start", start);

            Message msg = new Message();
            msg.what = MSG_PUSH;
            msg.setData(bundle);

            sendMessage(msg);
        }

        public void onServiceTimer(long timer) {
            Bundle bundle = new Bundle();
            bundle.putLong("timer", timer);

            Message msg = new Message();
            msg.what = MSG_TIMER;
            msg.setData(bundle);

            sendMessage(msg);
        }

        public void onServiceError() {
            sendEmptyMessage(MSG_ERROR);
        }

        @Override
        public void handleMessage(Message msg) {
            HandleService handle = mWeakListener.get();
            if (handle == null)
                return;

            switch (msg.what) {
                case MSG_DESTROY: {
                    handle.onServiceDestroy();
                }
                break;

                case MSG_RECORD: {
                    Bundle bundle = msg.getData();
                    boolean start = bundle.getBoolean("start");

                    handle.onServiceRecord(start);
                }
                break;

                case MSG_PUSH: {
                    Bundle bundle = msg.getData();
                    boolean start = bundle.getBoolean("start");

                    handle.onServicePush(start);
                }
                break;

                case MSG_TIMER: {
                    Bundle bundle = msg.getData();
                    long timer = bundle.getLong("timer");

                    handle.onServiceTimer(timer);
                }
                break;

                case MSG_ERROR: {
                    handle.onServiceError();
                }
                break;
            }
        }
    }

    public interface HandleService {
        public void onServiceDestroy();
        public void onServiceRecord(boolean start);
        public void onServicePush(boolean start);
        public void onServiceTimer(long timer);
        public void onServiceError();
    }

    private void showTips(int resId) {
        Toast toast = Toast.makeText(this, resId, Toast.LENGTH_LONG);
        toast.show();
    }

    private void showTips(String msg) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        toast.show();
    }

    private void showTips(int resId, Object... formatArgs) {
        String content = getString(resId, formatArgs);
        Toast toast = Toast.makeText(this, content, Toast.LENGTH_LONG);
        toast.show();
    }
}
