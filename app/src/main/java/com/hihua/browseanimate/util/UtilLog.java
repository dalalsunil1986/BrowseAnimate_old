package com.hihua.browseanimate.util;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * Created by hihua on 18/3/30.
 */

public class UtilLog {

    public static void writeDebug(Class<?> classObj, String content) {
        String className = classObj.getName();
        Log.d(className, content != null ? content : "null");
    }

    public static void writeInfo(Class<?> classObj, String content) {
        String className = classObj.getName();
        Log.i(className, content != null ? content : "null");
    }

    public static void writeError(Class<?> classObj, Exception e) {
        String className = classObj.getName();
        String msg = e.getMessage();
        Log.e(className, msg != null ? msg : "null", e);
    }

    public static void writeLog(Class<?> classObj, String content) {
        String dateTime = UtilDateTime.getNow("yyyy-MM-dd HH:mm:ss");
        String className = classObj.getName();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(dateTime);
        stringBuilder.append("    ");
        stringBuilder.append(className);
        stringBuilder.append("\n");
        stringBuilder.append(content != null ? content : "null");
        stringBuilder.append("\n");

        File root = Environment.getExternalStorageDirectory();
        File dir = new File(root.getPath(), "browseanimate");

        if (!dir.exists())
            dir.mkdirs();

        String fileName = dir.getPath() + "/log.txt";

        OutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(fileName, true);
        } catch (FileNotFoundException e) {

        }

        String log = stringBuilder.toString();
        byte[] bytes = log.getBytes(Charset.defaultCharset());

        try {
            outputStream.write(bytes);
        } catch (IOException e) {

        }

        try {
            outputStream.close();
        } catch (IOException e) {

        }
    }
}
