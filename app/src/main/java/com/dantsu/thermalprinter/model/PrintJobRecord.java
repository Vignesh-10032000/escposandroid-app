package com.dantsu.thermalprinter.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PrintJobRecord implements Serializable {
    private String id;
    private long timestamp;
    private String title;
    private String details;
    private String connectionType;
    private String targetDevice;
    private boolean success;
    private String rawText;

    public PrintJobRecord(String title, String details, String connectionType, String targetDevice, boolean success, String rawText) {
        this.id = String.valueOf(System.currentTimeMillis());
        this.timestamp = System.currentTimeMillis();
        this.title = title;
        this.details = details;
        this.connectionType = connectionType;
        this.targetDevice = targetDevice;
        this.success = success;
        this.rawText = rawText;
    }

    /**
     * BUG-08 FIX: Restore id and timestamp from persisted storage so history shows correct dates.
     */
    public void restoreIdAndTimestamp(String persistedId, long persistedTimestamp) {
        if (persistedId != null && !persistedId.isEmpty()) this.id = persistedId;
        if (persistedTimestamp > 0) this.timestamp = persistedTimestamp;
    }

    public String getId() { return id; }
    public long getTimestamp() { return timestamp; }
    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    public String getTitle() { return title; }
    public String getDetails() { return details; }
    public String getConnectionType() { return connectionType; }
    public String getTargetDevice() { return targetDevice; }
    public boolean isSuccess() { return success; }
    public String getRawText() { return rawText; }
}
