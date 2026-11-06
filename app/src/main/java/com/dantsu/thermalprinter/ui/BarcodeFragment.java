package com.dantsu.thermalprinter.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dantsu.thermalprinter.R;
import com.dantsu.thermalprinter.core.BarcodePreviewHelper;
import com.dantsu.thermalprinter.core.PrinterExecutor;
import com.dantsu.thermalprinter.databinding.FragmentBarcodeBinding;
import com.dantsu.thermalprinter.model.PrinterConfig;
import com.google.zxing.BarcodeFormat;

public class BarcodeFragment extends Fragment {

    private FragmentBarcodeBinding binding;
    private PrinterConfig config;
    private PrintProgressDialog progressDialog;

    private enum CodeType { QR, CODE128, EAN13, EAN8, UPCA, UPCE }
    private CodeType currentType = CodeType.QR;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBarcodeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context context = requireContext();
        config = PrinterConfig.load(context);
        progressDialog = new PrintProgressDialog(context);

        setupTypeSelector();
        setupInputListeners();
        setupPrintButton();
        updateBarcodePreview();
    }

    private void setupTypeSelector() {
        binding.chipTypeQr.setOnClickListener(v -> setCodeType(CodeType.QR, "https://github.com/dantsu/escpos-thermalprinter-android", "Any URL, alphanumeric text, UPI payment address, or WiFi key"));
        binding.chipTypeCode128.setOnClickListener(v -> setCodeType(CodeType.CODE128, "DAN-TSU-2026", "Alphanumeric text string (A-Z, 0-9, symbols)"));
        binding.chipTypeEan13.setOnClickListener(v -> setCodeType(CodeType.EAN13, "831254784551", "Exactly 12 or 13 digits"));
        binding.chipTypeEan8.setOnClickListener(v -> setCodeType(CodeType.EAN8, "4512784", "Exactly 7 or 8 digits"));
        binding.chipTypeUpcA.setOnClickListener(v -> setCodeType(CodeType.UPCA, "45127845215", "Exactly 11 or 12 digits"));
        binding.chipTypeUpcE.setOnClickListener(v -> setCodeType(CodeType.UPCE, "512789", "Exactly 6 digits"));
    }

    private void setCodeType(CodeType type, String sampleData, String hint) {
        currentType = type;
        binding.editBarcodeData.setText(sampleData);
        binding.textBarcodeHelperHint.setText(hint);
        binding.textBarcodeTypePreviewBadge.setText(type.name() + " PREVIEW");
        binding.radioGroupBarcodeTextPos.setVisibility(type != CodeType.QR ? View.VISIBLE : View.GONE);
        updateBarcodePreview();
    }

    private void setupInputListeners() {
        binding.editBarcodeData.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updateBarcodePreview(); }
        });
        binding.sliderBarcodeSize.addOnChangeListener((slider, value, fromUser) ->
                binding.textSizeLabel.setText("Printed Size: " + ((int) value) + " mm"));
        binding.radioGroupBarcodeTextPos.setOnCheckedChangeListener((group, checkedId) -> updateBarcodePreview());
    }

    private void updateBarcodePreview() {
        if (binding == null || !isAdded()) return; // BUG-02 FIX
        String data = binding.editBarcodeData.getText().toString().trim();
        binding.textBarcodeCaption.setText(data);

        if (data.isEmpty()) {
            binding.imgVisualBarcodePreview.setImageBitmap(null);
            return;
        }

        // BUG-06 FIX: Async generation — doesn't block UI thread
        switch (currentType) {
            case QR:
                BarcodePreviewHelper.generateQrCodeAsync(data, 220, 220, bmp -> {
                    if (binding != null && isAdded()) binding.imgVisualBarcodePreview.setImageBitmap(bmp);
                });
                break;
            case CODE128:
                BarcodePreviewHelper.generateBarcodeAsync(data, BarcodeFormat.CODE_128, 400, 140, bmp -> {
                    if (binding != null && isAdded()) binding.imgVisualBarcodePreview.setImageBitmap(bmp);
                });
                break;
            case EAN13:
                if (data.matches("\\d{12,13}"))
                    BarcodePreviewHelper.generateBarcodeAsync(data, BarcodeFormat.EAN_13, 400, 140, bmp -> {
                        if (binding != null && isAdded()) binding.imgVisualBarcodePreview.setImageBitmap(bmp);
                    });
                break;
            case EAN8:
                if (data.matches("\\d{7,8}"))
                    BarcodePreviewHelper.generateBarcodeAsync(data, BarcodeFormat.EAN_8, 400, 140, bmp -> {
                        if (binding != null && isAdded()) binding.imgVisualBarcodePreview.setImageBitmap(bmp);
                    });
                break;
            case UPCA:
                if (data.matches("\\d{11,12}"))
                    BarcodePreviewHelper.generateBarcodeAsync(data, BarcodeFormat.UPC_A, 400, 140, bmp -> {
                        if (binding != null && isAdded()) binding.imgVisualBarcodePreview.setImageBitmap(bmp);
                    });
                break;
            case UPCE:
                if (data.matches("\\d{6}"))
                    BarcodePreviewHelper.generateBarcodeAsync(data, BarcodeFormat.UPC_E, 400, 140, bmp -> {
                        if (binding != null && isAdded()) binding.imgVisualBarcodePreview.setImageBitmap(bmp);
                    });
                break;
        }
    }

    private void setupPrintButton() {
        binding.btnPrintBarcodeDirect.setOnClickListener(v -> {
            if (!isAdded()) return;
            String data = binding.editBarcodeData.getText().toString().trim();
            if (data.isEmpty()) {
                PrintProgressDialog.showError(requireContext(), "Missing Data", "Please enter code data to print.");
                return;
            }

            int size = (int) binding.sliderBarcodeSize.getValue();
            String textPos = "below";
            if (binding.radioPosAbove.isChecked()) textPos = "above";
            else if (binding.radioPosNone.isChecked()) textPos = "none";

            StringBuilder printScript = new StringBuilder();
            printScript.append("[C]<b><font size='big'>").append(currentType.name()).append("</font></b>\n");
            printScript.append("[C]--------------------------------\n");

            if (currentType == CodeType.QR) {
                printScript.append("[C]<qrcode size='").append(size).append("'>").append(data).append("</qrcode>\n");
            } else {
                String typeName = "128";
                if (currentType == CodeType.EAN13) typeName = "ean13";
                else if (currentType == CodeType.EAN8) typeName = "ean8";
                else if (currentType == CodeType.UPCA) typeName = "upca";
                else if (currentType == CodeType.UPCE) typeName = "upce";
                printScript.append("[C]<barcode type='").append(typeName)
                        .append("' height='").append(Math.min(25, Math.max(8, size / 2)))
                        .append("' text='").append(textPos).append("'>")
                        .append(data).append("</barcode>\n");
            }
            printScript.append("[L]\n").append("[C]Data: ").append(data).append("\n")
                    .append("[C]================================\n");

            config = PrinterConfig.load(requireContext());
            progressDialog.show("Printing " + currentType.name(), "Connecting to " + config.getConnectionType() + "\u2026");

            PrinterExecutor.printFormattedText(requireContext(), printScript.toString(),
                    "Barcode " + currentType.name(), config, new PrinterExecutor.PrintCallback() {
                        @Override public void onStart() {}
                        @Override public void onProgress(String message) {
                            if (isAdded()) progressDialog.updateStep(message); // BUG-02 FIX
                        }
                        @Override public void onSuccess(String message) {
                            if (!isAdded()) return;
                            progressDialog.dismiss();
                            PrintProgressDialog.showSuccess(requireContext(), message);
                        }
                        @Override public void onError(String errorMessage, Throwable error) {
                            if (!isAdded()) return;
                            progressDialog.dismiss();
                            PrintProgressDialog.showError(requireContext(), "Print Error", errorMessage);
                        }
                    });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
