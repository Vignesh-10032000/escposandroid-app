package com.dantsu.thermalprinter.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.connection.usb.UsbConnection;
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections;
import com.dantsu.thermalprinter.MainActivity;
import com.dantsu.thermalprinter.R;
import com.dantsu.thermalprinter.core.PrintHistoryStore;
import com.dantsu.thermalprinter.core.PrinterConnectionManager;
import com.dantsu.thermalprinter.core.PrinterExecutor;
import com.dantsu.thermalprinter.databinding.FragmentSettingsBinding;
import com.dantsu.thermalprinter.model.PrintJobRecord;
import com.dantsu.thermalprinter.model.PrinterConfig;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Executors;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private PrinterConfig config;
    private PrintProgressDialog progressDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context context = requireContext();
        config = PrinterConfig.load(context);
        progressDialog = new PrintProgressDialog(context);

        loadUiFromConfig();
        setupInterfaceSelector();
        setupBluetoothControls();
        setupTcpControls();
        setupUsbControls();
        setupHardwareProfileControls();
        setupDiagnosticTools();
    }

    private void loadUiFromConfig() {
        if (PrinterConfig.CONNECTION_BLUETOOTH.equals(config.getConnectionType())) {
            binding.radioBluetooth.setChecked(true);
            showInterfaceSection(PrinterConfig.CONNECTION_BLUETOOTH);
        } else if (PrinterConfig.CONNECTION_TCP.equals(config.getConnectionType())) {
            binding.radioTcp.setChecked(true);
            showInterfaceSection(PrinterConfig.CONNECTION_TCP);
        } else if (PrinterConfig.CONNECTION_USB.equals(config.getConnectionType())) {
            binding.radioUsb.setChecked(true);
            showInterfaceSection(PrinterConfig.CONNECTION_USB);
        }

        binding.textSelectedBluetooth.setText("Target: " + config.getBluetoothDeviceName() +
                (config.getBluetoothDeviceAddress().isEmpty() ? "" : " (" + config.getBluetoothDeviceAddress() + ")"));

        binding.editTcpIp.setText(config.getTcpIp());
        binding.editTcpPort.setText(String.valueOf(config.getTcpPort()));

        if (config.getPaperWidthMM() == 80) {
            binding.radioPaper80mm.setChecked(true);
        } else {
            binding.radioPaper58mm.setChecked(true);
        }

        binding.sliderFeedDistance.setValue(config.getFeedPaperMM());
        binding.textFeedDistanceLabel.setText("Feed Paper After Job: " + (int) config.getFeedPaperMM() + " mm");

        binding.switchAutoCut.setChecked(config.isAutoCut());
        binding.switchCashDrawer.setChecked(config.isOpenCashDrawer());
        binding.switchEscAsterisk.setChecked(config.isUseEscAsterisk());
    }

    private void showInterfaceSection(String type) {
        binding.layoutBluetoothControls.setVisibility(PrinterConfig.CONNECTION_BLUETOOTH.equals(type) ? View.VISIBLE : View.GONE);
        binding.layoutTcpControls.setVisibility(PrinterConfig.CONNECTION_TCP.equals(type) ? View.VISIBLE : View.GONE);
        binding.layoutUsbControls.setVisibility(PrinterConfig.CONNECTION_USB.equals(type) ? View.VISIBLE : View.GONE);
    }

    private void setupInterfaceSelector() {
        binding.radioGroupInterface.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioBluetooth) {
                config.setConnectionType(PrinterConfig.CONNECTION_BLUETOOTH);
            } else if (checkedId == R.id.radioTcp) {
                config.setConnectionType(PrinterConfig.CONNECTION_TCP);
            } else if (checkedId == R.id.radioUsb) {
                config.setConnectionType(PrinterConfig.CONNECTION_USB);
            }
            config.save(requireContext());
            showInterfaceSection(config.getConnectionType());
            notifyParentStatusUpdate();
        });
    }

    private void setupBluetoothControls() {
        binding.btnBrowseBluetooth.setOnClickListener(v -> {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                mainActivity.checkBluetoothPermissions(this::showBluetoothDeviceChooser);
            }
        });
    }

    private void showBluetoothDeviceChooser() {
        try {
            BluetoothConnection[] list = (new BluetoothPrintersConnections()).getList();
            if (list == null || list.length == 0) {
                PrintProgressDialog.showError(requireContext(), "No Bluetooth Printers",
                        "No paired Bluetooth printers found. Please pair your thermal printer in Android Bluetooth Settings first.");
                return;
            }

            final String[] names = new String[list.length + 1];
            names[0] = "Default Paired Printer";
            for (int i = 0; i < list.length; i++) {
                BluetoothConnection c = list[i];
                String name = c.getDevice() != null ? c.getDevice().getName() : "Unknown Device";
                String mac = c.getDevice() != null ? c.getDevice().getAddress() : "";
                names[i + 1] = name + " [" + mac + "]";
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("Select Bluetooth Thermal Printer")
                    .setItems(names, (dialog, which) -> {
                        if (which == 0) {
                            config.setBluetoothDeviceAddress("");
                            config.setBluetoothDeviceName("Default Printer");
                            PrinterConnectionManager.getInstance().setSelectedBluetoothDevice(null);
                        } else {
                            BluetoothConnection selected = list[which - 1];
                            PrinterConnectionManager.getInstance().setSelectedBluetoothDevice(selected);
                            config.setBluetoothDeviceAddress(selected.getDevice().getAddress());
                            config.setBluetoothDeviceName(selected.getDevice().getName());
                        }
                        config.save(requireContext());
                        binding.textSelectedBluetooth.setText("Target: " + config.getBluetoothDeviceName());
                        notifyParentStatusUpdate();
                        Toast.makeText(requireContext(), "Selected: " + config.getBluetoothDeviceName(), Toast.LENGTH_SHORT).show();
                    })
                    .show();

        } catch (Exception e) {
            PrintProgressDialog.showError(requireContext(), "Bluetooth Error", e.getMessage());
        }
    }

    private void setupTcpControls() {
        binding.editTcpIp.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                config.setTcpIp(s.toString().trim());
                config.save(requireContext());
                notifyParentStatusUpdate();
            }
        });

        binding.editTcpPort.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                try {
                    config.setTcpPort(Integer.parseInt(s.toString().trim()));
                    config.save(requireContext());
                } catch (Exception ignored) {}
            }
        });

        binding.btnTestTcpPing.setOnClickListener(v -> {
            String ip = config.getTcpIp();
            int port = config.getTcpPort();
            progressDialog.show("Testing TCP Ping", "Attempting socket connection to " + ip + ":" + port + "…");

            Executors.newSingleThreadExecutor().execute(() -> {
                boolean reachable = false;
                String errorMsg = "";
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(ip, port), 4000);
                    reachable = true;
                } catch (Exception e) {
                    errorMsg = e.getMessage();
                }

                final boolean success = reachable;
                final String msg = errorMsg;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        if (success) {
                            PrintProgressDialog.showSuccess(requireContext(), "Connected to printer at " + ip + ":" + port + " successfully!");
                        } else {
                            PrintProgressDialog.showError(requireContext(), "TCP Connection Failed",
                                    "Could not connect to " + ip + ":" + port + ".\nDetails: " + msg);
                        }
                    });
                }
            });
        });
    }

    private void setupUsbControls() {
        binding.btnRequestUsbPermission.setOnClickListener(v -> {
            UsbConnection usbConnection = UsbPrintersConnections.selectFirstConnected(requireContext());
            if (usbConnection == null) {
                PrintProgressDialog.showError(requireContext(), "No USB Device Found",
                        "Please connect your thermal printer via a USB OTG adapter and turn on printer power.");
                return;
            }
            PrinterConnectionManager.getInstance().requestUsbPermission(requireContext(), usbConnection);
            binding.textSelectedUsb.setText("USB Connected: " + usbConnection.getDevice().getDeviceName());
            Toast.makeText(requireContext(), "Requested USB permission for printer", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupHardwareProfileControls() {
        binding.radioGroupPaperWidth.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioPaper80mm) {
                config.setPaperWidthMM(80);
            } else {
                config.setPaperWidthMM(58);
            }
            config.save(requireContext());
            notifyParentStatusUpdate();
        });

        binding.sliderFeedDistance.addOnChangeListener((slider, value, fromUser) -> {
            config.setFeedPaperMM(value);
            config.save(requireContext());
            binding.textFeedDistanceLabel.setText("Feed Paper After Job: " + (int) value + " mm");
        });

        binding.switchAutoCut.setOnCheckedChangeListener((buttonView, isChecked) -> {
            config.setAutoCut(isChecked);
            config.save(requireContext());
        });

        binding.switchCashDrawer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            config.setOpenCashDrawer(isChecked);
            config.save(requireContext());
        });

        binding.switchEscAsterisk.setOnCheckedChangeListener((buttonView, isChecked) -> {
            config.setUseEscAsterisk(isChecked);
            config.save(requireContext());
        });
    }

    private void setupDiagnosticTools() {
        binding.btnPrintSelfTest.setOnClickListener(v -> {
            config = PrinterConfig.load(requireContext());
            progressDialog.show("Running Diagnostic Print", "Connecting to " + config.getConnectionType() + "…");

            PrinterExecutor.printDiagnosticTestPage(requireContext(), config, new PrinterExecutor.PrintCallback() {
                @Override public void onStart() {}
                @Override public void onProgress(String message) { progressDialog.updateStep(message); }
                @Override public void onSuccess(String message) {
                    progressDialog.dismiss();
                    PrintProgressDialog.showSuccess(requireContext(), "Hardware self-test page printed!");
                }
                @Override public void onError(String errorMessage, Throwable error) {
                    progressDialog.dismiss();
                    PrintProgressDialog.showError(requireContext(), "Self-Test Failed", errorMessage);
                }
            });
        });

        binding.btnQuickFeed.setOnClickListener(v -> {
            String feedText = "[L]\n[L]\n[L]\n";
            quickPrintCommand(feedText, "Paper Feed");
        });

        binding.btnQuickCut.setOnClickListener(v -> {
            String cutText = "[C]--- PAPER CUT TEST ---\n";
            quickPrintCommand(cutText, "Paper Cut");
        });

        binding.btnQuickKickDrawer.setOnClickListener(v -> {
            PrinterConfig custom = PrinterConfig.load(requireContext());
            custom.setOpenCashDrawer(true);
            quickPrintCommand("[C]--- CASH DRAWER PULSE ---\n", "Cash Drawer Pulse");
        });

        binding.btnViewPrintHistory.setOnClickListener(v -> showHistoryDialog());
    }

    private void quickPrintCommand(String text, String title) {
        config = PrinterConfig.load(requireContext());
        progressDialog.show(title, "Sending command…");
        PrinterExecutor.printFormattedText(requireContext(), text, title, config, new PrinterExecutor.PrintCallback() {
            @Override public void onStart() {}
            @Override public void onProgress(String message) { progressDialog.updateStep(message); }
            @Override public void onSuccess(String message) {
                progressDialog.dismiss();
                Toast.makeText(requireContext(), title + " completed", Toast.LENGTH_SHORT).show();
            }
            @Override public void onError(String errorMessage, Throwable error) {
                progressDialog.dismiss();
                PrintProgressDialog.showError(requireContext(), title + " Error", errorMessage);
            }
        });
    }

    private void showHistoryDialog() {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_print_history, null);
        RecyclerView rv = v.findViewById(R.id.recyclerHistory);
        TextView empty = v.findViewById(R.id.textEmptyHistory);
        View btnClear = v.findViewById(R.id.btnClearHistory);

        List<PrintJobRecord> list = PrintHistoryStore.getJobs(requireContext());
        if (list.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
        } else {
            empty.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(v)
                .setPositiveButton("Close", null)
                .create();

        PrintHistoryAdapter adapter = new PrintHistoryAdapter(list, record -> {
            dialog.dismiss();
            if (record.getRawText() != null && !record.getRawText().isEmpty() && !record.getRawText().equals("[Image]")) {
                quickPrintCommand(record.getRawText(), "Re-Print: " + record.getTitle());
            } else {
                Toast.makeText(requireContext(), "Re-printing binary job: " + record.getTitle(), Toast.LENGTH_SHORT).show();
            }
        });

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        btnClear.setOnClickListener(btn -> {
            PrintHistoryStore.clearHistory(requireContext());
            list.clear();
            adapter.notifyDataSetChanged();
            empty.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
        });

        dialog.show();
    }

    private void notifyParentStatusUpdate() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.updateStatusDisplay();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
