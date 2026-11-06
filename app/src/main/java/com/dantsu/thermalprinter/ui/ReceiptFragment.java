package com.dantsu.thermalprinter.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.dantsu.thermalprinter.R;
import com.dantsu.thermalprinter.core.BarcodePreviewHelper;
import com.dantsu.thermalprinter.core.PrinterExecutor;
import com.dantsu.thermalprinter.core.ReceiptFormatter;
import com.dantsu.thermalprinter.databinding.FragmentReceiptBinding;
import com.dantsu.thermalprinter.model.PrinterConfig;
import com.dantsu.thermalprinter.model.Receipt;
import com.dantsu.thermalprinter.model.ReceiptItem;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.zxing.BarcodeFormat;

import java.util.Locale;

public class ReceiptFragment extends Fragment {

    private FragmentReceiptBinding binding;
    private final Receipt receipt = new Receipt();
    private ReceiptItemAdapter itemAdapter;
    private PrinterConfig config;
    private PrintProgressDialog progressDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReceiptBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context context = requireContext();
        config = PrinterConfig.load(context);
        progressDialog = new PrintProgressDialog(context);

        setupRecyclerView();
        setupInputListeners();
        setupActionButtons();
        updateFullUiFromModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        config = PrinterConfig.load(requireContext());
        binding.textPreviewPaperWidth.setText(config.getPaperWidthMM() + "mm Paper (" + config.getCharsPerLine() + " Chars)");
        updateThermalPreview();
    }

    private void setupRecyclerView() {
        itemAdapter = new ReceiptItemAdapter(receipt.getItems(), receipt.getCurrencySymbol(), position -> {
            receipt.getItems().remove(position);
            itemAdapter.notifyItemRemoved(position);
            updateThermalPreview();
        });
        binding.recyclerReceiptItems.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerReceiptItems.setAdapter(itemAdapter);
    }

    private void setupActionButtons() {
        binding.btnAddItem.setOnClickListener(v -> showAddItemDialog());

        binding.btnQuickPreviewToggle.setOnClickListener(v -> {
            boolean isVisible = binding.cardLivePreview.getVisibility() == View.VISIBLE;
            binding.cardLivePreview.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            binding.btnQuickPreviewToggle.setText(isVisible ? "Show Preview" : "Hide Preview");
        });

        binding.btnPrintReceipt.setOnClickListener(v -> printReceiptNow());
    }

    private void showAddItemDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_item, null);
        EditText editName = dialogView.findViewById(R.id.editDialogItemName);
        EditText editQty = dialogView.findViewById(R.id.editDialogItemQty);
        EditText editPrice = dialogView.findViewById(R.id.editDialogItemPrice);
        ChipGroup chipGroup = dialogView.findViewById(R.id.chipGroupItemPresets);

        // Preset chip listeners
        setupPresetChip(dialogView, R.id.chipPresetEspresso, "Espresso", 3.50, editName, editPrice);
        setupPresetChip(dialogView, R.id.chipPresetCappuccino, "Cappuccino", 4.75, editName, editPrice);
        setupPresetChip(dialogView, R.id.chipPresetCroissant, "Butter Croissant", 3.25, editName, editPrice);
        setupPresetChip(dialogView, R.id.chipPresetSandwich, "Club Sandwich", 8.50, editName, editPrice);
        setupPresetChip(dialogView, R.id.chipPresetPizza, "Pizza Slice", 4.00, editName, editPrice);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add Item to Bill")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = editName.getText().toString().trim();
                    if (name.isEmpty()) name = "Custom Item";
                    int qty = 1;
                    try { qty = Integer.parseInt(editQty.getText().toString().trim()); } catch (Exception ignored) {}
                    double price = 0.0;
                    try { price = Double.parseDouble(editPrice.getText().toString().trim()); } catch (Exception ignored) {}

                    receipt.getItems().add(new ReceiptItem(name, Math.max(1, qty), price));
                    itemAdapter.notifyItemInserted(receipt.getItems().size() - 1);
                    updateThermalPreview();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupPresetChip(View view, int chipId, String name, double price, EditText editName, EditText editPrice) {
        Chip chip = view.findViewById(chipId);
        if (chip != null) {
            chip.setOnClickListener(v -> {
                editName.setText(name);
                editPrice.setText(String.format(Locale.US, "%.2f", price));
            });
        }
    }

    private void setupInputListeners() {
        TextWatcher genericWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                syncModelFromUi();
                updateThermalPreview();
            }
        };

        binding.editStoreName.addTextChangedListener(genericWatcher);
        binding.editStoreTagline.addTextChangedListener(genericWatcher);
        binding.editStorePhone.addTextChangedListener(genericWatcher);
        binding.editStoreAddress.addTextChangedListener(genericWatcher);
        binding.editStoreTaxId.addTextChangedListener(genericWatcher);
        binding.editCurrency.addTextChangedListener(genericWatcher);
        binding.editReceiptNo.addTextChangedListener(genericWatcher);
        binding.editCashierName.addTextChangedListener(genericWatcher);
        binding.editCustomerName.addTextChangedListener(genericWatcher);
        binding.editPaymentMethod.addTextChangedListener(genericWatcher);
        binding.editTaxRate.addTextChangedListener(genericWatcher);
        binding.editDiscount.addTextChangedListener(genericWatcher);
        binding.editFooterMessage.addTextChangedListener(genericWatcher);
        binding.editBarcodeValue.addTextChangedListener(genericWatcher);
        binding.editQrValue.addTextChangedListener(genericWatcher);

        binding.switchBarcode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            receipt.setIncludeBarcode(isChecked);
            binding.layoutBarcodeValue.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            updateThermalPreview();
        });

        binding.switchQrCode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            receipt.setIncludeQrCode(isChecked);
            binding.layoutQrValue.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            updateThermalPreview();
        });
    }

    private void syncModelFromUi() {
        receipt.setStoreName(binding.editStoreName.getText().toString());
        receipt.setTagline(binding.editStoreTagline.getText().toString());
        receipt.setPhone(binding.editStorePhone.getText().toString());
        receipt.setAddress(binding.editStoreAddress.getText().toString());
        receipt.setTaxId(binding.editStoreTaxId.getText().toString());
        receipt.setCurrencySymbol(binding.editCurrency.getText().toString());
        receipt.setReceiptNo(binding.editReceiptNo.getText().toString());
        receipt.setCashierName(binding.editCashierName.getText().toString());
        receipt.setCustomerName(binding.editCustomerName.getText().toString());
        receipt.setPaymentMethod(binding.editPaymentMethod.getText().toString());
        receipt.setFooterMessage(binding.editFooterMessage.getText().toString());

        try {
            receipt.setTaxRatePercent(Double.parseDouble(binding.editTaxRate.getText().toString()));
        } catch (Exception e) { receipt.setTaxRatePercent(0.0); }

        try {
            receipt.setDiscountAmount(Double.parseDouble(binding.editDiscount.getText().toString()));
        } catch (Exception e) { receipt.setDiscountAmount(0.0); }

        receipt.setBarcodeValue(binding.editBarcodeValue.getText().toString());
        receipt.setQrCodeValue(binding.editQrValue.getText().toString());
    }

    private void updateFullUiFromModel() {
        binding.editStoreName.setText(receipt.getStoreName());
        binding.editStoreTagline.setText(receipt.getTagline());
        binding.editStorePhone.setText(receipt.getPhone());
        binding.editStoreAddress.setText(receipt.getAddress());
        binding.editStoreTaxId.setText(receipt.getTaxId());
        binding.editCurrency.setText(receipt.getCurrencySymbol());
        binding.editReceiptNo.setText(receipt.getReceiptNo());
        binding.editCashierName.setText(receipt.getCashierName());
        binding.editCustomerName.setText(receipt.getCustomerName());
        binding.editPaymentMethod.setText(receipt.getPaymentMethod());
        binding.editTaxRate.setText(String.format(Locale.US, "%.2f", receipt.getTaxRatePercent()));
        binding.editDiscount.setText(String.format(Locale.US, "%.2f", receipt.getDiscountAmount()));
        binding.editFooterMessage.setText(receipt.getFooterMessage());
        binding.editBarcodeValue.setText(receipt.getBarcodeValue());
        binding.editQrValue.setText(receipt.getQrCodeValue());

        binding.switchBarcode.setChecked(receipt.isIncludeBarcode());
        binding.switchQrCode.setChecked(receipt.isIncludeQrCode());

        updateThermalPreview();
    }

    private void updateThermalPreview() {
        if (binding == null) return;
        config = PrinterConfig.load(requireContext());

        String totalBadge = String.format(Locale.US, "Total Due: %s%.2f", receipt.getCurrencySymbol(), receipt.getGrandTotal());
        binding.textGrandTotalBadge.setText(totalBadge);

        String formattedText = ReceiptFormatter.formatReceipt(receipt, config);

        // Strip raw DantSu tags for clean on-screen monospace simulation
        String cleanPreview = formattedText
                .replaceAll("\\[[LCR]\\]", "")
                .replaceAll("</?[bui]>", "")
                .replaceAll("<font[^>]*>", "")
                .replaceAll("</font>", "")
                .replaceAll("<u[^>]*>", "")
                .replaceAll("</u>", "")
                .replaceAll("<barcode[^>]*>.*?</barcode>", "[BARCODE GENERATED BELOW]")
                .replaceAll("<qrcode[^>]*>.*?</qrcode>", "[QR CODE GENERATED BELOW]")
                .replaceAll("<img>.*?</img>", "[STORE LOGO BITMAP]");

        binding.textLiveThermalPreview.setText(cleanPreview.trim());

        // Update live Barcode preview bitmap
        if (receipt.isIncludeBarcode() && receipt.getBarcodeValue() != null && !receipt.getBarcodeValue().trim().isEmpty()) {
            binding.imgPreviewBarcode.setVisibility(View.VISIBLE);
            String barcode = receipt.getBarcodeValue().trim();
            Bitmap bmap = null;
            if (barcode.matches("\\d{12}")) {
                bmap = BarcodePreviewHelper.generateBarcode(barcode, BarcodeFormat.EAN_13, 400, 100);
            } else if (barcode.matches("\\d{11}")) {
                bmap = BarcodePreviewHelper.generateBarcode(barcode, BarcodeFormat.UPC_A, 400, 100);
            } else {
                bmap = BarcodePreviewHelper.generateBarcode(barcode, BarcodeFormat.CODE_128, 400, 100);
            }
            if (bmap != null) binding.imgPreviewBarcode.setImageBitmap(bmap);
        } else {
            binding.imgPreviewBarcode.setVisibility(View.GONE);
        }

        // Update live QR Code preview bitmap
        if (receipt.isIncludeQrCode() && receipt.getQrCodeValue() != null && !receipt.getQrCodeValue().trim().isEmpty()) {
            binding.imgPreviewQr.setVisibility(View.VISIBLE);
            Bitmap qrBitmap = BarcodePreviewHelper.generateQrCode(receipt.getQrCodeValue().trim(), 200, 200);
            if (qrBitmap != null) binding.imgPreviewQr.setImageBitmap(qrBitmap);
        } else {
            binding.imgPreviewQr.setVisibility(View.GONE);
        }
    }

    private void printReceiptNow() {
        syncModelFromUi();
        config = PrinterConfig.load(requireContext());
        String textToPrint = ReceiptFormatter.formatReceipt(receipt, config);

        progressDialog.show("Printing Receipt", "Connecting to " + config.getConnectionType() + "…");

        PrinterExecutor.printFormattedText(
                requireContext(),
                textToPrint,
                "Receipt " + receipt.getReceiptNo(),
                config,
                new PrinterExecutor.PrintCallback() {
                    @Override
                    public void onStart() {}

                    @Override
                    public void onProgress(String message) {
                        progressDialog.updateStep(message);
                    }

                    @Override
                    public void onSuccess(String message) {
                        progressDialog.dismiss();
                        PrintProgressDialog.showSuccess(requireContext(), message);
                    }

                    @Override
                    public void onError(String errorMessage, Throwable error) {
                        progressDialog.dismiss();
                        PrintProgressDialog.showError(requireContext(), "Print Error", errorMessage);
                    }
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
