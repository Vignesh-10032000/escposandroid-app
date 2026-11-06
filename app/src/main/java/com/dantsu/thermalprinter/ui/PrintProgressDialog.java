package com.dantsu.thermalprinter.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.dantsu.thermalprinter.R;

public class PrintProgressDialog {

    private final Dialog dialog;
    private final TextView textTitle;
    private final TextView textStep;

    public PrintProgressDialog(Context context) {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_print_progress, null);
        dialog.setContentView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setCancelable(false);

        textTitle = view.findViewById(R.id.textProgressTitle);
        textStep = view.findViewById(R.id.textProgressStep);
    }

    public void show(String title, String initialStep) {
        textTitle.setText(title);
        textStep.setText(initialStep);
        dialog.show();
    }

    public void updateStep(String step) {
        textStep.setText(step);
    }

    public void dismiss() {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public static void showError(Context context, String title, String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("OK", null)
                .show();
    }

    public static void showSuccess(Context context, String message) {
        new AlertDialog.Builder(context)
                .setTitle("Print Successful")
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("Done", null)
                .show();
    }
}
