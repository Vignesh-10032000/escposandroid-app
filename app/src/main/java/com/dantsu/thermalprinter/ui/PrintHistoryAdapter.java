package com.dantsu.thermalprinter.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.dantsu.thermalprinter.R;
import com.dantsu.thermalprinter.model.PrintJobRecord;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class PrintHistoryAdapter extends RecyclerView.Adapter<PrintHistoryAdapter.ViewHolder> {

    public interface OnReprintListener {
        void onReprint(PrintJobRecord record);
    }

    private final List<PrintJobRecord> records;
    private final OnReprintListener listener;

    public PrintHistoryAdapter(List<PrintJobRecord> records, OnReprintListener listener) {
        this.records = records;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PrintJobRecord record = records.get(position);
        holder.textTitle.setText(record.getTitle());
        holder.textDate.setText(record.getFormattedDate() + " • " + record.getConnectionType() + " (" + record.getTargetDevice() + ")");

        if (record.isSuccess()) {
            holder.textStatus.setText("● Success: " + record.getDetails());
            holder.textStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_green));
        } else {
            holder.textStatus.setText("● Failed: " + record.getDetails());
            holder.textStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_red));
        }

        holder.btnReprint.setOnClickListener(v -> {
            if (listener != null) listener.onReprint(record);
        });
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle;
        TextView textDate;
        TextView textStatus;
        MaterialButton btnReprint;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textHistoryTitle);
            textDate = itemView.findViewById(R.id.textHistoryDate);
            textStatus = itemView.findViewById(R.id.textHistoryStatus);
            btnReprint = itemView.findViewById(R.id.btnReprint);
        }
    }
}
