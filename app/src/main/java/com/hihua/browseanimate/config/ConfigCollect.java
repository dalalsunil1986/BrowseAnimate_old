package com.hihua.browseanimate.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.hihua.browseanimate.util.UtilLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Created by hihua on 17/6/23.
 */

public class ConfigCollect {
    private String title;
    private String url;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public static List<ConfigCollect> getCollects(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("config_collect", Context.MODE_PRIVATE);

        String string = preferences.getString("collect_json", null);
        if (string != null && string.length() > 0) {
            try {
                JSONArray jsonArray = new JSONArray(string);
                int size = jsonArray.length();
                if (size > 0) {
                    List<ConfigCollect> collects = new Vector<ConfigCollect>();
                    for (int i = 0; i < size;i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);

                        String title = jsonObject.getString("title");
                        String url = jsonObject.getString("url");

                        ConfigCollect collect = new ConfigCollect();
                        collect.setTitle(title);
                        collect.setUrl(url);

                        collects.add(collect);
                    }

                    if (collects.size() > 0)
                        return collects;
                }
            } catch (Exception e) {
                UtilLog.writeError(ConfigCollect.class.getClass(), e);
            }
        }

        return null;
    }

    public static boolean saveCollects(Context context, List<ConfigCollect> collects) {
        if (collects != null) {
            try {
                List<Map<String, String>> list = new Vector<Map<String, String>>();

                for (ConfigCollect collect : collects) {
                    String title = collect.getTitle();
                    String url = collect.getUrl();

                    Map<String, String> map = new HashMap<String, String>();
                    map.put("title", title);
                    map.put("url", url);

                    list.add(map);
                }

                JSONArray jsonArray = new JSONArray(list);
                String string = jsonArray.toString();

                SharedPreferences preferences = context.getSharedPreferences("config_collect", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("collect_json", string);
                editor.commit();

                return true;
            } catch (Exception e) {
                UtilLog.writeError(ConfigCollect.class.getClass(), e);
            }
        }

        return false;
    }

    public static List<ConfigCollect> initCollect() {
        List<ConfigCollect> collects = new Vector<ConfigCollect>();

        ConfigCollect collect = new ConfigCollect();
        collect.setTitle("百度搜索");
        collect.setUrl("http://m.baidu.com");
        collects.add(collect);

        collect = new ConfigCollect();
        collect.setTitle("【暗黑破坏神3：夺魂之镰】凯恩之角_暗黑3（Diablo3）中文网");
        collect.setUrl("http://d.163.com");
        collects.add(collect);

        collect = new ConfigCollect();
        collect.setTitle("广州天气");
        collect.setUrl("http://www.tqyb.com.cn");
        collects.add(collect);

        collect = new ConfigCollect();
        collect.setTitle("TicWatch 表盘专区");
        collect.setUrl("https://bbs.chumenwenwen.com/forum.php?mod=forumdisplay&fid=282");
        collects.add(collect);

        collect = new ConfigCollect();
        collect.setTitle("OnePlus 6论坛");
        collect.setUrl("http://www.oneplusbbs.com/forum-119-1.html");
        collects.add(collect);

        return collects;
    }
}
