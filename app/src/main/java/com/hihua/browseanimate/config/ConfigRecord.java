package com.hihua.browseanimate.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;

/**
 * Created by hihua on 17/6/20.
 */

public class ConfigRecord {
    private int audioBitRate;
    private int audioChannels;
    private int audioSampleRate;
    private int audioCodec;
    private int videoBitRate;
    private int videoFrameWidth;
    private int videoFrameHeight;
    private int videoFrameRate;
    private int videoCodec;
    private int fileFormat;
    private int orientationHint;

    public int getAudioBitRate() {
        return audioBitRate;
    }

    public void setAudioBitRate(int audioBitRate) {
        this.audioBitRate = audioBitRate;
    }

    public int getAudioChannels() {
        return audioChannels;
    }

    public void setAudioChannels(int audioChannels) {
        this.audioChannels = audioChannels;
    }

    public int getAudioSampleRate() {
        return audioSampleRate;
    }

    public void setAudioSampleRate(int audioSampleRate) {
        this.audioSampleRate = audioSampleRate;
    }

    public int getAudioCodec() {
        return audioCodec;
    }

    public void setAudioCodec(int audioCodec) {
        this.audioCodec = audioCodec;
    }

    public int getVideoBitRate() {
        return videoBitRate;
    }

    public void setVideoBitRate(int videoBitRate) {
        this.videoBitRate = videoBitRate;
    }

    public int getVideoFrameWidth() {
        return videoFrameWidth;
    }

    public void setVideoFrameWidth(int videoFrameWidth) {
        this.videoFrameWidth = videoFrameWidth;
    }

    public int getVideoFrameHeight() {
        return videoFrameHeight;
    }

    public void setVideoFrameHeight(int videoFrameHeight) {
        this.videoFrameHeight = videoFrameHeight;
    }

    public int getVideoFrameRate() {
        return videoFrameRate;
    }

    public void setVideoFrameRate(int videoFrameRate) {
        this.videoFrameRate = videoFrameRate;
    }

    public int getVideoCodec() {
        return videoCodec;
    }

    public void setVideoCodec(int videoCodec) {
        this.videoCodec = videoCodec;
    }

    public int getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(int fileFormat) {
        this.fileFormat = fileFormat;
    }

    public int getOrientationHint() {
        return orientationHint;
    }

    public void setOrientationHint(int orientationHint) {
        this.orientationHint = orientationHint;
    }

    private static ConfigRecord parse(CamcorderProfile camcorderProfile) {
        ConfigRecord configRecord = new ConfigRecord();
        configRecord.setAudioBitRate(camcorderProfile.audioBitRate);
        configRecord.setAudioChannels(camcorderProfile.audioChannels);
        configRecord.setAudioSampleRate(camcorderProfile.audioSampleRate);
        configRecord.setAudioCodec(camcorderProfile.audioCodec);
        configRecord.setVideoBitRate(camcorderProfile.videoBitRate);
        configRecord.setVideoFrameWidth(camcorderProfile.videoFrameWidth);
        configRecord.setVideoFrameHeight(camcorderProfile.videoFrameHeight);
        configRecord.setVideoFrameRate(camcorderProfile.videoFrameRate);
        configRecord.setVideoCodec(camcorderProfile.videoCodec);
        configRecord.setFileFormat(camcorderProfile.fileFormat);

        return configRecord;
    }

    public static ConfigRecord initProfile() {
        ConfigRecord configRecord = null;

        if (configRecord == null && CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_1080P)) {
            CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
            configRecord = parse(camcorderProfile);
        }

        if (configRecord == null && CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P)) {
            CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
            configRecord = parse(camcorderProfile);
        }

        if (configRecord == null && CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P)) {
            CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
            configRecord = parse(camcorderProfile);
        }

        if (configRecord == null && CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_HIGH)) {
            CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            configRecord = parse(camcorderProfile);
        }

        if (configRecord != null)
            configRecord.setOrientationHint(90);

        return configRecord;
    }

    public static void saveProfile(Context context, ConfigRecord configRecord) {
        SharedPreferences preferences = context.getSharedPreferences("config_record", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("audio_bitrate", configRecord.getAudioBitRate());
        editor.putInt("audio_channels", configRecord.getAudioChannels());
        editor.putInt("audio_samplerate", configRecord.getAudioSampleRate());

        switch (configRecord.getAudioCodec()) {
            case MediaRecorder.AudioEncoder.AAC: {
                editor.putString("audio_codec", "aac");
                break;
            }

            case MediaRecorder.AudioEncoder.AAC_ELD: {
                editor.putString("audio_codec", "aac_eld");
                break;
            }

            case MediaRecorder.AudioEncoder.HE_AAC: {
                editor.putString("audio_codec", "aac_he");
                break;
            }

            default: {
                editor.putString("audio_codec", "default");
                break;
            }
        }

        editor.putInt("video_bitrate", configRecord.getVideoBitRate());
        editor.putInt("video_frame_width", configRecord.getVideoFrameWidth());
        editor.putInt("video_frame_height", configRecord.getVideoFrameHeight());
        editor.putInt("video_frame_rate", configRecord.getVideoFrameRate());

        switch (configRecord.getVideoCodec()) {
            case MediaRecorder.VideoEncoder.H263:
                editor.putString("video_codec", "h263");
                break;

            case MediaRecorder.VideoEncoder.H264:
                editor.putString("video_codec", "h264");
                break;

            case MediaRecorder.VideoEncoder.MPEG_4_SP:
                editor.putString("video_codec", "mp4");
                break;

            default:
                editor.putString("video_codec", "default");
                break;
        }

        switch (configRecord.getFileFormat()) {
            case MediaRecorder.OutputFormat.MPEG_4: {
                editor.putString("file_format", "mpeg4");
                break;
            }

            default: {
                editor.putString("file_format", "default");
                break;
            }
        }

        editor.putInt("orientation", configRecord.getOrientationHint());
        editor.commit();
    }

    public static ConfigRecord getProfile(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("config_record", Context.MODE_PRIVATE);

        int audioBitRate = preferences.getInt("audio_bitrate", 0);
        int audioChannels = preferences.getInt("audio_channels", 0);
        int audioSampleRate = preferences.getInt("audio_samplerate", 0);
        String audioCodec = preferences.getString("audio_codec", null);
        int videoBitRate = preferences.getInt("video_bitrate", 0);
        int videoFrameWidth = preferences.getInt("video_frame_width", 0);
        int videoFrameHeight = preferences.getInt("video_frame_height", 0);
        int videoFrameRate = preferences.getInt("video_frame_rate", 0);
        String videoCodec = preferences.getString("video_codec", null);
        String fileFormat = preferences.getString("file_format", null);
        int orientationHint = preferences.getInt("orientation", -1);

        if (audioBitRate == 0 || audioChannels == 0 || audioSampleRate == 0 || audioCodec == null)
            return null;

        if (videoBitRate == 0 || videoFrameWidth == 0 || videoFrameHeight == 0 || videoFrameRate == 0 || videoCodec == null)
            return null;

        if (fileFormat == null || orientationHint == -1)
            return null;

        ConfigRecord configRecord = new ConfigRecord();
        configRecord.setAudioBitRate(audioBitRate);
        configRecord.setAudioChannels(audioChannels);
        configRecord.setAudioSampleRate(audioSampleRate);

        if (audioCodec.equals("aac"))
            configRecord.setAudioCodec(MediaRecorder.AudioEncoder.AAC);
        else {
            if (audioCodec.equals("aac_eld"))
                configRecord.setAudioCodec(MediaRecorder.AudioEncoder.AAC_ELD);
            else {
                if (audioCodec.equals("aac_he"))
                    configRecord.setAudioCodec(MediaRecorder.AudioEncoder.HE_AAC);
                else
                    configRecord.setAudioCodec(MediaRecorder.AudioEncoder.DEFAULT);
            }
        }

        configRecord.setVideoBitRate(videoBitRate);
        configRecord.setVideoFrameWidth(videoFrameWidth);
        configRecord.setVideoFrameHeight(videoFrameHeight);
        configRecord.setVideoFrameRate(videoFrameRate);

        if (videoCodec.equals("h263"))
            configRecord.setVideoCodec(MediaRecorder.VideoEncoder.H263);
        else {
            if (videoCodec.equals("h264"))
                configRecord.setVideoCodec(MediaRecorder.VideoEncoder.H264);
            else {
                if (videoCodec.equals("mp4"))
                    configRecord.setVideoCodec(MediaRecorder.VideoEncoder.MPEG_4_SP);
                else
                    configRecord.setVideoCodec(MediaRecorder.VideoEncoder.DEFAULT);
            }
        }

        if (fileFormat.equals("mpeg4"))
            configRecord.setFileFormat(MediaRecorder.OutputFormat.MPEG_4);
        else
            configRecord.setFileFormat(MediaRecorder.OutputFormat.DEFAULT);

        configRecord.setOrientationHint(orientationHint);
        return configRecord;
    }
}
