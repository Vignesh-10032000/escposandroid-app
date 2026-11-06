package com.dantsu.thermalprinter.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.dantsu.thermalprinter.model.PrintJobRecord;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PrintHistoryStore {
    private static final String PREF_NAME = "print_history_prefs";
    private static final String KEY_JOBS = "jobs_list";
    private static final int MAX_JOBS = 30;

    public static synchronized void addJob(Context context, PrintJobRecord job) {
        List<PrintJobRecord> list = getJobs(context);
        list.add(0, job);
        if (list.size() > MAX_JOBS) {
            list = list.subList(0, MAX_JOBS);
        }
        saveJobs(context, list);
    }

    public static synchronized List<PrintJobRecord> getJobs(Context context) {
        List<PrintJobRecord> list = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_JOBS, null);
        if (json == null || json.isEmpty()) return list;

        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new PrintJobRecord(
                        obj.optString("title"),
                        obj.optString("details"),
                        obj.optString("connectionType"),
                        obj.optString("targetDevice"),
                        obj.optBoolean("success"),
                        obj.optString("rawText")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static synchronized void clearHistory(Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
        editor.remove(KEY_JOBS);
        editor.apply();
    }

    private static void saveJobs(Context context, List<PrintJobRecord> list) {
        try {
            JSONArray arr = new JSONArray();
            for (PrintJobRecord j : list) {
                JSONObject obj = new JSONObject();
                obj.put("title", j.getTitle());
                obj.put("details", j.getDetails());
                obj.put("connectionType", j.getConnectionType());
                obj.put("targetDevice", j.getTargetDevice());
                obj.put("success", j.isSuccess());
                obj.put("rawText", j.getRawText());
                arr.put(obj);
            }
            SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
            editor.putString(KEY_JOBS, arr.toString());
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
