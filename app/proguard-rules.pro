# OPT-06: ProGuard / R8 keep rules for ESC/POS Thermal Printer App

# Keep the DantSu EscPosPrinter library classes (reflection/native interop)
-keep class com.dantsu.escposprinter.** { *; }

# Keep models for JSON serialization (PrintHistoryStore uses org.json manually, no keep needed)
-keep class com.dantsu.thermalprinter.model.** { *; }

# Keep Bluetooth / USB connection wrappers used reflectively by Android framework
-keep class com.dantsu.escposprinter.connection.** { *; }

# Keep USB device parceling
-keep class android.hardware.usb.** { *; }

# ZXing barcode library
-keep class com.google.zxing.** { *; }

# AndroidX / Material
-dontwarn androidx.**
-keep class androidx.** { *; }
-keep class com.google.android.material.** { *; }
