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
    /** Max raw ESC/POS text bytes stored per job. Prevents SharedPrefs bloat from image hex strings. */
    private static final int MAX_RAW_TEXT_BYTES = 2048;

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
                PrintJobRecord record = new PrintJobRecord(
                        obj.optString("title"),
                        obj.optString("details"),
                        obj.optString("connectionType"),
                        obj.optString("targetDevice"),
                        obj.optBoolean("success"),
                        obj.optString("rawText")
                );
                // BUG-08 FIX: Restore persisted id and timestamp
                record.restoreIdAndTimestamp(
                        obj.optString("id", record.getId()),
                        obj.optLong("timestamp", record.getTimestamp())
                );
                list.add(record);
            }
        } catch (Exception e) {
            // Corrupt data — reset history gracefully
            clearHistory(context);
        }
        return list;
    }

    public static synchronized void clearHistory(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_JOBS)
                .apply();
    }

    private static void saveJobs(Context context, List<PrintJobRecord> list) {
        try {
            JSONArray arr = new JSONArray();
            for (PrintJobRecord j : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", j.getId());
                obj.put("timestamp", j.getTimestamp()); // BUG-08 FIX
                obj.put("title", j.getTitle());
                obj.put("details", j.getDetails());
                obj.put("connectionType", j.getConnectionType());
                obj.put("targetDevice", j.getTargetDevice());
                obj.put("success", j.isSuccess());
                // OPT-07: Truncate oversized rawText (image hex strings) to prevent SharedPrefs bloat
                String raw = j.getRawText();
                if (raw != null && raw.length() > MAX_RAW_TEXT_BYTES) {
                    raw = raw.substring(0, MAX_RAW_TEXT_BYTES) + "...[truncated]";
                }
                obj.put("rawText", raw);
                arr.put(obj);
            }
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_JOBS, arr.toString())
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
