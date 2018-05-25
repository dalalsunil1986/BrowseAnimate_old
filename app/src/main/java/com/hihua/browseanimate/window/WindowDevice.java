package com.hihua.browseanimate.window;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.p2p.WifiP2pDevice;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.hihua.browseanimate.R;

import java.util.ArrayList;

/**
 * Created by hihua on 18/1/24.
 */

public class WindowDevice {
    private static WindowDevice WindowDevice = null;
    private ListView mLvDevice;
    private PopupWindow mWindow;

    public synchronized static void listDevice(final Context context, final ArrayList<WifiP2pDevice> devices, final HandleDevice handle) {
        if (WindowDevice != null) {
            WindowDevice.stop();
            WindowDevice = null;
        }

        WindowDevice = new WindowDevice();
        WindowDevice.showDevice(context, devices, handle);
    }

    public synchronized static void close() {
        if (WindowDevice != null) {
            WindowDevice.stop();
            WindowDevice = null;
        }
    }

    public void showDevice(final Context context, final ArrayList<WifiP2pDevice> devices, final HandleDevice handle) {
        final LayoutInflater inflater = LayoutInflater.from(context);

        View parent = inflater.inflate(R.layout.window_device, null, false);
        mLvDevice = (ListView) parent.findViewById(R.id.lv_device);

        BaseAdapter adDevice = new BaseAdapter() {
            @Override
            public int getCount() {
                return devices.size();
            }

            @Override
            public Object getItem(int position) {
                return devices.get(position);
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = null;

                if (convertView == null)
                    view = inflater.inflate(R.layout.item_device, null);
                else
                    view = convertView;

                final WifiP2pDevice device = devices.get(position);

                TextView tvDeviceName = (TextView) view.findViewById(R.id.tv_device_name);
                TextView tvDeviceAddress = (TextView) view.findViewById(R.id.tv_device_address);
                TextView tvDeviceStatus = (TextView) view.findViewById(R.id.tv_device_status);

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

                tvDeviceName.setText(device.deviceName);
                tvDeviceAddress.setText(device.deviceAddress);
                tvDeviceStatus.setText(status);

                view.setClickable(true);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handle.onSelectDevice(device);
                    }
                });

                return view;
            }
        };

        mLvDevice.setAdapter(adDevice);

        mWindow = new PopupWindow(parent, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, true);
        mWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));
        mWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                stop();
            }
        });

        mWindow.update();
        mWindow.showAtLocation(parent, Gravity.CENTER, 0, 0);
    }

    public void stop() {
        if (mWindow != null) {
            if (mWindow.isShowing())
                mWindow.dismiss();

            mWindow = null;
        }
    }

    public interface HandleDevice {
        public void onSelectDevice(WifiP2pDevice device);
    }
}
