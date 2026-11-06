package com.dantsu.thermalprinter.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Receipt implements Serializable {
    private String storeName = "COFFEE & BAKE SHOP";
    private String tagline = "Artisanal Brews & Pastries";
    private String address = "124 Innovation Way, Tech Park";
    private String phone = "+1 (555) 789-2045";
    private String taxId = "TAX ID: 84-729104-A";

    private String receiptNo = "INV-1049";
    private String cashierName = "Alex M.";
    private String customerName = "John Doe";
    private String customerPhone = "+1 (555) 321-9988";
    private Date timestamp = new Date();

    private String currencySymbol = "$";
    private List<ReceiptItem> items = new ArrayList<>();
    private double taxRatePercent = 8.25;
    private double discountAmount = 2.00;
    private String paymentMethod = "Credit Card (Chip & PIN)";

    private String footerMessage = "Thank you for visiting!\nPlease keep receipt for exchanges.";
    private boolean includeBarcode = true;
    private String barcodeValue = "104982341029"; // 12 digits for EAN13
    private boolean includeQrCode = true;
    private String qrCodeValue = "https://example.com/pay/inv-1049";

    public Receipt() {
        // Default sample items
        items.add(new ReceiptItem("Caramel Macchiato (L)", 1, 5.50));
        items.add(new ReceiptItem("Butter Croissant", 2, 3.25));
        items.add(new ReceiptItem("Avocado Toast", 1, 9.00));
        items.add(new ReceiptItem("Espresso Double Shot", 1, 3.50));
    }

    public double getSubtotal() {
        double total = 0.0;
        for (ReceiptItem item : items) {
            total += item.getSubtotal();
        }
        return total;
    }

    public double getTaxAmount() {
        double subtotalAfterDiscount = Math.max(0, getSubtotal() - discountAmount);
        return subtotalAfterDiscount * (taxRatePercent / 100.0);
    }

    public double getGrandTotal() {
        double subtotalAfterDiscount = Math.max(0, getSubtotal() - discountAmount);
        return subtotalAfterDiscount + getTaxAmount();
    }

    // Getters and Setters
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }

    public String getReceiptNo() { return receiptNo; }
    public void setReceiptNo(String receiptNo) { this.receiptNo = receiptNo; }

    public String getCashierName() { return cashierName; }
    public void setCashierName(String cashierName) { this.cashierName = cashierName; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getCurrencySymbol() { return currencySymbol; }
    public void setCurrencySymbol(String currencySymbol) { this.currencySymbol = currencySymbol; }

    public List<ReceiptItem> getItems() { return items; }
    public void setItems(List<ReceiptItem> items) { this.items = items; }

    public double getTaxRatePercent() { return taxRatePercent; }
    public void setTaxRatePercent(double taxRatePercent) { this.taxRatePercent = taxRatePercent; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getFooterMessage() { return footerMessage; }
    public void setFooterMessage(String footerMessage) { this.footerMessage = footerMessage; }

    public boolean isIncludeBarcode() { return includeBarcode; }
    public void setIncludeBarcode(boolean includeBarcode) { this.includeBarcode = includeBarcode; }

    public String getBarcodeValue() { return barcodeValue; }
    public void setBarcodeValue(String barcodeValue) { this.barcodeValue = barcodeValue; }

    public boolean isIncludeQrCode() { return includeQrCode; }
    public void setIncludeQrCode(boolean includeQrCode) { this.includeQrCode = includeQrCode; }

    public String getQrCodeValue() { return qrCodeValue; }
    public void setQrCodeValue(String qrCodeValue) { this.qrCodeValue = qrCodeValue; }
}
