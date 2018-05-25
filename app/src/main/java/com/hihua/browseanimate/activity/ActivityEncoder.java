package com.hihua.browseanimate.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.hihua.browseanimate.R;
import com.hihua.browseanimate.push.PushBase;
import com.hihua.browseanimate.push.PushEncoder;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Created by hihua on 17/11/27.
 */

public class ActivityEncoder extends Activity {
    private Button btListen = null;
    private Button btDecoder = null;
    private Button btSetting = null;
    private TextView tvLog = null;
    private PushEncoder mPushEncoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPushEncoder = new PushEncoder(this, mHandleSocket);

        setContentView(R.layout.activity_encoder_test);
        setControl();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mPushEncoder.close();
    }

    private void setControl() {
        btListen = (Button) findViewById(R.id.bt_listen);
        btListen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String btText = btListen.getText().toString();
                String stText = getText(R.string.push_button_start_listen).toString();

                if (btText.equals(stText)) {
                    final AlertDialog.Builder dialog = new AlertDialog.Builder(ActivityEncoder.this);
                    dialog.setTitle(R.string.push_select_method).setMessage(R.string.push_select_network);
                    dialog.setPositiveButton(R.string.push_select_wifip2p, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mPushEncoder.startListen();
                            mPushEncoder.startQuery();
                            btListen.setText(R.string.push_button_close_listen);
                            dialog.dismiss();
                        }
                    });
                    /*
                    dialog.setNeutralButton(R.string.push_select_wifi, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mPushEncoder.startListen();
                            btListen.setText(R.string.push_button_close_listen);
                            dialog.dismiss();
                        }
                    });
                    */
                    dialog.setNegativeButton(R.string.push_select_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                } else {
                    mPushEncoder.close();
                    btListen.setText(stText);
                }
            }
        });

        btDecoder = (Button) findViewById(R.id.bt_decoder);
        btDecoder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ActivityEncoder.this, ActivityDecoder.class);
                startActivity(intent);
            }
        });

        btSetting = (Button) findViewById(R.id.bt_setting);
        btSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(ActivityEncoder.this, ActivitySetting.class);
                //startActivity(intent);
            }
        });

        tvLog = (TextView) findViewById(R.id.tv_log);
    }

    PushEncoder.HandleSocket mHandleSocket = new PushEncoder.HandleSocket() {
        @Override
        public void onServerListen(String address, int port) {
            address = getAddress();

            String log = String.format("%s:%d开始监听", address, port);
            writeTxtLog(log);
        }

        @Override
        public void onServerAccept(String address) {
            String log = String.format("%s已连接", address);
            writeTxtLog(log);
        }

        @Override
        public void onServerDisconnect(String address) {
            if (address != null && address.length() > 0) {
                String log = String.format("%s已断开", address);
                writeTxtLog(log);
            }
        }

        @Override
        public void onServerP2PQuery(boolean stop) {
            if (stop)
                writeTxtLog("wifi p2p query stop");
            else
                writeTxtLog("wifi p2p query...");
        }

        @Override
        public void onServerP2PDevices(ArrayList<WifiP2pDevice> devices, String address) {
            for (WifiP2pDevice device : devices) {
                String log = String.format("%s  %s", device.deviceName, device.deviceAddress);
                if (address != null && address.equals(device.deviceAddress))
                    log = String.format("%s  %s connect", device.deviceName, device.deviceAddress);

                writeTxtLog(log);
            }
        }

        @Override
        public void onServerP2PConnected(final WifiP2pInfo info) {
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

        @Override
        public void onServerError(PushBase.Error error) {
            switch (error) {
                case ERROR_WIFIP2P: {
                    mPushEncoder.close();
                    btListen.setText(R.string.push_button_start_listen);

                    showTips(R.string.push_error_wifip2p);
                    writeTxtLog(R.string.push_error_wifip2p);
                }
                break;

                case ERROR_DEIVCE: {
                    mPushEncoder.close();
                    btListen.setText(R.string.push_button_start_listen);

                    showTips(R.string.push_error_device);
                    writeTxtLog(R.string.push_error_device);
                }
                break;

                case ERROR_NO_FOUND_DEVICE: {
                    mPushEncoder.close();
                    btListen.setText(R.string.push_button_start_listen);

                    showTips(R.string.push_error_no_found_device);
                    writeTxtLog(R.string.push_error_no_found_device);
                }
                break;

                case ERROR_ENCODER: {
                    mPushEncoder.close();
                    btListen.setText(R.string.push_button_start_listen);

                    showTips(R.string.push_error_encoder);
                    writeTxtLog(R.string.push_error_encoder);
                }
                break;

                case ERROR_LISTEN: {
                    mPushEncoder.close();
                    btListen.setText(R.string.push_button_start_listen);

                    showTips(R.string.push_error_listen);
                    writeTxtLog(R.string.push_error_listen);
                }
                break;
            }
        }
    };

    private String getAddress() {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            if (enumeration != null) {
                while (enumeration.hasMoreElements()) {
                    NetworkInterface networkInterface = enumeration.nextElement();
                    if (networkInterface != null) {
                        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                        if (inetAddresses != null) {
                            while (inetAddresses.hasMoreElements()) {
                                InetAddress address = inetAddresses.nextElement();
                                if (!address.isLoopbackAddress()) {
                                    stringBuilder.append(address.getHostAddress());
                                    stringBuilder.append("\n");
                                }
                            }
                        }
                    }
                }
            }

            if (stringBuilder.length() > 0)
                stringBuilder.deleteCharAt(0);
        } catch (SocketException e) {

        }

        return stringBuilder.toString();
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
