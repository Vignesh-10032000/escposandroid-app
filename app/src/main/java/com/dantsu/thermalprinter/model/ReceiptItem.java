package com.dantsu.thermalprinter.model;

import java.io.Serializable;
import java.util.Locale;

public class ReceiptItem implements Serializable {
    private String name;
    private int quantity;
    private double unitPrice;

    public ReceiptItem(String name, int quantity, double unitPrice) {
        this.name = name;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double getSubtotal() {
        return quantity * unitPrice;
    }

    public String getFormattedPrice(String currencySymbol) {
        return String.format(Locale.US, "%s%.2f", currencySymbol, unitPrice);
    }

    public String getFormattedSubtotal(String currencySymbol) {
        return String.format(Locale.US, "%s%.2f", currencySymbol, getSubtotal());
    }
}
