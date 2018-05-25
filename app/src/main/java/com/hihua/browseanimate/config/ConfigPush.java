package com.hihua.browseanimate.config;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by hihua on 17/10/26.
 */

public class ConfigPush {
    private Set<String> deviceAddress;
    private String address;
    private int portData;
    private int portCmd;
    private String format;
    private ConfigVideo configVideo;
    private ConfigAudio configAudio;

    public Set<String> getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(Set<String> deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPortData() {
        return portData;
    }

    public void setPortData(int portData) {
        this.portData = portData;
    }

    public int getPortCmd() {
        return portCmd;
    }

    public void setPortCmd(int portCmd) {
        this.portCmd = portCmd;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public ConfigVideo getConfigVideo() {
        return configVideo;
    }

    public void setConfigVideo(ConfigVideo configVideo) {
        this.configVideo = configVideo;
    }

    public ConfigAudio getConfigAudio() {
        return configAudio;
    }

    public void setConfigAudio(ConfigAudio configAudio) {
        this.configAudio = configAudio;
    }

    public static void saveProfile(Context context, ConfigPush configPush) {
        SharedPreferences preferences = context.getSharedPreferences("config_push", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putStringSet("device_address", configPush.getDeviceAddress());
        editor.putString("address", configPush.getAddress());
        editor.putInt("port_data", configPush.getPortData());
        editor.putInt("port_cmd", configPush.getPortCmd());
        editor.putString("format", configPush.getFormat());

        ConfigVideo.saveProfile(editor, configPush.configVideo);
        ConfigAudio.saveProfile(editor, configPush.configAudio);

        editor.commit();
    }

    public static ConfigPush getProfile(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("config_push", Context.MODE_PRIVATE);

        Set<String> deviceAddress = preferences.getStringSet("device_address", null);
        String address = preferences.getString("address", null);
        //String address = preferences.getString("address", "f6:29:81:c8:70:73");//client->vivo
        //String address = preferences.getString("address", "9a:ff:d0:88:40:78");//client->lenovo
        //String address = preferences.getString("address", "3a:aa:3c:17:93:98");//client->i9300
        int portData = preferences.getInt("port_data", 9001);
        int portCmd = preferences.getInt("port_cmd", 9002);
        String format = preferences.getString("format", "flv");

        ConfigAudio configAudio = ConfigAudio.getProfile(context);
        ConfigVideo configVideo = ConfigVideo.getProfile(context);

        ConfigPush configPush = new ConfigPush();
        configPush.setDeviceAddress(deviceAddress);
        configPush.setAddress(address);
        configPush.setPortData(portData);
        configPush.setPortCmd(portCmd);
        configPush.setFormat(format);
        configPush.setConfigAudio(configAudio);
        configPush.setConfigVideo(configVideo);

        return configPush;
    }

    public static String parse(ConfigPush configPush) {
        try {
            JSONObject root = new JSONObject();
            root.put("format", configPush.getFormat());

            ConfigVideo.parse(root, configPush.getConfigVideo());
            ConfigAudio.parse(root, configPush.getConfigAudio());

            return root.toString();
        } catch (JSONException e) {

        }

        return null;
    }

    public static ConfigPush parse(String json) {
        try {
            JSONObject root = new JSONObject(json);

            String format = root.getString("format");

            ConfigVideo configVideo = ConfigVideo.parse(root);
            ConfigAudio configAudio = ConfigAudio.parse(root);

            ConfigPush configPush = new ConfigPush();
            configPush.setFormat(format);
            configPush.setConfigVideo(configVideo);
            configPush.setConfigAudio(configAudio);

            return configPush;
        } catch (JSONException e) {
            return null;
        }
    }

    public static class ConfigVideo {
        private String name;
        private int bitrate;
        private int width;
        private int height;
        private int framerate;
        private JSONObject priv;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getBitrate() {
            return bitrate;
        }

        public void setBitrate(int bitrate) {
            this.bitrate = bitrate;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getFramerate() {
            return framerate;
        }

        public void setFramerate(int framerate) {
            this.framerate = framerate;
        }

        public JSONObject getPriv() {
            return priv;
        }

        public void setPriv(JSONObject priv) {
            this.priv = priv;
        }

        public void putPriv(String key, String value) {
            if (priv == null)
                priv = new JSONObject();

            try {
                priv.put(key, value);
            } catch (JSONException e) {

            }
        }

        private static JSONObject parsePriv(String json) {
            if (json != null) {
                try {
                    JSONObject priv = new JSONObject(json);
                    return priv;
                } catch (JSONException e) {

                }
            }

            return null;
        }

        private static String parsePriv(JSONObject priv) {
            if (priv != null)
                return priv.toString();

            return null;
        }

        public static void parse(JSONObject root, ConfigVideo configVideo) {
            if (configVideo != null) {
                try {
                    JSONObject video = new JSONObject();
                    video.put("name", configVideo.getName());
                    video.put("bitrate", configVideo.getBitrate());
                    video.put("width", configVideo.getWidth());
                    video.put("height", configVideo.getHeight());
                    video.put("framerate", configVideo.getFramerate());
                    video.put("priv", configVideo.getPriv());

                    root.put("video", video);
                } catch (JSONException e) {

                }
            }
        }

        public static ConfigVideo parse(JSONObject root) {
            try {
                String name = root.has("name") ? root.getString("name") : null;
                int bitrate = root.has("bitrate") ? root.getInt("bitrate") : 0;
                int width = root.has("width") ? root.getInt("width") : 0;
                int height = root.has("height") ? root.getInt("height") : 0;
                int framerate = root.has("framerate") ? root.getInt("framerate") : 0;
                String priv = root.has("priv") ? root.getString("priv") : null;

                ConfigVideo configVideo = new ConfigVideo();
                configVideo.setName(name);
                configVideo.setWidth(width);
                configVideo.setHeight(height);
                configVideo.setFramerate(framerate);
                configVideo.setPriv(parsePriv(priv));

                return configVideo;
            } catch (JSONException e) {

            }

            return null;
        }

        public static ConfigVideo getProfile(Context context) {
            SharedPreferences preferences = context.getSharedPreferences("config_push", Context.MODE_PRIVATE);

            String name = preferences.getString("video_name", "libx264");
            int bitrate = preferences.getInt("video_bitrate", 1200 * 1000);
            int width = preferences.getInt("video_width", 720);
            int height = preferences.getInt("video_height", 480);
            int framerate = preferences.getInt("video_frame_rate", 20);
            String priv = preferences.getString("video_priv", "{\"tune\":\"zerolatency\",\"preset\":\"veryfast\"}");

            ConfigVideo configVideo = new ConfigVideo();
            configVideo.setName(name);
            configVideo.setBitrate(bitrate);
            configVideo.setWidth(width);
            configVideo.setHeight(height);
            configVideo.setFramerate(framerate);
            configVideo.setPriv(parsePriv(priv));

            return configVideo;
        }

        public static void saveProfile(SharedPreferences.Editor editor, ConfigVideo configVideo) {
            if (configVideo != null) {
                editor.putString("video_name", configVideo.getName());
                editor.putInt("video_bitrate", configVideo.getBitrate());
                editor.putInt("video_width", configVideo.getWidth());
                editor.putInt("video_height", configVideo.getHeight());
                editor.putInt("video_frame_rate", configVideo.getFramerate());
                editor.putString("video_priv", parsePriv(configVideo.getPriv()));
            }
        }
    }

    public static class ConfigAudio {
        private String name;
        private int bitrate;
        private int channels;
        private int samplerate;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getBitrate() {
            return bitrate;
        }

        public void setBitrate(int bitrate) {
            this.bitrate = bitrate;
        }

        public int getChannels() {
            return channels;
        }

        public void setChannels(int channels) {
            this.channels = channels;
        }

        public int getSamplerate() {
            return samplerate;
        }

        public void setSamplerate(int samplerate) {
            this.samplerate = samplerate;
        }

        public static void parse(JSONObject root, ConfigAudio configAudio) {
            if (configAudio != null) {
                try {
                    JSONObject audio = new JSONObject();
                    audio.put("name", configAudio.getName());
                    audio.put("bitrate", configAudio.getBitrate());
                    audio.put("channels", configAudio.getChannels());
                    audio.put("samplerate", configAudio.getSamplerate());

                    root.put("audio", audio);
                } catch (JSONException e) {

                }
            }
        }

        public static ConfigAudio parse(JSONObject root) {
            try {
                String name = root.has("name") ? root.getString("name") : null;
                int bitrate = root.has("bitrate") ? root.getInt("bitrate") : 0;
                int channels = root.has("channels") ? root.getInt("channels") : 0;
                int samplerate = root.has("samplerate") ? root.getInt("samplerate") : 0;

                ConfigAudio configAudio = new ConfigAudio();
                configAudio.setName(name);
                configAudio.setBitrate(bitrate);
                configAudio.setChannels(channels);
                configAudio.setSamplerate(samplerate);

                return configAudio;
            } catch (JSONException e) {

            }

            return null;
        }

        public static ConfigAudio getProfile(Context context) {
            SharedPreferences preferences = context.getSharedPreferences("config_push", Context.MODE_PRIVATE);

            String name = preferences.getString("audio_name", "libfdk_aac");
            int bitrate = preferences.getInt("audio_bitrate", 64 * 1000);
            int channels = preferences.getInt("audio_channels", 2);
            int samplerate = preferences.getInt("audio_samplerate", 44100);

            ConfigAudio configAudio = new ConfigAudio();
            configAudio.setName(name);
            configAudio.setBitrate(bitrate);
            configAudio.setChannels(channels);
            configAudio.setSamplerate(samplerate);

            return configAudio;
        }

        public static void saveProfile(SharedPreferences.Editor editor, ConfigAudio configAudio) {
            if (configAudio != null) {
                editor.putString("audio_name", configAudio.getName());
                editor.putInt("audio_bitrate", configAudio.getBitrate());
                editor.putInt("audio_channels", configAudio.getChannels());
                editor.putInt("audio_samplerate", configAudio.getSamplerate());
            }
        }
    }
}
