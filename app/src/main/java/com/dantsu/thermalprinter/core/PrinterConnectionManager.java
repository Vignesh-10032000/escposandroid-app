package com.dantsu.thermalprinter.core;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Build;

import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.connection.tcp.TcpConnection;
import com.dantsu.escposprinter.connection.usb.UsbConnection;
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections;
import com.dantsu.thermalprinter.model.PrinterConfig;

public class PrinterConnectionManager {
    public static final String ACTION_USB_PERMISSION = "com.dantsu.thermalprinter.USB_PERMISSION";

    private static volatile PrinterConnectionManager instance;
    private BluetoothConnection selectedBluetoothDevice = null;
    private UsbConnection selectedUsbDevice = null;
    /** Track last used connection type to detect when user switches interfaces */
    private String lastConnectionType = null;

    private PrinterConnectionManager() {}

    public static PrinterConnectionManager getInstance() {
        if (instance == null) {
            synchronized (PrinterConnectionManager.class) {
                if (instance == null) instance = new PrinterConnectionManager();
            }
        }
        return instance;
    }

    public BluetoothConnection getSelectedBluetoothDevice() {
        return selectedBluetoothDevice;
    }

    public void setSelectedBluetoothDevice(BluetoothConnection device) {
        this.selectedBluetoothDevice = device;
    }

    public UsbConnection getSelectedUsbDevice() {
        return selectedUsbDevice;
    }

    public void setSelectedUsbDevice(UsbConnection device) {
        this.selectedUsbDevice = device;
    }

    /**
     * Clears the cached connection when the user changes connection type,
     * preventing stale Bluetooth/USB handles from leaking into new sessions.
     * BUG-07 FIX.
     */
    private void invalidateIfTypeChanged(String newType) {
        if (!newType.equals(lastConnectionType)) {
            selectedBluetoothDevice = null;
            selectedUsbDevice = null;
            lastConnectionType = newType;
        }
    }

    /**
     * Returns the target DeviceConnection according to the current config.
     */
    public DeviceConnection getDeviceConnection(Context context, PrinterConfig config) {
        String type = config.getConnectionType();
        invalidateIfTypeChanged(type); // BUG-07 FIX

        if (PrinterConfig.CONNECTION_BLUETOOTH.equals(type)) {
            if (selectedBluetoothDevice != null) return selectedBluetoothDevice;
            // Auto-match saved address, fall back to first paired device
            BluetoothConnection[] list = new BluetoothPrintersConnections().getList();
            if (list != null && list.length > 0) {
                String savedAddress = config.getBluetoothDeviceAddress();
                if (savedAddress != null && !savedAddress.isEmpty()) {
                    for (BluetoothConnection conn : list) {
                        if (conn.getDevice() != null
                                && savedAddress.equalsIgnoreCase(conn.getDevice().getAddress())) {
                            selectedBluetoothDevice = conn;
                            return conn;
                        }
                    }
                }
                selectedBluetoothDevice = list[0];
                return list[0];
            }
            return BluetoothPrintersConnections.selectFirstPaired();

        } else if (PrinterConfig.CONNECTION_TCP.equals(type)) {
            return new TcpConnection(config.getTcpIp(), config.getTcpPort(), config.getTcpTimeout());

        } else if (PrinterConfig.CONNECTION_USB.equals(type)) {
            if (selectedUsbDevice != null) return selectedUsbDevice;
            UsbConnection usb = UsbPrintersConnections.selectFirstConnected(context);
            if (usb != null) selectedUsbDevice = usb;
            return usb;
        }

        return null;
    }

    /**
     * Requests USB permission if not yet granted.
     */
    public void requestUsbPermission(Context context, UsbConnection usbConnection) {
        if (usbConnection == null || usbConnection.getDevice() == null) return;
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) return;
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;
        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                context, 0, new Intent(ACTION_USB_PERMISSION), flags);
        usbManager.requestPermission(usbConnection.getDevice(), permissionIntent);
    }
}
