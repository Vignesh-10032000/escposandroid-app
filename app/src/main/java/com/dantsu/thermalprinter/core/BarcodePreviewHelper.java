package com.dantsu.thermalprinter.core;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Barcode and QR code preview generator.
 * Includes an LRU-style last-result cache and async generation to keep the UI thread free.
 */
public class BarcodePreviewHelper {

    // --- Cache: last generated result to avoid re-encoding unchanged input ---
    private static String lastQrData = null;
    private static int lastQrW, lastQrH;
    private static Bitmap lastQrBitmap = null;

    private static String lastBarData = null;
    private static BarcodeFormat lastBarFormat = null;
    private static int lastBarW, lastBarH;
    private static Bitmap lastBarBitmap = null;

    private static final ExecutorService barcodeExecutor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface BitmapCallback {
        void onResult(Bitmap bitmap);
    }

    // --- Async variant (for use in TextWatcher / UI callbacks) ---

    public static void generateQrCodeAsync(String data, int widthPx, int heightPx, BitmapCallback cb) {
        // Return cached result immediately if input is unchanged
        if (Objects.equals(data, lastQrData) && lastQrW == widthPx && lastQrH == heightPx && lastQrBitmap != null) {
            cb.onResult(lastQrBitmap);
            return;
        }
        barcodeExecutor.execute(() -> {
            Bitmap result = generateQrCode(data, widthPx, heightPx);
            lastQrData = data;
            lastQrW = widthPx;
            lastQrH = heightPx;
            lastQrBitmap = result;
            mainHandler.post(() -> cb.onResult(result));
        });
    }

    public static void generateBarcodeAsync(String data, BarcodeFormat format, int widthPx, int heightPx, BitmapCallback cb) {
        if (Objects.equals(data, lastBarData) && lastBarFormat == format
                && lastBarW == widthPx && lastBarH == heightPx && lastBarBitmap != null) {
            cb.onResult(lastBarBitmap);
            return;
        }
        barcodeExecutor.execute(() -> {
            Bitmap result = generateBarcode(data, format, widthPx, heightPx);
            lastBarData = data;
            lastBarFormat = format;
            lastBarW = widthPx;
            lastBarH = heightPx;
            lastBarBitmap = result;
            mainHandler.post(() -> cb.onResult(result));
        });
    }

    // --- Synchronous variants (for use in background threads) ---

    public static Bitmap generateQrCode(String data, int widthPx, int heightPx) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, widthPx, heightPx, hints);
            return bitMatrixToBitmap(matrix);
        } catch (Exception e) {
            return null;
        }
    }

    public static Bitmap generateBarcode(String data, BarcodeFormat format, int widthPx, int heightPx) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new MultiFormatWriter().encode(data, format, widthPx, heightPx, hints);
            return bitMatrixToBitmap(matrix);
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap bitMatrixToBitmap(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }
}
