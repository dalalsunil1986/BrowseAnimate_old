package com.hihua.browseanimate.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.Editable;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hihua.browseanimate.R;
import com.hihua.browseanimate.cmd.CmdBase;
import com.hihua.browseanimate.cmd.CmdClient;
import com.hihua.browseanimate.push.PushBase;
import com.hihua.browseanimate.push.PushDecoder;
import com.hihua.browseanimate.socket.SocketBuffer;
import com.hihua.browseanimate.socket.SocketClient;
import com.hihua.browseanimate.util.UtilLog;
import com.hihua.browseanimate.window.WindowLoading;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hihua on 17/11/27.
 */

public class ActivityDecoder extends Activity {
    private ViewGroup mPlayerFullscreen;
    private ViewGroup mPlayerWindow;
    private SurfaceView mSurfaceView;
    private ViewGroup mPlayerStatus;
    private ImageView mPlayerStart;
    private ImageView mPlayerPause;
    private ImageView mPlayerRecord;
    private TextView mPlayerPacket;
    private TextView mPlayerTimer;
    private TextView mPlayerSpeed;
    private ImageView mPlayerScreen;
    private PushDecoder mPushDecoder;
    private CmdClient mCmdClient;
    private TextView tvLog;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();
        mPushDecoder = new PushDecoder(this, mHandleDecoder);

        setContentView(R.layout.activity_decoder_test);
        setControl();

        lockScreen(true);
    }

    @Override
    protected void onDestroy() {
        if (mCmdClient != null) {
            mCmdClient.close();
            mCmdClient = null;
        }

        mPushDecoder.release();
        lockScreen(false);

        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void setControl() {
        mPlayerFullscreen = (ViewGroup) findViewById(R.id.rl_player_fullscreen);
        mPlayerWindow = (ViewGroup) findViewById(R.id.rl_player_window);
        mSurfaceView = (SurfaceView) findViewById(R.id.sv_player_show);
        mPlayerStatus = (ViewGroup) findViewById(R.id.ll_player_status);
        mPlayerStart = (ImageView) findViewById(R.id.iv_player_start);
        mPlayerPause = (ImageView) findViewById(R.id.iv_player_pause);
        mPlayerRecord = (ImageView) findViewById(R.id.iv_player_record);
        mPlayerPacket = (TextView) findViewById(R.id.tv_player_packet);
        mPlayerTimer = (TextView) findViewById(R.id.tv_player_timer);
        mPlayerSpeed = (TextView) findViewById(R.id.tv_player_speed);
        mPlayerScreen = (ImageView) findViewById(R.id.iv_player_screen);

        mPlayerStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*
                final AlertDialog.Builder dialog = new AlertDialog.Builder(ActivityDecoder.this);
                dialog.setIcon(R.drawable.abc_ic_cab_done_holo_light);
                dialog.setTitle(R.string.push_select_method).setMessage(R.string.push_select_network);
                dialog.setPositiveButton(R.string.push_select_wifip2p, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPushDecoder.startQuery();

                        mPlayerStart.setVisibility(View.GONE);
                        mPlayerPause.setVisibility(View.VISIBLE);

                        dialog.dismiss();
                    }
                });
                dialog.setNeutralButton(R.string.push_select_wifi, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final EditText editText = new EditText(ActivityDecoder.this);
                        AlertDialog.Builder inputDialog = new AlertDialog.Builder(ActivityDecoder.this);
                        inputDialog.setTitle(R.string.push_select_address).setView(editText);
                        inputDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Editable editable = editText.getText();
                                String address = editable.toString();
                                if (address != null && address.length() > 0) {
                                    mPushDecoder.startConnect(address);

                                    mPlayerStart.setVisibility(View.GONE);
                                    mPlayerPause.setVisibility(View.VISIBLE);
                                }
                            }
                        }).show();

                        dialog.dismiss();
                    }
                });
                dialog.setNegativeButton(R.string.push_select_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
                */

                mPushDecoder.startQuery();

                mPlayerStart.setVisibility(View.GONE);
                mPlayerPause.setVisibility(View.VISIBLE);
            }
        });

        mPlayerRecord.setOnClickListener(new View.OnClickListener() {
            int selectItem = 0;

            @Override
            public void onClick(View v) {
                final String address = mPushDecoder.getAddress();
                if (address != null && address.length() > 0) {
                    final JSONObject args = new JSONObject();

                    final AlertDialog.Builder dialog = new AlertDialog.Builder(ActivityDecoder.this);
                    dialog.setTitle(R.string.push_record_title);
                    dialog.setSingleChoiceItems(R.array.push_record_items, 0, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            selectItem = which;
                        }
                    });
                    dialog.setPositiveButton(R.string.push_record_confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (selectItem) {
                                case 0: {
                                    boolean pushClose = false;

                                    try {
                                        args.put("push_close", pushClose);
                                    } catch (JSONException e) {

                                    }

                                    doRecord(CmdBase.ACTION.START_RECORD, args, pushClose, address);
                                }
                                break;

                                case 1: {
                                    boolean pushClose = true;

                                    try {
                                        args.put("push_close", pushClose);
                                    } catch (JSONException e) {

                                    }

                                    doRecord(CmdBase.ACTION.START_RECORD, args, pushClose, address);
                                }
                                break;

                                case 2: {
                                    boolean pushClose = false;

                                    try {
                                        args.put("push_close", pushClose);
                                    } catch (JSONException e) {

                                    }

                                    doRecord(CmdBase.ACTION.STOP_RECORD, args, pushClose, address);
                                }
                                break;

                                case 3: {
                                    boolean pushClose = true;

                                    try {
                                        args.put("push_close", pushClose);
                                    } catch (JSONException e) {

                                    }

                                    doRecord(CmdBase.ACTION.STOP_RECORD, args, pushClose, address);
                                }
                                break;
                            }
                        }
                    });
                    dialog.setNegativeButton(R.string.push_record_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    dialog.show();
                }
            }
        });

        mPlayerPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPlayerStart.setVisibility(View.VISIBLE);
                mPlayerPause.setVisibility(View.GONE);

                mPushDecoder.closeConnect();
            }
        });

        mPlayerScreen.setOnClickListener(new View.OnClickListener() {
            public void moveView(ViewGroup srcViewGroup, ViewGroup destViewGroup) {
                int count = srcViewGroup.getChildCount();
                if (count > 0) {
                    List<View> views = new ArrayList<View>();

                    for (int i = 0;i < count;i++) {
                        View view = srcViewGroup.getChildAt(i);
                        views.add(view);
                    }

                    srcViewGroup.removeAllViews();

                    for (View view : views)
                        destViewGroup.addView(view);
                }
            }

            @Override
            public void onClick(View v) {
                int visibility = mPlayerFullscreen.getVisibility();
                if (visibility == View.GONE) {
                    moveView(mPlayerWindow, mPlayerFullscreen);
                    mPlayerFullscreen.setVisibility(View.VISIBLE);
                    mPlayerWindow.setVisibility(View.GONE);
                    mPlayerScreen.setImageResource(R.mipmap.ic_window);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                } else {
                    moveView(mPlayerFullscreen, mPlayerWindow);
                    mPlayerFullscreen.setVisibility(View.GONE);
                    mPlayerWindow.setVisibility(View.VISIBLE);
                    mPlayerScreen.setImageResource(R.mipmap.ic_fullscreen);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
            }
        });

        mSurfaceView.setClickable(true);
        mSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayerStatus.getVisibility() != View.VISIBLE)
                    mPlayerStatus.setVisibility(View.VISIBLE);

                mHandler.removeCallbacks(mRunnableDisplay);
                mHandler.postDelayed(mRunnableDisplay, 3 * 1000);
            }
        });

        SurfaceHolder holder = mSurfaceView.getHolder();

        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Surface surface = holder.getSurface();
                mPushDecoder.surfaceSet(surface);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        tvLog = (TextView) findViewById(R.id.tv_log);
    }

    private void doRecord(final CmdBase.ACTION action, final JSONObject args, final boolean pushClose, final String address) {
        final Context context = ActivityDecoder.this;

        if (mCmdClient != null)
            mCmdClient.close();

        mCmdClient = new CmdClient(context, new SocketClient.HandleClientSocket() {
            @Override
            public void onConnect(String addr, int port) {
                mCmdClient.sendCmd(action, args);
                WindowLoading.startLoading(context, R.string.push_record_sending);
            }

            @Override
            public void onDisconnect(String addr, int port) {

            }

            @Override
            public void onSend(boolean success, String content) {
                if (pushClose) {
                    mCmdClient.close();
                    mCmdClient = null;

                    WindowLoading.closeLoading();
                    finish();
                }
            }

            @Override
            public void onReceive(SocketBuffer socketBuffer) {
                final String content = socketBuffer.toString();
                UtilLog.writeDebug(getClass(), "recieve4: " + content);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        WindowLoading.closeLoading();

                        try {
                            JSONObject json = new JSONObject(content);
                            final String msg = json.getString("msg");

                            if (msg != null && msg.length() > 0)
                                showTips(msg);

                        } catch (JSONException e) {

                        }
                    }
                });

                mCmdClient.close();
                mCmdClient = null;
            }
        });

        mCmdClient.connect(address);
        WindowLoading.startLoading(context, R.string.push_record_connecting);
    }

    private final Runnable mRunnableDisplay = new Runnable() {
        @Override
        public void run() {
            mPlayerStatus.setVisibility(View.GONE);
        }
    };

    private final PushDecoder.HandleDecoder mHandleDecoder = new PushDecoder.HandleDecoder() {

        @Override
        public void onClientConnect(String addr, int port) {
            String log = String.format("%s:%d连接成功", addr, port);
            showTips(log);
            writeTxtLog(log);
        }

        @Override
        public void onClientDisconnect(String addr, int port) {
            String log = String.format("%s:%d断开", addr, port);
            showTips(log);
            writeTxtLog(log);
        }

        @Override
        public void onClientError(PushBase.Error error) {
            switch (error) {
                case ERROR_WIFIP2P: {
                    showTips(R.string.push_error_wifip2p);
                    writeTxtLog(R.string.push_error_wifip2p);

                    finish();
                }
                break;

                case ERROR_DECODER: {
                    showTips(R.string.push_error_decoder);
                    writeTxtLog(R.string.push_error_decoder);

                    finish();
                }
                break;
            }
        }

        @Override
        public void onClientPacket(long packet) {
            if (mPlayerStatus.getVisibility() == View.VISIBLE) {
                BigDecimal a = new BigDecimal(packet);
                String txt = "";

                if (packet > 1000 * 1024) {
                    BigDecimal b = new BigDecimal(1000 * 1024);
                    BigDecimal c = a.divide(b);
                    txt = String.format("%.2fM", c.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                } else {
                    if (packet > 1024) {
                        BigDecimal b = new BigDecimal(1024);
                        BigDecimal c = a.divide(b);
                        txt = String.format("%.2fK", c.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                    } else {
                        txt = String.format("%dB", packet);
                    }
                }

                mPlayerPacket.setText(txt);
            }
        }

        @Override
        public void onClientSpeed(long speed, long ms) {
            if (mPlayerStatus.getVisibility() == View.VISIBLE) {
                BigDecimal a = new BigDecimal(speed);
                String txt = "";

                if (speed > 1000 * 1024) {
                    BigDecimal b = new BigDecimal(1000 * 1024);
                    BigDecimal c = a.divide(b);
                    txt = String.format("%.2fM/s", c.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                } else {
                    if (speed > 1024) {
                        BigDecimal b = new BigDecimal(1024);
                        BigDecimal c = a.divide(b);
                        txt = String.format("%.2fK/s", c.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                    } else {
                        txt = String.format("%dB/s", speed);
                    }
                }

                mPlayerSpeed.setText(txt);
            }
        }

        @Override
        public void onClientTimer(long timer) {
            if (mPlayerStatus.getVisibility() == View.VISIBLE) {
                BigDecimal a = new BigDecimal(timer);
                BigDecimal b = new BigDecimal(1000);
                BigDecimal c = a.divide(b);

                long second = c.longValue();
                String text = String.format("%02d:%02d", second / 60, second % 60);
                mPlayerTimer.setText(text);
            }
        }

        @Override
        public void onClientP2PQuery(boolean stop) {
            if (stop)
                writeTxtLog("wifi p2p query stop");
            else
                writeTxtLog("wifi p2p query start");
        }

        @Override
        public void onClientP2PDevices(ArrayList<WifiP2pDevice> devices) {
            for (WifiP2pDevice device : devices) {
                String log = String.format("%s  %s", device.deviceName, device.deviceAddress);
                writeTxtLog(log);
            }
        }

        @Override
        public void onClientP2PConnected(final WifiP2pInfo info) {
            final Runnable runnable = new Runnable() {

                @Override
                public void run() {
                    InetAddress address = info.groupOwnerAddress;

                    if (address != null) {
                        final StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(address.getHostName());
                        stringBuilder.append(",");
                        stringBuilder.append(address.getHostAddress());
                        stringBuilder.append(",");
                        stringBuilder.append(info.groupFormed);
                        stringBuilder.append(",");
                        stringBuilder.append(info.isGroupOwner);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                writeTxtLog(stringBuilder.toString());
                            }
                        });
                    }
                }
            };

            Thread thread = new Thread(runnable);
            thread.start();
        }
    };

    private void lockScreen(boolean lock) {
        if (lock)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void writeTxtLog(String content) {
        tvLog.append("\n");
        tvLog.append(content);
    }

    private void writeTxtLog(int resId) {
        String txt = getString(resId);
        tvLog.append("\n");
        tvLog.append(txt);
    }

    private void showTips(String content) {
        Toast toast = Toast.makeText(this, content, Toast.LENGTH_LONG);
        toast.show();
    }

    private void showTips(int resId) {
        Toast toast = Toast.makeText(this, resId, Toast.LENGTH_LONG);
        toast.show();
    }

    private void showTips(int resId, Object... formatArgs) {
        String content = getString(resId, formatArgs);
        Toast toast = Toast.makeText(this, content, Toast.LENGTH_LONG);
        toast.show();
    }
}
