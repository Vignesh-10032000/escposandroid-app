package com.dantsu.thermalprinter.core;

import android.graphics.Bitmap;
import android.graphics.Color;

public class DitheringHelper {

    /**
     * Applies Floyd-Steinberg error diffusion dithering to a bitmap
     * Converts RGB to 1-bit monochrome (Black & White) with smooth gray shading
     */
    public static Bitmap floydSteinbergDither(Bitmap source, float contrast, boolean invert) {
        int width = source.getWidth();
        int height = source.getHeight();

        // Extract grayscale values into a 2D float array
        float[][] gray = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = source.getPixel(x, y);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                // Standard Luminance formula
                float luminance = (0.299f * r + 0.587f * g + 0.114f * b);

                // Apply contrast
                luminance = (((luminance / 255.0f - 0.5f) * contrast) + 0.5f) * 255.0f;
                luminance = Math.max(0, Math.min(255, luminance));

                if (invert) {
                    luminance = 255.0f - luminance;
                }
                gray[y][x] = luminance;
            }
        }

        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Floyd-Steinberg diffusion
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float oldPixel = gray[y][x];
                float newPixel = oldPixel < 128 ? 0 : 255;
                float error = oldPixel - newPixel;

                gray[y][x] = newPixel;
                output.setPixel(x, y, newPixel == 0 ? Color.BLACK : Color.WHITE);

                if (x + 1 < width) {
                    gray[y][x + 1] += error * (7.0f / 16.0f);
                }
                if (x - 1 >= 0 && y + 1 < height) {
                    gray[y + 1][x - 1] += error * (3.0f / 16.0f);
                }
                if (y + 1 < height) {
                    gray[y + 1][x] += error * (5.0f / 16.0f);
                }
                if (x + 1 < width && y + 1 < height) {
                    gray[y + 1][x + 1] += error * (1.0f / 16.0f);
                }
            }
        }

        return output;
    }

    /**
     * Applies simple threshold binarization
     */
    public static Bitmap thresholdBinarize(Bitmap source, int threshold, boolean invert) {
        int width = source.getWidth();
        int height = source.getHeight();
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = source.getPixel(x, y);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                int luminance = (int) (0.299f * r + 0.587f * g + 0.114f * b);

                if (invert) {
                    luminance = 255 - luminance;
                }

                int color = (luminance < threshold) ? Color.BLACK : Color.WHITE;
                output.setPixel(x, y, color);
            }
        }
        return output;
    }
}
