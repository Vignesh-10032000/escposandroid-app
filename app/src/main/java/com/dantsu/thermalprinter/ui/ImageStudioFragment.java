package com.dantsu.thermalprinter.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dantsu.thermalprinter.R;
import com.dantsu.thermalprinter.core.DitheringHelper;
import com.dantsu.thermalprinter.core.PrinterExecutor;
import com.dantsu.thermalprinter.databinding.FragmentImageStudioBinding;
import com.dantsu.thermalprinter.model.PrinterConfig;

import java.io.InputStream;
import java.util.Locale;

public class ImageStudioFragment extends Fragment {

    private FragmentImageStudioBinding binding;
    private PrinterConfig config;
    private PrintProgressDialog progressDialog;

    private Bitmap originalBitmap = null;
    private Bitmap processedBitmap = null;

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleGalleryResult
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentImageStudioBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context context = requireContext();
        config = PrinterConfig.load(context);
        progressDialog = new PrintProgressDialog(context);

        setupControls();
        loadPresetLogo();
    }

    private void setupControls() {
        binding.btnPickGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        binding.btnLoadPresetLogo.setOnClickListener(v -> loadPresetLogo());

        binding.radioGroupDitherMode.setOnCheckedChangeListener((group, checkedId) -> applyDithering());

        binding.sliderContrast.addOnChangeListener((slider, value, fromUser) -> {
            binding.textContrastLabel.setText(String.format(Locale.US, "Contrast Boost: %.1fx", value));
            applyDithering();
        });

        binding.switchInvertImage.setOnCheckedChangeListener((buttonView, isChecked) -> applyDithering());

        binding.btnPrintImageDirect.setOnClickListener(v -> {
            if (processedBitmap == null) {
                PrintProgressDialog.showError(requireContext(), "No Image", "Please select or load an image first.");
                return;
            }

            config = PrinterConfig.load(requireContext());
            progressDialog.show("Printing Image", "Sending raster data to " + config.getConnectionType() + "…");

            PrinterExecutor.printImage(
                    requireContext(),
                    processedBitmap,
                    "Image Print Job",
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

    private void handleGalleryResult(Uri uri) {
        if (uri == null) return;
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            Bitmap raw = BitmapFactory.decodeStream(is);
            if (is != null) is.close();

            if (raw != null) {
                originalBitmap = scaleToPrinterWidth(raw);
                applyDithering();
            }
        } catch (Exception e) {
            e.printStackTrace();
            PrintProgressDialog.showError(requireContext(), "Image Error", "Could not load image: " + e.getMessage());
        }
    }

    private void loadPresetLogo() {
        try {
            Bitmap raw = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
            if (raw != null) {
                originalBitmap = scaleToPrinterWidth(raw);
                applyDithering();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bitmap scaleToPrinterWidth(Bitmap src) {
        config = PrinterConfig.load(requireContext());
        int targetWidth = (config.getPaperWidthMM() == 80) ? 576 : 384;
        if (src.getWidth() <= targetWidth) {
            return src;
        }
        float ratio = (float) targetWidth / (float) src.getWidth();
        int targetHeight = (int) (src.getHeight() * ratio);
        // Ensure height doesn't exceed 256 for optimal ESC/POS slice compatibility
        if (targetHeight > 256) {
            float hRatio = 256.0f / (float) targetHeight;
            targetHeight = 256;
            targetWidth = (int) (targetWidth * hRatio);
        }
        return Bitmap.createScaledBitmap(src, targetWidth, targetHeight, true);
    }

    private void applyDithering() {
        if (originalBitmap == null || binding == null) return;

        float contrast = binding.sliderContrast.getValue();
        boolean invert = binding.switchInvertImage.isChecked();
        boolean useFloydSteinberg = binding.radioFloydSteinberg.isChecked();

        if (useFloydSteinberg) {
            processedBitmap = DitheringHelper.floydSteinbergDither(originalBitmap, contrast, invert);
        } else {
            int threshold = (int) (128 * (2.0f - contrast));
            threshold = Math.max(30, Math.min(225, threshold));
            processedBitmap = DitheringHelper.thresholdBinarize(originalBitmap, threshold, invert);
        }

        binding.imgThermalPreview.setImageBitmap(processedBitmap);
        binding.textImageDimensionsBadge.setText(
                "THERMAL BITMAP: " + processedBitmap.getWidth() + "x" + processedBitmap.getHeight() + " px (" +
                (useFloydSteinberg ? "Floyd-Steinberg" : "Threshold") + ")"
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
