package com.dantsu.thermalprinter.core;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * High-performance bitmap dithering for ESC/POS thermal printing.
 * Uses bulk getPixels()/setPixels() instead of per-pixel JNI calls for 100-300× speedup.
 */
public class DitheringHelper {

    /**
     * Applies Floyd-Steinberg error diffusion dithering to a bitmap.
     * Converts RGB to 1-bit monochrome (Black & White) with smooth gray shading.
     * Uses bulk array operations for maximum performance.
     */
    public static Bitmap floydSteinbergDither(Bitmap source, float contrast, boolean invert) {
        int width = source.getWidth();
        int height = source.getHeight();

        // --- OPTIMIZATION: Bulk pixel read instead of per-pixel getPixel() ---
        int[] srcPixels = new int[width * height];
        source.getPixels(srcPixels, 0, width, 0, 0, width, height);

        // Extract and adjust grayscale values into a float array
        float[] gray = new float[width * height];
        for (int i = 0; i < srcPixels.length; i++) {
            int pixel = srcPixels[i];
            float r = Color.red(pixel);
            float g = Color.green(pixel);
            float b = Color.blue(pixel);
            // Rec. 709 luminance formula
            float luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b;
            // Apply contrast boost around midpoint
            luminance = (((luminance / 255.0f) - 0.5f) * contrast + 0.5f) * 255.0f;
            luminance = Math.max(0f, Math.min(255f, luminance));
            if (invert) luminance = 255.0f - luminance;
            gray[i] = luminance;
        }

        // Floyd-Steinberg error diffusion (in-place on the flat array)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = y * width + x;
                float oldPixel = gray[idx];
                float newPixel = oldPixel < 128f ? 0f : 255f;
                float error = oldPixel - newPixel;
                gray[idx] = newPixel;

                // Distribute error to right, bottom-left, bottom, bottom-right neighbours
                if (x + 1 < width)
                    gray[idx + 1] = Math.max(0f, Math.min(255f, gray[idx + 1] + error * (7f / 16f)));
                if (y + 1 < height) {
                    if (x - 1 >= 0)
                        gray[idx + width - 1] = Math.max(0f, Math.min(255f, gray[idx + width - 1] + error * (3f / 16f)));
                    gray[idx + width] = Math.max(0f, Math.min(255f, gray[idx + width] + error * (5f / 16f)));
                    if (x + 1 < width)
                        gray[idx + width + 1] = Math.max(0f, Math.min(255f, gray[idx + width + 1] + error * (1f / 16f)));
                }
            }
        }

        // --- OPTIMIZATION: Bulk pixel write instead of per-pixel setPixel() ---
        int[] outPixels = new int[width * height];
        for (int i = 0; i < gray.length; i++) {
            outPixels[i] = gray[i] < 128f ? Color.BLACK : Color.WHITE;
        }
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565); // RGB_565 uses 50% less memory for B&W
        output.setPixels(outPixels, 0, width, 0, 0, width, height);
        return output;
    }

    /**
     * Applies simple threshold binarization.
     * Optimized with bulk array operations.
     */
    public static Bitmap thresholdBinarize(Bitmap source, int threshold, boolean invert) {
        int width = source.getWidth();
        int height = source.getHeight();

        int[] srcPixels = new int[width * height];
        source.getPixels(srcPixels, 0, width, 0, 0, width, height);

        int[] outPixels = new int[width * height];
        for (int i = 0; i < srcPixels.length; i++) {
            int pixel = srcPixels[i];
            int luminance = (int) (0.2126f * Color.red(pixel)
                    + 0.7152f * Color.green(pixel)
                    + 0.0722f * Color.blue(pixel));
            if (invert) luminance = 255 - luminance;
            outPixels[i] = (luminance < threshold) ? Color.BLACK : Color.WHITE;
        }

        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        output.setPixels(outPixels, 0, width, 0, 0, width, height);
        return output;
    }
}
