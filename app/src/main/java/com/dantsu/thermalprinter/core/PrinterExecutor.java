package com.dantsu.thermalprinter.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import com.dantsu.escposprinter.EscPosCharsetEncoding;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;
import com.dantsu.thermalprinter.model.PrintJobRecord;
import com.dantsu.thermalprinter.model.PrinterConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrinterExecutor {

    public interface PrintCallback {
        void onStart();
        void onProgress(String message);
        void onSuccess(String message);
        void onError(String errorMessage, Throwable error);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Prints formatted ESC/POS text
     */
    public static void printFormattedText(
            final Context context,
            final String textToPrint,
            final String jobTitle,
            final PrinterConfig config,
            final PrintCallback callback
    ) {
        if (callback != null) callback.onStart();

        executor.execute(() -> {
            postProgress(callback, "Establishing connection to printer…");

            DeviceConnection connection = PrinterConnectionManager.getInstance().getDeviceConnection(context, config);
            if (connection == null) {
                postError(callback, "No printer connection found. Please check Bluetooth, USB, or Network settings.", null);
                saveJobHistory(context, jobTitle, "Failed: No printer connection", config, false, textToPrint);
                return;
            }

            EscPosPrinter printer = null;
            try {
                EscPosCharsetEncoding encoding = new EscPosCharsetEncoding(config.getCharsetName(), config.getCharsetId());

                printer = new EscPosPrinter(
                        connection,
                        config.getPrinterDpi(),
                        (float) config.getPaperWidthMM(),
                        config.getCharsPerLine(),
                        encoding
                );

                if (config.isUseEscAsterisk()) {
                    printer.useEscAsteriskCommand(true);
                }

                postProgress(callback, "Sending print job…");

                if (config.isOpenCashDrawer()) {
                    // Open cash drawer and cut
                    printer.printFormattedTextAndOpenCashBox(textToPrint, config.getFeedPaperMM());
                } else if (config.isAutoCut()) {
                    printer.printFormattedTextAndCut(textToPrint, config.getFeedPaperMM());
                } else {
                    printer.printFormattedText(textToPrint, config.getFeedPaperMM());
                }

                saveJobHistory(context, jobTitle, "Print completed successfully", config, true, textToPrint);
                postSuccess(callback, "Print job completed successfully!");

            } catch (EscPosConnectionException e) {
                postError(callback, "Connection failed: " + e.getMessage(), e);
                saveJobHistory(context, jobTitle, "Connection failed: " + e.getMessage(), config, false, textToPrint);
            } catch (EscPosParserException e) {
                postError(callback, "Formatting syntax error: " + e.getMessage(), e);
                saveJobHistory(context, jobTitle, "Syntax error: " + e.getMessage(), config, false, textToPrint);
            } catch (EscPosEncodingException e) {
                postError(callback, "Charset encoding error: " + e.getMessage(), e);
                saveJobHistory(context, jobTitle, "Encoding error: " + e.getMessage(), config, false, textToPrint);
            } catch (EscPosBarcodeException e) {
                postError(callback, "Barcode syntax error: " + e.getMessage(), e);
                saveJobHistory(context, jobTitle, "Barcode error: " + e.getMessage(), config, false, textToPrint);
            } catch (Exception e) {
                postError(callback, "Printing error: " + e.getMessage(), e);
                saveJobHistory(context, jobTitle, "Error: " + e.getMessage(), config, false, textToPrint);
            } finally {
                if (printer != null) {
                    try {
                        printer.disconnectPrinter();
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    /**
     * Prints a standalone image bitmap
     */
    public static void printImage(
            final Context context,
            final Bitmap bitmap,
            final String jobTitle,
            final PrinterConfig config,
            final PrintCallback callback
    ) {
        if (callback != null) callback.onStart();

        executor.execute(() -> {
            postProgress(callback, "Preparing image and connecting…");

            DeviceConnection connection = PrinterConnectionManager.getInstance().getDeviceConnection(context, config);
            if (connection == null) {
                postError(callback, "No printer connection found.", null);
                return;
            }

            EscPosPrinter printer = null;
            try {
                printer = new EscPosPrinter(
                        connection,
                        config.getPrinterDpi(),
                        (float) config.getPaperWidthMM(),
                        config.getCharsPerLine()
                );

                if (config.isUseEscAsterisk()) {
                    printer.useEscAsteriskCommand(true);
                }

                postProgress(callback, "Converting image to ESC/POS raster…");
                String hexString = PrinterTextParserImg.bitmapToHexadecimalString(printer, bitmap, false);
                String formatted = "[C]<img>" + hexString + "</img>\n";

                postProgress(callback, "Printing image…");
                if (config.isAutoCut()) {
                    printer.printFormattedTextAndCut(formatted, config.getFeedPaperMM());
                } else {
                    printer.printFormattedText(formatted, config.getFeedPaperMM());
                }

                saveJobHistory(context, jobTitle, "Image print successful", config, true, "[Image]");
                postSuccess(callback, "Image printed successfully!");

            } catch (Exception e) {
                postError(callback, "Failed to print image: " + e.getMessage(), e);
                saveJobHistory(context, jobTitle, "Image print failed: " + e.getMessage(), config, false, "[Image]");
            } finally {
                if (printer != null) {
                    try {
                        printer.disconnectPrinter();
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    /**
     * Prints a hardware self-test diagnostic slip
     */
    public static void printDiagnosticTestPage(
            final Context context,
            final PrinterConfig config,
            final PrintCallback callback
    ) {
        int width = config.getCharsPerLine();
        StringBuilder test = new StringBuilder();
        test.append("[C]<b><font size='big'>PRINTER SELF-TEST</font></b>\n");
        test.append("[C]================================\n");
        test.append("[L]Profile:[R]").append(config.getPaperWidthMM()).append("mm (").append(width).append(" CPL)\n");
        test.append("[L]Resolution:[R]").append(config.getPrinterDpi()).append(" DPI\n");
        test.append("[L]Interface:[R]").append(config.getConnectionType()).append("\n");
        test.append("[L]Charset:[R]").append(config.getCharsetName()).append("\n");
        test.append("[C]--------------------------------\n");
        test.append("[L]Alignment Check:\n");
        test.append("[L]Left Align Text\n");
        test.append("[C]Center Align Text\n");
        test.append("[R]Right Align Text\n");
        test.append("[C]--------------------------------\n");
        test.append("[L]Styles & Weights:\n");
        test.append("[L]<b>Bold Typography</b>\n");
        test.append("[L]<u>Underlined Text</u>\n");
        test.append("[L]<font color='bg-black'>Inverted Black Background</font>\n");
        test.append("[L]<font size='tall'>Double Height</font>\n");
        test.append("[L]<font size='wide'>Double Width</font>\n");
        test.append("[L]<b><font size='big'>Big 2x2 Header</font></b>\n");
        test.append("[C]--------------------------------\n");
        test.append("[L]Barcodes & 2D QR:\n");
        test.append("[C]<barcode type='128' height='8'>TEST-128</barcode>\n");
        test.append("[L]\n");
        test.append("[C]<qrcode size='18'>https://github.com/dantsu/escpos-thermalprinter-android</qrcode>\n");
        test.append("[C]================================\n");
        test.append("[C]DIAGNOSTIC TEST COMPLETE\n");

        printFormattedText(context, test.toString(), "Self-Test Page", config, callback);
    }

    private static void postProgress(PrintCallback callback, String msg) {
        if (callback != null) mainHandler.post(() -> callback.onProgress(msg));
    }

    private static void postSuccess(PrintCallback callback, String msg) {
        if (callback != null) mainHandler.post(() -> callback.onSuccess(msg));
    }

    private static void postError(PrintCallback callback, String msg, Throwable t) {
        if (callback != null) mainHandler.post(() -> callback.onError(msg, t));
    }

    private static void saveJobHistory(Context context, String title, String details, PrinterConfig config, boolean success, String raw) {
        // Save to PrintHistoryStore
        PrintHistoryStore.addJob(context, new PrintJobRecord(
                title, details, config.getConnectionType(),
                config.getConnectionType().equals(PrinterConfig.CONNECTION_TCP) ? config.getTcpIp() : config.getBluetoothDeviceName(),
                success, raw
        ));
    }
}
