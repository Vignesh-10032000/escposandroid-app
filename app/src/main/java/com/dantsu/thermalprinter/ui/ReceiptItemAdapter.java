package com.dantsu.thermalprinter.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dantsu.thermalprinter.R;
import com.dantsu.thermalprinter.model.ReceiptItem;

import java.util.List;

public class ReceiptItemAdapter extends RecyclerView.Adapter<ReceiptItemAdapter.ViewHolder> {

    public interface OnItemDeletedListener {
        void onItemDeleted(int position);
    }

    private final List<ReceiptItem> items;
    private final String currencySymbol;
    private final OnItemDeletedListener listener;

    public ReceiptItemAdapter(List<ReceiptItem> items, String currencySymbol, OnItemDeletedListener listener) {
        this.items = items;
        this.currencySymbol = currencySymbol;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_receipt_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReceiptItem item = items.get(position);
        holder.textItemName.setText(item.getName());
        holder.textItemQtyAndPrice.setText(item.getQuantity() + " x " + item.getFormattedPrice(currencySymbol));
        holder.textItemSubtotal.setText(item.getFormattedSubtotal(currencySymbol));

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onItemDeleted(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textItemName;
        TextView textItemQtyAndPrice;
        TextView textItemSubtotal;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textItemName = itemView.findViewById(R.id.textItemName);
            textItemQtyAndPrice = itemView.findViewById(R.id.textItemQtyAndPrice);
            textItemSubtotal = itemView.findViewById(R.id.textItemSubtotal);
            btnDelete = itemView.findViewById(R.id.btnDeleteItem);
        }
    }
}
