# ESC/POS Thermal Print Studio for Android

A full-fledged, modern Android ESC/POS thermal printer application built on top of the **DantSu ESCPOS-ThermalPrinter-Android** engine, upgraded for Android 14+ (SDK 34), Gradle 8.7, and Android Gradle Plugin 8.3.2.

---

## ✨ Features & Architecture

### 1. 🧾 POS Receipt Generator & What-You-See-Is-What-You-Print Simulation
- **Store Branding:** Custom Store Name, Tagline, Phone, Address, Tax/VAT ID, and Currency Symbol ($ / € / £ / ₹ / etc.).
- **Invoice Metadata:** Receipt #, Cashier Name, Customer Name, and Payment Method (Card, Cash, Mobile, UPI).
- **Dynamic Line Items:** Add, edit, and delete billed items with real-time subtotal calculations. Preloaded fast presets (Espresso, Cappuccino, Croissant, Sandwich, Pizza).
- **Automatic Accounting:** Real-time Subtotal, Tax Rate %, Discount Amount, and Grand Total computation.
- **Live Thermal Receipt Preview:** Interactive on-screen monospace simulation with receipt card styling, tear edges, and live-rendered ZXing barcode & QR code previews.
- **One-Tap Print:** Direct printing via the active connection with real-time status feedback.

### 2. ⚡ ESC/POS Tag Editor & Business Presets
- **Interactive Syntax Editor:** Direct multi-line formatting editor for all DantSu tags (`[L]`, `[C]`, `[R]`, `<b>`, `<u>`, `<u type='double'>`, `<font size='tall|wide|big'>`, `<font color='bg-black'>`, `<img>`, `<barcode>`, `<qrcode>`).
- **Quick Insert Snippets Toolbar:** One-tap insertion of alignment tags, 2-column tables, 3-column tables, bold/underline styles, barcodes, QR codes, and dividers at the cursor position.
- **Ready-to-Use Business Templates:**
  - ☕ **Cafe & Coffee Shop Bill**
  - 🛍️ **Retail Store Invoice**
  - 🚗 **Parking Lot Gate Ticket**
  - 🍳 **Kitchen Order Ticket (KOT / Chef Slip)**
  - ✨ **Full ESC/POS Style & Capability Showcase**
- **Live Monospace Preview:** Simulates output in real-time as you type or change templates.

### 3. 📊 Barcode & QR Code Studio
- **Supported Formats:**
  - **QR Code (2D):** Any URL, text, UPI payment address, or Wi-Fi configuration with customizable millimeter sizing.
  - **Code 128 (1D):** Alphanumeric string codes.
  - **EAN-13 (1D):** 12 or 13 digit standard retail barcode.
  - **EAN-8 (1D):** 7 or 8 digit compact barcode.
  - **UPC-A (1D):** 11 or 12 digit North American UPC barcode.
  - **UPC-E (1D):** 6 digit zero-suppressed UPC barcode.
- **On-Screen ZXing Rendering:** Visual preview of the generated barcode before sending to printer.
- **Positioning:** Configurable readable text position (`below`, `above`, or `none`).

### 4. 🖼️ Image & Logo Studio (Floyd-Steinberg Dithering)
- **Image Sources:** Pick photos from the Android Gallery or load built-in store logos.
- **Floyd-Steinberg Error Diffusion Dithering:** Converts photographic gradients and illustrations into high-contrast 1-bit monochrome thermal dots without losing details.
- **Strict Threshold Mode:** Clean binary black-and-white mode for crisp vector-like logos.
- **Controls:** Real-time Contrast Boost slider (0.5x – 2.5x) and Invert Colors (white-on-black).
- **Auto-Scale:** Automatically scales images to the active printer head resolution (384 px for 58mm, 576 px for 80mm).

### 5. 🔌 Connection Hub & Hardware Management
- **Multi-Interface Support:**
  - **Bluetooth:** Automatic pairing list, device chooser dialog, and modern Android 12+ runtime permissions (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`).
  - **TCP / Network (Wi-Fi / Ethernet):** IP address input, port configuration (standard 9100 / 9300), and integrated socket Ping / Connection test.
  - **USB OTG:** USB printer enumeration, automatic USB permission broadcast handling, and attach/detach event listeners.
- **Hardware Profiles:**
  - Paper Width: 58mm (384 dots, 32 CPL) vs 80mm (576 dots, 48 CPL).
  - Feed Paper Distance: 0 to 50 mm.
  - Auto-Cut Paper: Automatic paper cutter pulse after printing.
  - Cash Drawer Kick: Sends `ESC p 0 25 250` kick pulse to open cash drawer connected to printer.
  - Raster Mode: Standard GS v 0 vs legacy ESC * mode toggle.
- **Diagnostic Tools:**
  - Full Hardware Self-Test page print.
  - Feed paper 20mm test.
  - Cut paper test.
  - Open cash drawer pulse test.
  - **Print History & Re-Print:** Persistent log of recent print jobs with one-tap re-print.

---

## 🚀 Building & Running

### Prerequisites
- JDK 17 or JDK 21
- Android SDK with Platform 34 and Build-Tools 34

### Build via Command Line
```powershell
cd thermal-printer-app
.\gradlew assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Open in Android Studio
1. Open Android Studio.
2. Click **Open** and select the folder:
   `thermal-printer-app`
3. Click **Sync Project with Gradle Files**.
4. Select your connected Android device or emulator and click **Run**.
