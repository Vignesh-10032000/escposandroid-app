package com.dantsu.thermalprinter.core;

import com.dantsu.thermalprinter.model.PrinterConfig;
import com.dantsu.thermalprinter.model.Receipt;
import com.dantsu.thermalprinter.model.ReceiptItem;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ReceiptFormatter {

    /**
     * Formats a complete POS receipt into DantSu ESC/POS formatted text syntax
     */
    public static String formatReceipt(Receipt receipt, PrinterConfig config) {
        StringBuilder sb = new StringBuilder();
        int charsPerLine = config.getCharsPerLine();
        String currency = receipt.getCurrencySymbol();

        // 1. Store Header
        sb.append("[C]<b><font size='big'>").append(receipt.getStoreName()).append("</font></b>\n");
        if (receipt.getTagline() != null && !receipt.getTagline().trim().isEmpty()) {
            sb.append("[C]<i>").append(receipt.getTagline()).append("</i>\n");
        }
        sb.append("[C]").append(receipt.getAddress()).append("\n");
        sb.append("[C]Tel: ").append(receipt.getPhone()).append("\n");
        if (receipt.getTaxId() != null && !receipt.getTaxId().trim().isEmpty()) {
            sb.append("[C]").append(receipt.getTaxId()).append("\n");
        }
        sb.append("[L]\n");

        // 2. Divider
        sb.append("[C]").append(getDivider("=", charsPerLine)).append("\n");

        // 3. Receipt metadata
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        sb.append("[L]<b>RECEIPT:</b> ").append(receipt.getReceiptNo())
          .append("[R]").append(sdf.format(receipt.getTimestamp())).append("\n");
        sb.append("[L]Cashier: ").append(receipt.getCashierName())
          .append("[R]Pay: ").append(receipt.getPaymentMethod()).append("\n");
        if (receipt.getCustomerName() != null && !receipt.getCustomerName().trim().isEmpty()) {
            sb.append("[L]Customer: ").append(receipt.getCustomerName()).append("\n");
        }

        // 4. Items table divider & header
        sb.append("[C]").append(getDivider("-", charsPerLine)).append("\n");
        sb.append("[L]<b>ITEM</b>[R]<b>TOTAL</b>\n");
        sb.append("[C]").append(getDivider("-", charsPerLine)).append("\n");

        // 5. Line items
        for (ReceiptItem item : receipt.getItems()) {
            sb.append("[L]<b>").append(item.getName()).append("</b>")
              .append("[R]").append(item.getFormattedSubtotal(currency)).append("\n");
            sb.append("[L]  ").append(item.getQuantity()).append(" x ").append(item.getFormattedPrice(currency)).append("\n");
        }

        // 6. Totals section
        sb.append("[C]").append(getDivider("-", charsPerLine)).append("\n");
        sb.append("[L]SUBTOTAL:[R]").append(String.format(Locale.US, "%s%.2f", currency, receipt.getSubtotal())).append("\n");

        if (receipt.getDiscountAmount() > 0) {
            sb.append("[L]DISCOUNT:[R]-").append(String.format(Locale.US, "%s%.2f", currency, receipt.getDiscountAmount())).append("\n");
        }

        if (receipt.getTaxRatePercent() > 0) {
            sb.append("[L]TAX (").append(String.format(Locale.US, "%.1f", receipt.getTaxRatePercent())).append("%):")
              .append("[R]").append(String.format(Locale.US, "%s%.2f", currency, receipt.getTaxAmount())).append("\n");
        }

        sb.append("[C]").append(getDivider("=", charsPerLine)).append("\n");
        sb.append("[L]<b><font size='tall'>TOTAL DUE:</font></b>")
          .append("[R]<b><font size='tall'>").append(String.format(Locale.US, "%s%.2f", currency, receipt.getGrandTotal()))
          .append("</font></b>\n");
        sb.append("[C]").append(getDivider("=", charsPerLine)).append("\n");
        sb.append("[L]\n");

        // 7. Footer note
        if (receipt.getFooterMessage() != null && !receipt.getFooterMessage().trim().isEmpty()) {
            String[] lines = receipt.getFooterMessage().split("\n");
            for (String line : lines) {
                sb.append("[C]").append(line.trim()).append("\n");
            }
            sb.append("[L]\n");
        }

        // 8. Barcode (e.g. 12 digits EAN13 or Code 128)
        if (receipt.isIncludeBarcode() && receipt.getBarcodeValue() != null && !receipt.getBarcodeValue().trim().isEmpty()) {
            String barcode = receipt.getBarcodeValue().trim();
            if (barcode.matches("\\d{12}")) {
                sb.append("[C]<barcode type='ean13' height='10'>").append(barcode).append("</barcode>\n");
            } else {
                sb.append("[C]<barcode type='128' height='10'>").append(barcode).append("</barcode>\n");
            }
            sb.append("[L]\n");
        }

        // 9. QR Code
        if (receipt.isIncludeQrCode() && receipt.getQrCodeValue() != null && !receipt.getQrCodeValue().trim().isEmpty()) {
            sb.append("[C]<qrcode size='22'>").append(receipt.getQrCodeValue().trim()).append("</qrcode>\n");
        }

        return sb.toString();
    }

    private static String getDivider(String charPattern, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(charPattern);
        }
        return sb.toString();
    }
}
