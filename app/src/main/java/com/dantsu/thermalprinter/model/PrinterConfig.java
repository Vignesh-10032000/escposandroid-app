package com.dantsu.thermalprinter.model;

import android.content.Context;
import android.content.SharedPreferences;

public class PrinterConfig {
    private static final String PREF_NAME = "printer_config_prefs";

    public static final String CONNECTION_BLUETOOTH = "BLUETOOTH";
    public static final String CONNECTION_TCP = "TCP";
    public static final String CONNECTION_USB = "USB";

    private String connectionType = CONNECTION_BLUETOOTH;
    private String bluetoothDeviceAddress = "";
    private String bluetoothDeviceName = "Default Printer";

    private String tcpIp = "192.168.1.100";
    private int tcpPort = 9100;
    private int tcpTimeout = 15;

    private int paperWidthMM = 58; // 58 or 80
    private int printerDpi = 203;
    private int charsPerLine = 32; // 32 for 58mm, 48 for 80mm
    private float feedPaperMM = 20.0f;
    private boolean autoCut = true;
    private boolean openCashDrawer = false;
    private boolean useEscAsterisk = false;
    private String charsetName = "windows-1252";
    private int charsetId = 16;

    public static PrinterConfig load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        PrinterConfig config = new PrinterConfig();
        config.connectionType = prefs.getString("connectionType", CONNECTION_BLUETOOTH);
        config.bluetoothDeviceAddress = prefs.getString("bluetoothDeviceAddress", "");
        config.bluetoothDeviceName = prefs.getString("bluetoothDeviceName", "Default Printer");
        config.tcpIp = prefs.getString("tcpIp", "192.168.1.100");
        config.tcpPort = prefs.getInt("tcpPort", 9100);
        config.tcpTimeout = prefs.getInt("tcpTimeout", 15);
        config.paperWidthMM = prefs.getInt("paperWidthMM", 58);
        config.printerDpi = prefs.getInt("printerDpi", 203);
        config.charsPerLine = prefs.getInt("charsPerLine", config.paperWidthMM == 80 ? 48 : 32);
        config.feedPaperMM = prefs.getFloat("feedPaperMM", 20.0f);
        config.autoCut = prefs.getBoolean("autoCut", true);
        config.openCashDrawer = prefs.getBoolean("openCashDrawer", false);
        config.useEscAsterisk = prefs.getBoolean("useEscAsterisk", false);
        config.charsetName = prefs.getString("charsetName", "windows-1252");
        config.charsetId = prefs.getInt("charsetId", 16);
        return config;
    }

    public void save(Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
        editor.putString("connectionType", connectionType);
        editor.putString("bluetoothDeviceAddress", bluetoothDeviceAddress);
        editor.putString("bluetoothDeviceName", bluetoothDeviceName);
        editor.putString("tcpIp", tcpIp);
        editor.putInt("tcpPort", tcpPort);
        editor.putInt("tcpTimeout", tcpTimeout);
        editor.putInt("paperWidthMM", paperWidthMM);
        editor.putInt("printerDpi", printerDpi);
        editor.putInt("charsPerLine", charsPerLine);
        editor.putFloat("feedPaperMM", feedPaperMM);
        editor.putBoolean("autoCut", autoCut);
        editor.putBoolean("openCashDrawer", openCashDrawer);
        editor.putBoolean("useEscAsterisk", useEscAsterisk);
        editor.putString("charsetName", charsetName);
        editor.putInt("charsetId", charsetId);
        editor.apply();
    }

    // Getters and Setters
    public String getConnectionType() { return connectionType; }
    public void setConnectionType(String connectionType) { this.connectionType = connectionType; }

    public String getBluetoothDeviceAddress() { return bluetoothDeviceAddress; }
    public void setBluetoothDeviceAddress(String bluetoothDeviceAddress) { this.bluetoothDeviceAddress = bluetoothDeviceAddress; }

    public String getBluetoothDeviceName() { return bluetoothDeviceName; }
    public void setBluetoothDeviceName(String bluetoothDeviceName) { this.bluetoothDeviceName = bluetoothDeviceName; }

    public String getTcpIp() { return tcpIp; }
    public void setTcpIp(String tcpIp) { this.tcpIp = tcpIp; }

    public int getTcpPort() { return tcpPort; }
    public void setTcpPort(int tcpPort) { this.tcpPort = tcpPort; }

    public int getTcpTimeout() { return tcpTimeout; }
    public void setTcpTimeout(int tcpTimeout) { this.tcpTimeout = tcpTimeout; }

    public int getPaperWidthMM() { return paperWidthMM; }
    public void setPaperWidthMM(int paperWidthMM) {
        this.paperWidthMM = paperWidthMM;
        if (paperWidthMM == 80 && this.charsPerLine <= 32) {
            this.charsPerLine = 48;
        } else if (paperWidthMM == 58 && this.charsPerLine >= 48) {
            this.charsPerLine = 32;
        }
    }

    public int getPrinterDpi() { return printerDpi; }
    public void setPrinterDpi(int printerDpi) { this.printerDpi = printerDpi; }

    public int getCharsPerLine() { return charsPerLine; }
    public void setCharsPerLine(int charsPerLine) { this.charsPerLine = charsPerLine; }

    public float getFeedPaperMM() { return feedPaperMM; }
    public void setFeedPaperMM(float feedPaperMM) { this.feedPaperMM = feedPaperMM; }

    public boolean isAutoCut() { return autoCut; }
    public void setAutoCut(boolean autoCut) { this.autoCut = autoCut; }

    public boolean isOpenCashDrawer() { return openCashDrawer; }
    public void setOpenCashDrawer(boolean openCashDrawer) { this.openCashDrawer = openCashDrawer; }

    public boolean isUseEscAsterisk() { return useEscAsterisk; }
    public void setUseEscAsterisk(boolean useEscAsterisk) { this.useEscAsterisk = useEscAsterisk; }

    public String getCharsetName() { return charsetName; }
    public void setCharsetName(String charsetName) { this.charsetName = charsetName; }

    public int getCharsetId() { return charsetId; }
    public void setCharsetId(int charsetId) { this.charsetId = charsetId; }
}
