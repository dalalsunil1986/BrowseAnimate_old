package com.hihua.browseanimate.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.NumberKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.hihua.browseanimate.R;
import com.hihua.browseanimate.config.ConfigPush;
import com.hihua.browseanimate.config.ConfigRecord;
import com.hihua.browseanimate.util.UtilLog;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ActivitySetting extends Activity {
    private ActionRecord actionRecord;
    private ActionPush actionPush;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        actionRecord = new ActionRecord();
        actionPush = new ActionPush();

        setPager();
        setControl();
    }

    private void setControl() {
        ViewPager vpMain = (ViewPager) findViewById(R.id.vp_setting);
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
                        View view = actionRecord.getView();
                        container.addView(view);
                        return view;
                    }

                    case 1: {
                        View view = actionPush.getView();
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
                        View view = actionRecord.getView();
                        container.removeView(view);
                        return;
                    }

                    case 1: {
                        View view = actionPush.getView();
                        container.removeView(view);
                        return;
                    }
                }

                super.destroyItem(container, position, object);
            }
        });
    }

    private void setPager() {
        actionRecord.initView();
        actionPush.initView();
    }

    private void tip(int id) {
        Toast.makeText(this, id, Toast.LENGTH_LONG).show();
    }

    private int getValue(EditText editText, int value) throws Exception {
        Editable editable = editText.getText();
        if (editable.length() > 0) {
            String s = editable.toString();
            if (s != null && s.length() > 0) {
                try {
                    int v = Integer.parseInt(s);
                    if (v != value)
                        return v;
                } catch (Exception e) {
                    new Exception(e);
                }
            }
        }

        return 0;
    }

    private int getValue(Spinner spinner, int value) throws Exception {
        Object object = spinner.getSelectedItem();
        if (object != null) {
            String s = String.valueOf(object);
            if (s != null && s.length() > 0) {
                try {
                    int v = Integer.parseInt(s);
                    if (v != value)
                        return v;
                } catch (Exception e) {
                    new Exception(e);
                }
            }
        }

        return 0;
    }

    private String getValue(Spinner spinner) {
        Object object = spinner.getSelectedItem();
        if (object != null) {
            String s = String.valueOf(object);
            return s;
        }

        return null;
    }

    class ActionRecord {
        private View view;
        private EditText etAudioBitrate;
        private RadioButton rbAudioChannelsMono;
        private RadioButton rbAudioChannelsStereo;
        private Spinner spAudioSamplerate;
        private Spinner spAudioCodec;
        private EditText etVideoBitrate;
        private EditText etVideoFrameWidth;
        private EditText etVideoFrameHeight;
        private EditText etVideoFrameRate;
        private Spinner spVideoCodec;
        private RadioButton rbFileFormatMpeg4;
        private RadioButton rbFileFormatDefault;
        private Spinner spOrientation;
        private Button btConfirm;
        private Button btCancel;

        private ConfigRecord mConfigRecord;

        public void initView() {
            LayoutInflater inflater = getLayoutInflater();
            view = inflater.inflate(R.layout.layout_setting_record, null);

            etAudioBitrate = (EditText) view.findViewById(R.id.et_audio_bitrate);
            rbAudioChannelsMono = (RadioButton) view.findViewById(R.id.rb_audio_channels_mono);
            rbAudioChannelsStereo = (RadioButton) view.findViewById(R.id.rb_audio_channels_stereo);
            spAudioSamplerate = (Spinner) view.findViewById(R.id.sp_audio_samplerate);
            spAudioCodec = (Spinner) view.findViewById(R.id.sp_audio_codec);
            etVideoBitrate = (EditText) view.findViewById(R.id.et_video_bitrate);
            etVideoFrameWidth = (EditText) view.findViewById(R.id.et_video_frame_width);
            etVideoFrameHeight = (EditText) view.findViewById(R.id.et_video_frame_height);
            etVideoFrameRate = (EditText) view.findViewById(R.id.et_video_frame_rate);
            spVideoCodec = (Spinner) view.findViewById(R.id.sp_video_codec);
            rbFileFormatMpeg4 = (RadioButton) view.findViewById(R.id.rb_file_format_mpeg4);
            rbFileFormatDefault = (RadioButton) view.findViewById(R.id.rb_file_format_default);
            spOrientation = (Spinner) view.findViewById(R.id.sp_orientation);

            btConfirm = (Button) view.findViewById(R.id.bt_setting_confirm);
            btCancel = (Button) view.findViewById(R.id.bt_setting_cancel);

            btConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setSetting();
                    finish();
                }
            });

            btCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });

            getSetting();
        }

        public View getView() {
            return this.view;
        }

        private void getSetting() {
            mConfigRecord = ConfigRecord.getProfile(ActivitySetting.this);
            if (mConfigRecord == null)
                mConfigRecord = ConfigRecord.initProfile();

            getSetting(mConfigRecord);
        }

        private void getSetting(ConfigRecord configRecord) {
            if (configRecord != null) {
                int audioBitRate = configRecord.getAudioBitRate();
                int audioChannels = configRecord.getAudioChannels();
                int audioSampleRate = configRecord.getAudioSampleRate();
                int audioCodec = configRecord.getAudioCodec();
                int videoBitRate = configRecord.getVideoBitRate();
                int videoFrameWidth = configRecord.getVideoFrameWidth();
                int videoFrameHeight = configRecord.getVideoFrameHeight();
                int videoFrameRate = configRecord.getVideoFrameRate();
                int videoCodec = configRecord.getVideoCodec();
                int fileFormat = configRecord.getFileFormat();
                int orientationHint = configRecord.getOrientationHint();

                etAudioBitrate.setText(String.valueOf(audioBitRate));

                if (audioChannels == 1) {
                    rbAudioChannelsMono.setChecked(true);
                    rbAudioChannelsStereo.setChecked(false);
                } else {
                    rbAudioChannelsMono.setChecked(false);
                    rbAudioChannelsStereo.setChecked(true);
                }

                switch (audioSampleRate) {
                    case 22050:
                        spAudioSamplerate.setSelection(0);
                        break;

                    case 44100:
                        spAudioSamplerate.setSelection(1);
                        break;

                    case 48000:
                        spAudioSamplerate.setSelection(2);
                        break;
                }

                switch (audioCodec) {
                    case MediaRecorder.AudioEncoder.AAC:
                        spAudioCodec.setSelection(0);
                        break;

                    case MediaRecorder.AudioEncoder.AAC_ELD:
                        spAudioCodec.setSelection(1);
                        break;

                    case MediaRecorder.AudioEncoder.HE_AAC:
                        spAudioCodec.setSelection(2);
                        break;

                    default:
                        spAudioCodec.setSelection(3);
                        break;
                }

                etVideoBitrate.setText(String.valueOf(videoBitRate));
                etVideoFrameWidth.setText(String.valueOf(videoFrameWidth));
                etVideoFrameHeight.setText(String.valueOf(videoFrameHeight));
                etVideoFrameRate.setText(String.valueOf(videoFrameRate));

                switch (videoCodec) {
                    case MediaRecorder.VideoEncoder.H263:
                        spVideoCodec.setSelection(0);
                        break;

                    case MediaRecorder.VideoEncoder.H264:
                        spVideoCodec.setSelection(1);
                        break;

                    case MediaRecorder.VideoEncoder.MPEG_4_SP:
                        spVideoCodec.setSelection(2);
                        break;

                    default:
                        spVideoCodec.setSelection(3);
                        break;
                }

                if (fileFormat == MediaRecorder.OutputFormat.MPEG_4) {
                    rbFileFormatMpeg4.setChecked(true);
                    rbFileFormatDefault.setChecked(false);
                } else {
                    rbFileFormatMpeg4.setChecked(false);
                    rbFileFormatDefault.setChecked(true);
                }

                switch (orientationHint) {
                    case 90:
                        spOrientation.setSelection(1);
                        break;

                    case 180:
                        spOrientation.setSelection(2);
                        break;

                    case 270:
                        spOrientation.setSelection(3);
                        break;

                    default:
                        spOrientation.setSelection(0);
                        break;
                }
            }
        }

        private void setSetting() {
            ConfigRecord configRecord = ConfigRecord.getProfile(ActivitySetting.this);
            if (configRecord == null)
                configRecord = ConfigRecord.initProfile();

            if (configRecord != null && setSetting(configRecord)) {
                ConfigRecord.saveProfile(ActivitySetting.this, configRecord);
                tip(R.string.setting_save_success);
            }
        }

        private boolean setSetting(ConfigRecord configRecord) {
            boolean change = false;

            try {
                int audioBitRate = getValue(etAudioBitrate, configRecord.getAudioBitRate());
                if (audioBitRate > 0) {
                    configRecord.setAudioBitRate(audioBitRate);
                    change = true;
                }

                int audioChannels = configRecord.getAudioChannels();
                if (rbAudioChannelsMono.isChecked()) {
                    if (audioChannels != 1) {
                        configRecord.setAudioChannels(1);
                        change = true;
                    }
                } else {
                    if (audioChannels != 2) {
                        configRecord.setAudioChannels(2);
                        change = true;
                    }
                }

                int audioSampleRate = getValue(spAudioSamplerate, configRecord.getAudioSampleRate());
                if (audioSampleRate > 0) {
                    configRecord.setAudioSampleRate(audioSampleRate);
                    change = true;
                }

                String audioCodec = getValue(spAudioCodec);
                if (audioCodec != null) {
                    if (audioCodec.equals("AAC") && configRecord.getAudioCodec() != MediaRecorder.AudioEncoder.AAC) {
                        configRecord.setAudioCodec(MediaRecorder.AudioEncoder.AAC);
                        change = true;
                    }

                    if (audioCodec.equals("AAC_ELD") && configRecord.getAudioCodec() != MediaRecorder.AudioEncoder.AAC_ELD) {
                        configRecord.setAudioCodec(MediaRecorder.AudioEncoder.AAC_ELD);
                        change = true;
                    }

                    if (audioCodec.equals("HE_AAC") && configRecord.getAudioCodec() != MediaRecorder.AudioEncoder.HE_AAC) {
                        configRecord.setAudioCodec(MediaRecorder.AudioEncoder.HE_AAC);
                        change = true;
                    }

                    if (audioCodec.equals("Default") && configRecord.getAudioCodec() != MediaRecorder.AudioEncoder.DEFAULT) {
                        configRecord.setAudioCodec(MediaRecorder.AudioEncoder.DEFAULT);
                        change = true;
                    }
                }

                int videoBitRate = getValue(etVideoBitrate, configRecord.getVideoBitRate());
                if (videoBitRate > 0) {
                    configRecord.setVideoBitRate(videoBitRate);
                    change = true;
                }

                int videoFrameWidth = getValue(etVideoFrameWidth, configRecord.getVideoFrameWidth());
                if (videoFrameWidth > 0) {
                    configRecord.setVideoFrameWidth(videoFrameWidth);
                    change = true;
                }

                int videoFrameHeight = getValue(etVideoFrameHeight, configRecord.getVideoFrameHeight());
                if (videoFrameHeight > 0) {
                    configRecord.setVideoFrameHeight(videoFrameHeight);
                    change = true;
                }

                int videoFrameRate = getValue(etVideoFrameRate, configRecord.getVideoFrameRate());
                if (videoFrameRate > 0) {
                    configRecord.setVideoFrameRate(videoFrameRate);
                    change = true;
                }

                String videoCodec = getValue(spVideoCodec);
                if (videoCodec != null) {
                    if (videoCodec.equals("H263") && configRecord.getVideoCodec() != MediaRecorder.VideoEncoder.H263) {
                        configRecord.setVideoCodec(MediaRecorder.VideoEncoder.H263);
                        change = true;
                    }

                    if (videoCodec.equals("H264") && configRecord.getVideoCodec() != MediaRecorder.VideoEncoder.H264) {
                        configRecord.setVideoCodec(MediaRecorder.VideoEncoder.H264);
                        change = true;
                    }

                    if (videoCodec.equals("MP4") && configRecord.getVideoCodec() != MediaRecorder.VideoEncoder.MPEG_4_SP) {
                        configRecord.setVideoCodec(MediaRecorder.VideoEncoder.MPEG_4_SP);
                        change = true;
                    }

                    if (videoCodec.equals("Default") && configRecord.getVideoCodec() != MediaRecorder.VideoEncoder.DEFAULT) {
                        configRecord.setVideoCodec(MediaRecorder.VideoEncoder.DEFAULT);
                        change = true;
                    }
                }

                int fileFormat = configRecord.getFileFormat();
                if (rbFileFormatMpeg4.isChecked()) {
                    if (fileFormat != MediaRecorder.OutputFormat.MPEG_4) {
                        configRecord.setFileFormat(MediaRecorder.OutputFormat.MPEG_4);
                        change = true;
                    }
                } else {
                    if (fileFormat != MediaRecorder.OutputFormat.DEFAULT) {
                        configRecord.setFileFormat(MediaRecorder.OutputFormat.DEFAULT);
                        change = true;
                    }
                }

                String orientationHint = getValue(spOrientation);
                if (orientationHint != null && !orientationHint.equals(String.valueOf(configRecord.getOrientationHint()))) {
                    configRecord.setOrientationHint(Integer.parseInt(orientationHint));
                    change = true;
                }

                return change;
            } catch (Exception e) {
                UtilLog.writeError(getClass(), e);
                tip(R.string.setting_save_fail);
                return false;
            }
        }
    }

    class ActionPush {
        private View view;
        private TextView tvDeviceAddress;
        private Spinner spDeviceAddress;
        private EditText etPortData;
        private EditText etPortCmd;
        private Spinner spPushFormat;
        private EditText etAudioBitrate;
        private RadioButton rbAudioChannelsMono;
        private RadioButton rbAudioChannelsStereo;
        private Spinner spAudioSamplerate;
        private Spinner spAudioName;
        private EditText etVideoBitrate;
        private EditText etVideoFrameWidth;
        private EditText etVideoFrameHeight;
        private EditText etVideoFrameRate;
        private Spinner spVideoName;
        private EditText etVideoPriv;
        private Button btConfirm;
        private Button btCancel;

        private ConfigPush mConfigPush;

        public void initView() {
            LayoutInflater inflater = getLayoutInflater();
            view = inflater.inflate(R.layout.layout_setting_push, null);

            tvDeviceAddress = (TextView) view.findViewById(R.id.tv_device_address);
            spDeviceAddress = (Spinner) view.findViewById(R.id.sp_device_address);
            etPortData = (EditText) view.findViewById(R.id.et_port_data);
            etPortCmd = (EditText) view.findViewById(R.id.et_port_cmd);
            spPushFormat = (Spinner) view.findViewById(R.id.sp_push_format);
            spAudioName = (Spinner) view.findViewById(R.id.sp_audio_name);
            etAudioBitrate = (EditText) view.findViewById(R.id.et_audio_bitrate);
            rbAudioChannelsMono = (RadioButton) view.findViewById(R.id.rb_audio_channels_mono);
            rbAudioChannelsStereo = (RadioButton) view.findViewById(R.id.rb_audio_channels_stereo);
            spAudioSamplerate = (Spinner) view.findViewById(R.id.sp_audio_samplerate);
            spVideoName = (Spinner) view.findViewById(R.id.sp_video_name);
            etVideoBitrate = (EditText) view.findViewById(R.id.et_video_bitrate);
            etVideoFrameWidth = (EditText) view.findViewById(R.id.et_video_frame_width);
            etVideoFrameHeight = (EditText) view.findViewById(R.id.et_video_frame_height);
            etVideoFrameRate = (EditText) view.findViewById(R.id.et_video_frame_rate);
            etVideoPriv = (EditText) view.findViewById(R.id.et_video_priv);

            btConfirm = (Button) view.findViewById(R.id.bt_setting_confirm);
            btCancel = (Button) view.findViewById(R.id.bt_setting_cancel);

            tvDeviceAddress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(ActivitySetting.this);
                    dialog.setTitle(R.string.setting_device_select_title);
                    dialog.setMessage(R.string.setting_device_select_message);
                    dialog.setPositiveButton(R.string.setting_device_select_add, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            final EditText editText = new EditText(ActivitySetting.this);
                            editText.setFilters(new InputFilter[] { new NumberKeyListener() {

                                @Override
                                public int getInputType() {
                                    return InputType.TYPE_CLASS_TEXT;
                                }

                                @Override
                                protected char[] getAcceptedChars() {
                                    return "ABCDEFabcdef0123456789".toCharArray();
                                }
                            }});

                            AlertDialog.Builder input = new AlertDialog.Builder(ActivitySetting.this);
                            input.setTitle(R.string.setting_device_input_title);
                            input.setView(editText);
                            input.setPositiveButton(R.string.setting_confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    Editable editable = editText.getText();
                                    if (editable.length() > 0) {
                                        String txt = editable.toString();
                                        txt = txt.trim();
                                        txt = txt.toUpperCase();
                                        txt = txt.replace(":", "");

                                        if (txt.length() > 0) {
                                            int length = txt.length();
                                            int index = 0;

                                            StringBuilder addr = new StringBuilder();
                                            while (index < length) {
                                                if (index > 0 && index % 2 == 0)
                                                    addr.append(":");

                                                String s = txt.substring(index, index + 1);
                                                addr.append(s);

                                                index++;
                                            }

                                            txt = addr.toString();
                                            boolean found = false;

                                            Set<String> deviceAddress = mConfigPush.getDeviceAddress();
                                            if (deviceAddress != null) {
                                                for (String device : deviceAddress) {
                                                    if (device.equalsIgnoreCase(txt)) {
                                                        found = true;
                                                        break;
                                                    }
                                                }
                                            }

                                            if (found)
                                                tip(R.string.setting_device_same);
                                            else {
                                                if (deviceAddress == null)
                                                    deviceAddress = new HashSet<String>();

                                                deviceAddress.add(txt);
                                                mConfigPush.setDeviceAddress(deviceAddress);

                                                refreshDeviceAddress(mConfigPush);
                                            }
                                        }
                                    }
                                }
                            });
                            input.setNeutralButton(R.string.setting_cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });

                            input.show();
                        }
                    });
                    dialog.setNeutralButton(R.string.setting_device_select_del, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            int position = spDeviceAddress.getSelectedItemPosition();
                            if (position > -1) {
                                Set<String> address = new HashSet<String>();

                                Set<String> deviceAddress = mConfigPush.getDeviceAddress();
                                if (deviceAddress != null) {
                                    int index = 0;

                                    for (String device : deviceAddress) {
                                        if (index != position)
                                            address.add(device);

                                        index++;
                                    }

                                    mConfigPush.setDeviceAddress(address);
                                    refreshDeviceAddress(mConfigPush);
                                }
                            }
                        }
                    });
                    dialog.setNegativeButton(R.string.setting_device_select_use, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            int position = spDeviceAddress.getSelectedItemPosition();
                            if (position > -1) {
                                Set<String> deviceAddress = mConfigPush.getDeviceAddress();
                                if (deviceAddress != null) {
                                    int index = 0;

                                    for (String device : deviceAddress) {
                                        if (index == position) {
                                            mConfigPush.setAddress(device);
                                            refreshDeviceAddress(mConfigPush);
                                            break;
                                        }

                                        index++;
                                    }
                                }
                            }
                        }
                    });

                    dialog.show();
                }
            });

            btConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (setSetting(mConfigPush)) {
                        ConfigPush.saveProfile(ActivitySetting.this, mConfigPush);
                        tip(R.string.setting_save_success);
                        finish();
                    }
                }
            });

            btCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });

            getSetting();
        }

        public View getView() {
            return this.view;
        }

        private void getSetting() {
            mConfigPush = ConfigPush.getProfile(ActivitySetting.this);
            getSetting(mConfigPush);
        }

        private void refreshDeviceAddress(ConfigPush configPush) {
            final Set<String> deviceAddress = configPush.getDeviceAddress();
            String address = configPush.getAddress();

            if (deviceAddress != null) {
                List<String> devices = new ArrayList<String>();
                for (String device : deviceAddress) {
                    if (address != null && address.length() > 0) {
                        if (device.equals(address)) {
                            String use = getString(R.string.setting_device_use);
                            device = String.format("%s%s", device, use);
                        }
                    }

                    devices.add(device);
                }

                ArrayAdapter adapter = new ArrayAdapter<String>(ActivitySetting.this, android.R.layout.simple_spinner_item, devices);
                spDeviceAddress.setAdapter(adapter);

                spDeviceAddress.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
            }
        }

        private void getSetting(ConfigPush configPush) {
            if (configPush != null) {
                String format = configPush.getFormat();
                int portData = configPush.getPortData();
                int portCmd = configPush.getPortCmd();

                ConfigPush.ConfigAudio configAudio = configPush.getConfigAudio();
                ConfigPush.ConfigVideo configVideo = configPush.getConfigVideo();

                String audioName = configAudio.getName();
                int audioBitRate = configAudio.getBitrate();
                int audioChannels = configAudio.getChannels();
                int audioSampleRate = configAudio.getSamplerate();
                String videoName = configVideo.getName();
                int videoBitRate = configVideo.getBitrate();
                int videoFrameWidth = configVideo.getWidth();
                int videoFrameHeight = configVideo.getHeight();
                int videoFrameRate = configVideo.getFramerate();
                JSONObject priv = configVideo.getPriv();

                refreshDeviceAddress(configPush);

                etPortData.setText(String.valueOf(portData));
                etPortCmd.setText(String.valueOf(portCmd));

                String[] array = getResources().getStringArray(R.array.setting_push_format_array);

                if (array != null) {
                    int index = 0;
                    for (String s : array) {
                        if (s.equals(format))
                            break;

                        index++;
                    }

                    spPushFormat.setSelection(index);

                    spPushFormat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            String[] array = getResources().getStringArray(R.array.setting_push_format_array);
                            mConfigPush.setFormat(array[position]);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });
                }

                array = getResources().getStringArray(R.array.setting_audio_name_array);

                if (array != null) {
                    int index = 0;
                    for (String s : array) {
                        if (s.equals(audioName))
                            break;

                        index++;
                    }

                    spAudioName.setSelection(index);

                    spAudioName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            String[] array = getResources().getStringArray(R.array.setting_audio_name_array);
                            ConfigPush.ConfigAudio configAudio = mConfigPush.getConfigAudio();
                            configAudio.setName(array[position]);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });
                }

                etAudioBitrate.setText(String.valueOf(audioBitRate));

                if (audioChannels == 1) {
                    rbAudioChannelsMono.setChecked(true);
                    rbAudioChannelsStereo.setChecked(false);
                } else {
                    rbAudioChannelsMono.setChecked(false);
                    rbAudioChannelsStereo.setChecked(true);
                }

                switch (audioSampleRate) {
                    case 22050:
                        spAudioSamplerate.setSelection(0);
                        break;

                    case 44100:
                        spAudioSamplerate.setSelection(1);
                        break;

                    case 48000:
                        spAudioSamplerate.setSelection(2);
                        break;
                }

                array = getResources().getStringArray(R.array.setting_video_name_array);

                if (array != null) {
                    int index = 0;
                    for (String s : array) {
                        if (s.equals(videoName))
                            break;

                        index++;
                    }

                    spVideoName.setSelection(index);

                    spVideoName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            String[] array = getResources().getStringArray(R.array.setting_video_name_array);
                            ConfigPush.ConfigVideo configVideo = mConfigPush.getConfigVideo();
                            configVideo.setName(array[position]);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });
                }

                etVideoBitrate.setText(String.valueOf(videoBitRate));
                etVideoFrameWidth.setText(String.valueOf(videoFrameWidth));
                etVideoFrameHeight.setText(String.valueOf(videoFrameHeight));
                etVideoFrameRate.setText(String.valueOf(videoFrameRate));

                if (priv != null)
                    etVideoPriv.setText(priv.toString());
            }
        }

        private boolean setSetting(ConfigPush configPush) {
            try {
                int portData = getValue(etPortData, configPush.getPortData());
                if (portData > 0)
                    configPush.setPortData(portData);

                int portCmd = getValue(etPortCmd, configPush.getPortCmd());
                if (portCmd > 0)
                    configPush.setPortCmd(portCmd);

                ConfigPush.ConfigAudio configAudio = configPush.getConfigAudio();
                ConfigPush.ConfigVideo configVideo = configPush.getConfigVideo();

                int audioBitrate = getValue(etAudioBitrate, configAudio.getBitrate());
                if (audioBitrate > 0)
                    configAudio.setBitrate(audioBitrate);

                int audioChannels = configAudio.getChannels();
                if (rbAudioChannelsMono.isChecked()) {
                    if (audioChannels != 1)
                        configAudio.setChannels(1);
                } else {
                    if (audioChannels != 2)
                        configAudio.setChannels(2);
                }

                int audioSampleRate = getValue(spAudioSamplerate, configAudio.getSamplerate());
                if (audioSampleRate > 0)
                    configAudio.setSamplerate(audioSampleRate);

                int videoBitRate = getValue(etVideoBitrate, configVideo.getBitrate());
                if (videoBitRate > 0)
                    configVideo.setBitrate(videoBitRate);

                int videoFrameWidth = getValue(etVideoFrameWidth, configVideo.getWidth());
                if (videoFrameWidth > 0)
                    configVideo.setWidth(videoFrameWidth);

                int videoFrameHeight = getValue(etVideoFrameHeight, configVideo.getHeight());
                if (videoFrameHeight > 0)
                    configVideo.setHeight(videoFrameHeight);

                int videoFrameRate = getValue(etVideoFrameRate, configVideo.getFramerate());
                if (videoFrameRate > 0)
                    configVideo.setFramerate(videoFrameRate);

                Editable editable = etVideoPriv.getText();
                if (editable != null) {
                    String txt = editable.toString();
                    if (txt != null && txt.length() > 0) {
                        JSONObject priv = new JSONObject(txt);
                        configVideo.setPriv(priv);
                    }
                }

                return true;
            } catch (Exception e) {
                UtilLog.writeError(getClass(), e);
                tip(R.string.setting_save_fail);
                return false;
            }
        }
    }
}
