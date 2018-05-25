package com.hihua.browseanimate.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by hihua on 17/6/20.
 */

public class UtilDateTime {

    public static String getNow(String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        return format.format(new Date());
    }

    public static String getTimestamp(long timestamp, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        return format.format(new Date(timestamp));
    }
}
