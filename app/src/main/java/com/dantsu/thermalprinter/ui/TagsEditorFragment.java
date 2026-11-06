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
import com.dantsu.thermalprinter.core.PrinterExecutor;
import com.dantsu.thermalprinter.databinding.FragmentTagsEditorBinding;
import com.dantsu.thermalprinter.model.PrinterConfig;

public class TagsEditorFragment extends Fragment {

    private FragmentTagsEditorBinding binding;
    private PrinterConfig config;
    private PrintProgressDialog progressDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTagsEditorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context context = requireContext();
        config = PrinterConfig.load(context);
        progressDialog = new PrintProgressDialog(context);

        setupTemplates();
        setupInsertButtons();
        setupEditorWatcher();
        setupActionButtons();

        // Default template: Cafe Bill
        loadTemplateCafe();
    }

    private void setupTemplates() {
        binding.chipTemplateCafe.setOnClickListener(v -> loadTemplateCafe());
        binding.chipTemplateRetail.setOnClickListener(v -> loadTemplateRetail());
        binding.chipTemplateParking.setOnClickListener(v -> loadTemplateParking());
        binding.chipTemplateKot.setOnClickListener(v -> loadTemplateKot());
        binding.chipTemplateShowcase.setOnClickListener(v -> loadTemplateShowcase());
    }

    private void setupInsertButtons() {
        binding.btnInsertLeft.setOnClickListener(v -> insertSnippet("[L]"));
        binding.btnInsertCenter.setOnClickListener(v -> insertSnippet("[C]"));
        binding.btnInsertRight.setOnClickListener(v -> insertSnippet("[R]"));
        binding.btnInsertBold.setOnClickListener(v -> insertSnippet("<b>Text</b>"));
        binding.btnInsertUnderline.setOnClickListener(v -> insertSnippet("<u>Text</u>"));
        binding.btnInsertTall.setOnClickListener(v -> insertSnippet("<font size='tall'>Tall Text</font>"));
        binding.btnInsertBig.setOnClickListener(v -> insertSnippet("<font size='big'>Big Header</font>"));
        binding.btnInsertInvert.setOnClickListener(v -> insertSnippet("<font color='bg-black'>Inverted Text</font>"));
        binding.btnInsertBarcode.setOnClickListener(v -> insertSnippet("[C]<barcode type='128' height='10'>CODE128</barcode>\n"));
        binding.btnInsertQr.setOnClickListener(v -> insertSnippet("[C]<qrcode size='20'>https://dantsu.com/</qrcode>\n"));
        binding.btnInsertDivider.setOnClickListener(v -> insertSnippet("[C]--------------------------------\n"));
    }

    private void insertSnippet(String snippet) {
        int start = Math.max(binding.editFormattedText.getSelectionStart(), 0);
        int end = Math.max(binding.editFormattedText.getSelectionEnd(), 0);
        binding.editFormattedText.getText().replace(Math.min(start, end), Math.max(start, end), snippet, 0, snippet.length());
    }

    private void setupEditorWatcher() {
        binding.editFormattedText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                updateSimulationView(s.toString());
            }
        });

        binding.btnClearEditor.setOnClickListener(v -> binding.editFormattedText.setText(""));
    }

    private void setupActionButtons() {
        binding.btnPrintCustomTags.setOnClickListener(v -> {
            String text = binding.editFormattedText.getText().toString();
            if (text.trim().isEmpty()) {
                PrintProgressDialog.showError(requireContext(), "Empty Print Job", "Please enter some formatted ESC/POS text or select a template.");
                return;
            }
            config = PrinterConfig.load(requireContext());
            progressDialog.show("Printing Custom ESC/POS", "Connecting to " + config.getConnectionType() + "…");

            PrinterExecutor.printFormattedText(
                    requireContext(),
                    text,
                    "Custom Tag Job",
                    config,
                    new PrinterExecutor.PrintCallback() {
                        @Override public void onStart() {}
                        @Override public void onProgress(String message) { progressDialog.updateStep(message); }
                        @Override public void onSuccess(String message) {
                            progressDialog.dismiss();
                            PrintProgressDialog.showSuccess(requireContext(), message);
                        }
                        @Override public void onError(String errorMessage, Throwable error) {
                            progressDialog.dismiss();
                            PrintProgressDialog.showError(requireContext(), "Print Error", errorMessage);
                        }
                    }
            );
        });
    }

    private void updateSimulationView(String rawText) {
        String clean = rawText
                .replaceAll("\\[[LCR]\\]", "")
                .replaceAll("</?[bui]>", "")
                .replaceAll("<font[^>]*>", "")
                .replaceAll("</font>", "")
                .replaceAll("<u[^>]*>", "")
                .replaceAll("</u>", "")
                .replaceAll("<barcode[^>]*>(.*?)</barcode>", "[BARCODE: $1]")
                .replaceAll("<qrcode[^>]*>(.*?)</qrcode>", "[QR CODE: $1]")
                .replaceAll("<img>.*?</img>", "[BITMAP IMAGE]");
        binding.textEditorSimulation.setText(clean.trim());
    }

    private void loadTemplateCafe() {
        String s = "[C]<b><font size='big'>MOCHA &amp; CO.</font></b>\n" +
                "[C]42 Roaster Avenue, Downtown\n" +
                "[C]Tel: +1 (555) 019-2834\n" +
                "[L]\n" +
                "[C]================================\n" +
                "[L]<b>TABLE: 12</b>[R]Host: Sarah\n" +
                "[L]Receipt: #8912[R]2026-11-06 18:25\n" +
                "[C]--------------------------------\n" +
                "[L]1x Vanilla Latte[R]$4.50\n" +
                "[L]  + Extra Oat Milk[R]$0.75\n" +
                "[L]2x Blueberry Muffin[R]$6.00\n" +
                "[L]1x Sparkling Water[R]$2.25\n" +
                "[C]--------------------------------\n" +
                "[L]SUBTOTAL:[R]$13.50\n" +
                "[L]TAX (8%):[R]$1.08\n" +
                "[C]================================\n" +
                "[L]<b><font size='tall'>TOTAL DUE:</font></b>[R]<b><font size='tall'>$14.58</font></b>\n" +
                "[C]================================\n" +
                "[L]\n" +
                "[C]Free Guest Wi-Fi: MochaGuest\n" +
                "[C]Pass: espresso2026\n" +
                "[L]\n" +
                "[C]<qrcode size='20'>https://mochacoffee.example/survey</qrcode>\n";
        binding.editFormattedText.setText(s);
    }

    private void loadTemplateRetail() {
        String s = "[C]<b><font size='big'>NEXUS FASHION</font></b>\n" +
                "[C]Flagship Mall, Level 2\n" +
                "[L]\n" +
                "[C]================================\n" +
                "[L]INV: #NF-90812[R]2026-11-06\n" +
                "[L]Cashier: Marcus[R]REG: 04\n" +
                "[C]--------------------------------\n" +
                "[L]<b>VINTAGE DENIM JACKET</b>[R]$69.99\n" +
                "[L]  Size: L | Color: Blue\n" +
                "[L]<b>CLASSIC COTTON TEE</b>[R]$24.99\n" +
                "[L]  Size: M | Color: White\n" +
                "[C]--------------------------------\n" +
                "[L]SUBTOTAL:[R]$94.98\n" +
                "[L]DISCOUNT (10%):[R]-$9.50\n" +
                "[L]STATE TAX:[R]$7.26\n" +
                "[C]================================\n" +
                "[L]<b><font size='tall'>TOTAL PAID:</font></b>[R]<b><font size='tall'>$92.74</font></b>\n" +
                "[C]================================\n" +
                "[L]Payment: VISA ending in 4102\n" +
                "[L]\n" +
                "[C]<barcode type='ean13' height='10'>908124578129</barcode>\n" +
                "[L]\n" +
                "[C]Return Policy: 30 days with tags\n";
        binding.editFormattedText.setText(s);
    }

    private void loadTemplateParking() {
        String s = "[C]<b><font size='big'>CITY METRO PARKING</font></b>\n" +
                "[C]North Garage Terminal\n" +
                "[L]\n" +
                "[C]================================\n" +
                "[C]<b><font size='tall'>ENTRY TICKET</font></b>\n" +
                "[C]================================\n" +
                "[L]ENTRY DATE:[R]2026-11-06\n" +
                "[L]ENTRY TIME:[R]18:45:10\n" +
                "[L]LANE / GATE:[R]Gate 03 (South)\n" +
                "[L]RATE:[R]$3.00 / hour\n" +
                "[C]--------------------------------\n" +
                "[C]<barcode type='128' height='12'>TKT-890241</barcode>\n" +
                "[C]TKT-890241\n" +
                "[C]--------------------------------\n" +
                "[C]Please keep ticket in vehicle.\n" +
                "[C]Do not leave on dashboard.\n" +
                "[C]Lost ticket fee: $35.00\n";
        binding.editFormattedText.setText(s);
    }

    private void loadTemplateKot() {
        String s = "[C]<b><font size='big'>KITCHEN ORDER (KOT)</font></b>\n" +
                "[C]================================\n" +
                "[L]<b><font size='tall'>TABLE: 07</font></b>[R]<b>ORDER #44</b>\n" +
                "[L]Server: David[R]Time: 18:32\n" +
                "[C]================================\n" +
                "[L]<b><font size='tall'>2x WAGYU BURGER</font></b>\n" +
                "[L]   * Medium Rare\n" +
                "[L]   * Extra Cheddar, No Pickles\n" +
                "[L]\n" +
                "[L]<b><font size='tall'>1x TRUFFLE FRIES</font></b>\n" +
                "[L]   * Rosemary salt\n" +
                "[L]\n" +
                "[L]<b><font size='tall'>1x CAESAR SALAD</font></b>\n" +
                "[L]   * Dressing on the side\n" +
                "[C]================================\n" +
                "[C]<b>*** RUSH ORDER ***</b>\n";
        binding.editFormattedText.setText(s);
    }

    private void loadTemplateShowcase() {
        String s = "[C]<b><font size='big'>ESC/POS SHOWCASE</font></b>\n" +
                "[C]DantSu Thermal Printer Engine\n" +
                "[C]================================\n" +
                "[L]Left aligned text\n" +
                "[C]Center aligned text\n" +
                "[R]Right aligned text\n" +
                "[C]--------------------------------\n" +
                "[L]Column 1[R]Column 2\n" +
                "[L]Col A[C]Col B[R]Col C\n" +
                "[C]--------------------------------\n" +
                "[L]<b>Bold font style</b>\n" +
                "[L]<u>Single underline style</u>\n" +
                "[L]<u type='double'>Double underline style</u>\n" +
                "[L]<font color='bg-black'>Inverted black background</font>\n" +
                "[L]<font size='tall'>Tall font (2x height)</font>\n" +
                "[L]<font size='wide'>Wide font (2x width)</font>\n" +
                "[L]<b><font size='big'>Big 2x2 Header</font></b>\n" +
                "[C]--------------------------------\n" +
                "[C]<barcode type='ean13' height='10'>831254784551</barcode>\n" +
                "[L]\n" +
                "[C]<qrcode size='22'>https://github.com/dantsu/escpos-thermalprinter-android</qrcode>\n" +
                "[C]================================\n" +
                "[C]ALL FORMATS VERIFIED\n";
        binding.editFormattedText.setText(s);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
