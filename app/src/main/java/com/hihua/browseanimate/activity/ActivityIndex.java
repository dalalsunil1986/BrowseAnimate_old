package com.hihua.browseanimate.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hihua.browseanimate.R;
import com.hihua.browseanimate.config.ConfigCollect;
import com.hihua.browseanimate.service.ServiceMain;

import java.util.List;

/**
 * Created by hihua on 18/3/16.
 */

public class ActivityIndex extends Activity {
    private ActionBrowse actionBrowse;
    private ActionCamera actionCamera;
    private ServiceConnection serviceConnection;
    private ServiceMain serviceMain;
    private ServiceMain.BinderMain binderMain;
    private Handler handler;
    private boolean recording = false;
    private boolean pushing = false;
    private enum MSG { INIT, STARTRECORD, STOPRECORD, STARTPUSH, STOPPUSH };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        actionBrowse = new ActionBrowse();
        actionCamera = new ActionCamera();
        handler = new Handler(mCallback);

        setPager();
        setControl();
        init();
    }

    @Override
    protected void onDestroy() {
        if (serviceConnection != null)
            unbindService(serviceConnection);

        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && actionBrowse.onKeyBack())
            return false;

        return super.onKeyDown(keyCode, event);
    }

    private void setControl() {
        ViewPager vpMain = (ViewPager) findViewById(R.id.vp_main);
        vpMain.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                switch (position) {
                    case 0: {
                        View view = actionBrowse.getView();
                        container.addView(view);
                        return view;
                    }

                    case 1: {
                        View view = actionCamera.getView();
                        container.addView(view);
                        return view;
                    }
                }

                return super.instantiateItem(container, position);
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                switch (position) {
                    case 0: {
                        View view = actionBrowse.getView();
                        container.removeView(view);
                        return;
                    }

                    case 1: {
                        View view = actionCamera.getView();
                        container.removeView(view);
                        return;
                    }
                }

                super.destroyItem(container, position, object);
            }
        });
    }

    private void setPager() {
        actionBrowse.initView();
        actionCamera.initView();
    }

    private void init() {
        if (getService())
            bindService(MSG.INIT);
    }

    private void doRecord() {
        if (recording) {
            if (binderMain == null) {
                if (getService())
                    bindService(MSG.STOPRECORD);
                else
                    recording = false;
            } else
                doRecord(false);
        } else {
            if (binderMain == null) {
                if (!getService())
                    startService();

                bindService(MSG.STARTRECORD);
            } else {
                doRecord(true);
            }
        }
    }

    private void doRecord(boolean start) {
        if (start) {
            //final boolean success = binderMain.startPreView(ActivityIndex.this, actionCamera.getSurfaceView()) && binderMain.startRecord(ActivityIndex.this, actionCamera.getSurfaceView());
            final boolean success = binderMain.startRecord(ActivityIndex.this, actionCamera.getSurfaceView());

            if (success)
                recording = true;
            else
                recording = false;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (success)
                        Toast.makeText(ActivityIndex.this, R.string.start_record_success, Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(ActivityIndex.this, R.string.start_record_fail, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            binderMain.closeRecord();
            recording = false;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ActivityIndex.this, R.string.close_record_success, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void doPush() {
        if (pushing) {
            if (binderMain == null) {
                if (getService())
                    bindService(MSG.STOPPUSH);
                else
                    pushing = false;
            } else
                doPush(false);
        } else {
            if (binderMain == null) {
                if (!getService())
                    startService();

                bindService(MSG.STARTPUSH);
            } else {
                doPush(true);
            }
        }
    }

    private void doPush(boolean start) {
        if (start) {
            final boolean success = binderMain.startPushListen();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (success) {
                        if (binderMain.startPushQuery()) {
                            pushing = true;
                            Toast.makeText(ActivityIndex.this, R.string.start_push_success, Toast.LENGTH_LONG).show();
                        } else {
                            binderMain.closePush();
                            Toast.makeText(ActivityIndex.this, R.string.push_error_device, Toast.LENGTH_LONG).show();
                        }
                    } else
                        Toast.makeText(ActivityIndex.this, R.string.start_push_fail, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            binderMain.closePush();
            pushing = false;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ActivityIndex.this, R.string.close_push_success, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void hideSoftInput(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private boolean getService() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceInfos = manager.getRunningServices(Integer.MAX_VALUE);
        if (serviceInfos != null) {
            String pkg = getPackageName();
            String serviceName = pkg + "." + "service.ServiceMain";

            for (ActivityManager.RunningServiceInfo serviceInfo : serviceInfos) {
                ComponentName componentName = serviceInfo.service;
                String className = componentName.getClassName();
                if (className.equals(serviceName))
                    return true;
            }
        }

        return false;
    }

    private boolean startService() {
        Intent intent = new Intent(this, ServiceMain.class);

        if (startService(intent) != null)
            return true;
        else
            return false;
    }

    private void bindService(final MSG msg) {
        if (serviceConnection == null) {
            serviceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    ServiceMain.ServiceBinder serviceBinder = (ServiceMain.ServiceBinder) service;
                    serviceMain = serviceBinder.getService();
                    serviceMain.setNotifyService(mHandleService);

                    binderMain = serviceMain.getBinderMain();

                    handler.sendEmptyMessage(msg.ordinal());
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }
            };
        }

        Intent intent = new Intent(this, ServiceMain.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceMain.HandleService mHandleService = new ServiceMain.HandleService() {
        @Override
        public void onServiceDestroy() {
            if (serviceConnection != null)
                unbindService(serviceConnection);

            serviceConnection = null;

            serviceMain = null;
            binderMain = null;
            recording = false;
            pushing = false;
        }

        @Override
        public void onServiceRecord(boolean start) {
            recording = start;
        }

        @Override
        public void onServicePush(boolean start) {
            pushing = start;
        }

        @Override
        public void onServiceTimer(long timer) {
            actionBrowse.setTimer(timer);
        }

        @Override
        public void onServiceError() {
            pushing = false;
        }
    };

    private Handler.Callback mCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG.INIT.ordinal()) {
                serviceMain.setNotifyService(mHandleService);
                pushing = binderMain.getPushing();
                recording = binderMain.getRecording();

                return false;
            }

            if (msg.what == MSG.STARTRECORD.ordinal()) {
                doRecord(true);
                return false;
            }

            if (msg.what == MSG.STOPRECORD.ordinal()) {
                doRecord(false);
                return false;
            }

            if (msg.what == MSG.STARTPUSH.ordinal()) {
                doPush(true);
                return false;
            }

            if (msg.what == MSG.STOPPUSH.ordinal()) {
                doPush(false);
                return false;
            }

            return false;
        }
    };

    class ActionBrowse implements View.OnClickListener {
        private View view;
        private EditText etBrowseUrl;
        private ImageButton btBrowseRefresh;
        private ProgressBar pbWeb;
        private WebView wvWeb;
        private ListView lvCollects;
        private ImageButton ivWebBack;
        private ImageButton ivWebForward;
        private ImageButton ivMenu;
        private TextView tvTimer;
        private List<ConfigCollect> collects;

        public void initView() {
            LayoutInflater inflater = getLayoutInflater();
            view = inflater.inflate(R.layout.layout_main_browse, null);
            etBrowseUrl = (EditText) view.findViewById(R.id.et_browse_url);
            btBrowseRefresh = (ImageButton) view.findViewById(R.id.iv_browse_refresh);
            pbWeb = (ProgressBar) view.findViewById(R.id.pb_web);
            wvWeb = (WebView) view.findViewById(R.id.wv_web);
            lvCollects = (ListView) view.findViewById(R.id.lv_collects);
            ivWebBack = (ImageButton) view.findViewById(R.id.iv_web_back);
            ivWebForward = (ImageButton) view.findViewById(R.id.iv_web_forward);
            ivMenu = (ImageButton) view.findViewById(R.id.iv_menu);
            tvTimer = (TextView) view.findViewById(R.id.tv_timer);

            etBrowseUrl.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        etBrowseUrl.setCursorVisible(true);
                        lvCollects.setVisibility(View.VISIBLE);
                    } else {
                        lvCollects.setVisibility(View.INVISIBLE);
                        hideSoftInput(v);
                    }
                }
            });

            etBrowseUrl.setClickable(true);
            etBrowseUrl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    etBrowseUrl.setCursorVisible(true);
                    lvCollects.setVisibility(View.VISIBLE);
                }
            });

            btBrowseRefresh.setOnClickListener(this);
            ivWebBack.setOnClickListener(this);
            ivWebForward.setOnClickListener(this);
            ivMenu.setOnClickListener(this);

            setWebView();
            setCollect();
        }

        public View getView() {
            return this.view;
        }

        public boolean onKeyBack() {
            if (wvWeb.canGoBack()) {
                wvWeb.goBack();
                return true;
            } else
                return false;
        }

        public void setTimer(long timer) {
            timer = timer / 1000;
            long minute = timer / 60;
            long second = timer % 60;

            String s = String.format("%d:%02d", minute, second);
            tvTimer.setText(s);
        }

        private void setWebView() {
            wvWeb.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    lvCollects.setVisibility(View.INVISIBLE);
                    hideSoftInput(v);
                    etBrowseUrl.setCursorVisible(false);
                    return v.onTouchEvent(event);
                }
            });

            wvWeb.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    etBrowseUrl.setText(url);
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        etBrowseUrl.setText(url);
                        view.loadUrl(url);
                    }

                    return true;
                }
            });

            wvWeb.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    if (newProgress < 100) {
                        if (pbWeb.getVisibility() != View.VISIBLE)
                            pbWeb.setVisibility(View.VISIBLE);

                        pbWeb.setProgress(newProgress);
                    } else
                        pbWeb.setVisibility(View.INVISIBLE);
                }
            });

            WebSettings webSettings = wvWeb.getSettings();
            webSettings.setSupportZoom(true);
            webSettings.setBuiltInZoomControls(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setDisplayZoomControls(false);
            webSettings.setDomStorageEnabled(true);
            webSettings.setAllowFileAccess(true);
            webSettings.setAllowContentAccess(true);
            webSettings.setDatabaseEnabled(true);
            webSettings.setJavaScriptEnabled(true);
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
            webSettings.setLoadWithOverviewMode(true);
        }

        private void webExplore() {
            Editable editable = etBrowseUrl.getText();
            if (editable.length() > 0) {
                String url = editable.toString();
                if (!url.startsWith("http://") && !url.startsWith("https://"))
                    url = "http://" + url;

                wvWeb.loadUrl(url);
            }
        }

        private void setCollect() {
            collects = ConfigCollect.getCollects(ActivityIndex.this);
            if (collects == null) {
                collects = ConfigCollect.initCollect();
                ConfigCollect.saveCollects(ActivityIndex.this, collects);
            }

            lvCollects.setAdapter(baCollect);
        }

        BaseAdapter baCollect = new BaseAdapter() {
            @Override
            public int getCount() {
                return collects != null ? collects.size() : 0;
            }

            @Override
            public Object getItem(int position) {
                return collects != null ? collects.get(position) : null;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                final LayoutInflater inflater = LayoutInflater.from(ActivityIndex.this);

                View view = null;

                if (convertView == null)
                    view = inflater.inflate(R.layout.item_collect, null);
                else
                    view = convertView;

                ConfigCollect configCollect = collects.get(position);
                final String title = configCollect.getTitle();
                final String url = configCollect.getUrl();

                TextView tvTitle = (TextView) view.findViewById(R.id.tv_collect_title);
                TextView tvUrl = (TextView) view.findViewById(R.id.tv_collect_url);
                TextView tvDelete = (TextView) view.findViewById(R.id.tv_collect_delete);

                tvTitle.setText(title);
                tvUrl.setText(url);
                tvDelete.setClickable(true);
                tvDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ActivityIndex.this);
                        builder.setTitle(R.string.collect_delete);
                        builder.setMessage("");
                        builder.setPositiveButton(R.string.collect_confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                collects.remove(position);
                                if (ConfigCollect.saveCollects(ActivityIndex.this, collects)) {
                                    notifyDataSetChanged();
                                    Toast.makeText(ActivityIndex.this, R.string.collect_delete_success, Toast.LENGTH_LONG).show();
                                } else
                                    Toast.makeText(ActivityIndex.this, R.string.collect_delete_fail, Toast.LENGTH_LONG).show();
                            }
                        });

                        builder.setNegativeButton(R.string.collect_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });

                        builder.show();
                    }
                });

                view.setClickable(true);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        lvCollects.setVisibility(View.INVISIBLE);
                        hideSoftInput(etBrowseUrl);
                        etBrowseUrl.setText(url);

                        wvWeb.loadUrl(url);
                    }
                });

                return view;
            }
        };

        @Override
        public void onClick(View view) {
            final Context context = ActivityIndex.this;

            if (view == btBrowseRefresh) {
                webExplore();
                return;
            }

            if (view == ivWebBack) {
                if (wvWeb.canGoBack())
                    wvWeb.goBack();

                return;
            }

            if (view == ivWebForward) {
                if (wvWeb.canGoForward())
                    wvWeb.goForward();

                return;
            }

            if (view == ivMenu) {
                final Dialog dialog = new Dialog(context, R.style.style_main_menu);

                View.OnClickListener onClick = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();

                        final int id = v.getId();
                        switch (id) {
                            case R.id.tv_main_menu_setting: {
                                Intent intent = new Intent(context, ActivitySetting.class);
                                startActivity(intent);
                                return;
                            }

                            case R.id.tv_main_menu_collect: {
                                String title = wvWeb.getTitle();
                                if (title == null || title.trim().length() == 0)
                                    title = getString(R.string.collect_blank);

                                String url = wvWeb.getUrl();
                                if (url != null && url.length() > 0) {
                                    for (ConfigCollect collect : collects) {
                                        if (collect.getUrl().equals(url)) {
                                            Toast.makeText(context, R.string.collect_save_already, Toast.LENGTH_LONG).show();
                                            return;
                                        }
                                    }

                                    ConfigCollect configCollect = new ConfigCollect();
                                    configCollect.setTitle(title);
                                    configCollect.setUrl(url);

                                    collects.add(configCollect);

                                    if (ConfigCollect.saveCollects(ActivityIndex.this, collects))
                                        Toast.makeText(context, R.string.collect_save_success, Toast.LENGTH_LONG).show();
                                    else
                                        Toast.makeText(context, R.string.collect_save_fail, Toast.LENGTH_LONG).show();
                                }

                                break;
                            }

                            case R.id.tv_main_menu_start_record: {
                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        doRecord();
                                    }
                                });

                                thread.start();
                                break;
                            }

                            case R.id.tv_main_menu_stop_record: {
                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        doRecord();
                                    }
                                });

                                thread.start();
                                break;
                            }

                            case R.id.tv_main_menu_start_push: {
                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        doPush();
                                    }
                                });

                                thread.start();
                                break;
                            }

                            case R.id.tv_main_menu_stop_push: {
                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        doPush();
                                    }
                                });

                                thread.start();
                                break;
                            }

                            case R.id.tv_main_menu_entry_push: {
                                Intent intent = new Intent(context, ActivityDecoder.class);
                                startActivity(intent);
                                return;
                            }

                            case R.id.tv_main_menu_exit: {
                                finish();
                                return;
                            }
                        }
                    }
                };

                View root = LayoutInflater.from(context).inflate(R.layout.layout_main_menu, null);
                root.findViewById(R.id.tv_main_menu_setting).setOnClickListener(onClick);
                root.findViewById(R.id.tv_main_menu_collect).setOnClickListener(onClick);

                if (!recording) {
                    root.findViewById(R.id.tv_main_menu_start_record).setVisibility(View.VISIBLE);
                    root.findViewById(R.id.tv_main_menu_stop_record).setVisibility(View.GONE);
                    root.findViewById(R.id.tv_main_menu_start_record).setOnClickListener(onClick);
                } else {
                    root.findViewById(R.id.tv_main_menu_start_record).setVisibility(View.GONE);
                    root.findViewById(R.id.tv_main_menu_stop_record).setVisibility(View.VISIBLE);
                    root.findViewById(R.id.tv_main_menu_stop_record).setOnClickListener(onClick);
                }

                if (!pushing) {
                    root.findViewById(R.id.tv_main_menu_start_push).setVisibility(View.VISIBLE);
                    root.findViewById(R.id.tv_main_menu_stop_push).setVisibility(View.GONE);
                    root.findViewById(R.id.tv_main_menu_start_push).setOnClickListener(onClick);
                } else {
                    root.findViewById(R.id.tv_main_menu_start_push).setVisibility(View.GONE);
                    root.findViewById(R.id.tv_main_menu_stop_push).setVisibility(View.VISIBLE);
                    root.findViewById(R.id.tv_main_menu_stop_push).setOnClickListener(onClick);
                }

                if (recording || pushing)
                    root.findViewById(R.id.tv_main_menu_entry_push).setVisibility(View.GONE);
                else {
                    root.findViewById(R.id.tv_main_menu_entry_push).setVisibility(View.VISIBLE);
                    root.findViewById(R.id.tv_main_menu_entry_push).setOnClickListener(onClick);
                }

                root.findViewById(R.id.tv_main_menu_exit).setOnClickListener(onClick);
                root.findViewById(R.id.tv_main_menu_cancel).setOnClickListener(onClick);

                dialog.setContentView(root);
                Window window = dialog.getWindow();
                window.setGravity(Gravity.BOTTOM);
                WindowManager.LayoutParams lp = window.getAttributes();
                lp.x = 0;
                lp.y = 0;
                lp.width = getResources().getDisplayMetrics().widthPixels;
                root.measure(0, 0);
                lp.height = root.getMeasuredHeight();
                lp.alpha = 9f;

                window.setAttributes(lp);
                dialog.show();

                return;
            }
        }
    }

    class ActionCamera {
        private View view;
        private SurfaceView surfaceView;
        private SurfaceHolder surfaceHolder;

        public void initView() {
            LayoutInflater inflater = getLayoutInflater();
            view = inflater.inflate(R.layout.layout_main_camera, null);
            surfaceView = (SurfaceView) view.findViewById(R.id.sv_record);
        }

        public SurfaceView getSurfaceView() {
            return surfaceView;
        }

        public View getView() {
            return this.view;
        }
    }
}
