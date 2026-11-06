package com.dantsu.thermalprinter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.dantsu.escposprinter.connection.usb.UsbConnection;
import com.dantsu.thermalprinter.core.PrinterConnectionManager;
import com.dantsu.thermalprinter.databinding.ActivityMainBinding;
import com.dantsu.thermalprinter.model.PrinterConfig;
import com.dantsu.thermalprinter.ui.MainPagerAdapter;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public interface OnPermissionsGrantedListener {
        void onPermissionsGranted();
    }

    private ActivityMainBinding binding;
    private PrinterConfig config;
    private OnPermissionsGrantedListener pendingBluetoothAction;

    private final ActivityResultLauncher<String[]> bluetoothPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted && pendingBluetoothAction != null) {
                    pendingBluetoothAction.onPermissionsGranted();
                } else if (!allGranted) {
                    Toast.makeText(this, "Bluetooth permissions are required to discover and connect to thermal printers.", Toast.LENGTH_LONG).show();
                }
                pendingBluetoothAction = null;
            }
    );

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (PrinterConnectionManager.ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                    UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (usbManager != null && usbDevice != null) {
                            PrinterConnectionManager.getInstance().setSelectedUsbDevice(new UsbConnection(usbManager, usbDevice));
                            Toast.makeText(context, "USB Printer authorized: " + usbDevice.getDeviceName(), Toast.LENGTH_SHORT).show();
                            updateStatusDisplay();
                        }
                    } else {
                        Toast.makeText(context, "USB permission denied by user.", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Toast.makeText(context, "USB Printer attached. Ready to print.", Toast.LENGTH_SHORT).show();
                updateStatusDisplay();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Toast.makeText(context, "USB Printer disconnected.", Toast.LENGTH_SHORT).show();
                updateStatusDisplay();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        config = PrinterConfig.load(this);

        setupToolbar();
        setupViewPagerAndTabs();
        registerUsbReceivers();
        updateStatusDisplay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        config = PrinterConfig.load(this);
        updateStatusDisplay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(usbReceiver);
        } catch (Exception ignored) {}
        binding = null; // BUG-10 FIX: prevent stale binding references
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
    }

    private void setupViewPagerAndTabs() {
        MainPagerAdapter adapter = new MainPagerAdapter(this);
        binding.viewPager.setAdapter(adapter);
        binding.viewPager.setOffscreenPageLimit(4);

        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.tab_receipt);
                    tab.setIcon(R.drawable.ic_receipt);
                    break;
                case 1:
                    tab.setText(R.string.tab_editor);
                    tab.setIcon(R.drawable.ic_code);
                    break;
                case 2:
                    tab.setText(R.string.tab_barcode);
                    tab.setIcon(R.drawable.ic_barcode);
                    break;
                case 3:
                    tab.setText(R.string.tab_image);
                    tab.setIcon(R.drawable.ic_image);
                    break;
                case 4:
                    tab.setText(R.string.tab_settings);
                    tab.setIcon(R.drawable.ic_settings);
                    break;
            }
        }).attach();
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerUsbReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(PrinterConnectionManager.ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(usbReceiver, filter);
        }
    }

    public void updateStatusDisplay() {
        // BUG-10 FIX: Guard against null binding (called after onDestroy in some lifecycle paths)
        if (binding == null) return;
        config = PrinterConfig.load(this);
        String connType = config.getConnectionType();
        binding.textConnectionMode.setText(connType);

        String targetName = "";
        if (PrinterConfig.CONNECTION_BLUETOOTH.equals(connType)) {
            targetName = config.getBluetoothDeviceName().isEmpty() ? "Default Printer" : config.getBluetoothDeviceName();
        } else if (PrinterConfig.CONNECTION_TCP.equals(connType)) {
            targetName = config.getTcpIp() + ":" + config.getTcpPort();
        } else if (PrinterConfig.CONNECTION_USB.equals(connType)) {
            targetName = "USB OTG Device";
        }

        binding.textCurrentPrinterStatus.setText(
                targetName + " • " + config.getPaperWidthMM() + "mm (" + config.getCharsPerLine() + " CPL)"
        );
    }

    /**
     * Checks and requests Bluetooth permissions for both Android 12+ and Android <= 11
     */
    public void checkBluetoothPermissions(OnPermissionsGrantedListener listener) {
        List<String> neededPermissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_ADMIN);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

        if (neededPermissions.isEmpty()) {
            if (listener != null) listener.onPermissionsGranted();
        } else {
            this.pendingBluetoothAction = listener;
            bluetoothPermissionLauncher.launch(neededPermissions.toArray(new String[0]));
        }
    }
}
