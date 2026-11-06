package com.best.deskclock.alarms;

/** Exact matching for a scanned mission barcode, including its symbology. */
public final class BarcodeMission {
    private BarcodeMission() { }

    public static boolean matches(String expectedFormat, String expectedValue,
                                  String scannedFormat, String scannedValue) {
        return expectedFormat != null && !expectedFormat.isEmpty()
                && expectedValue != null && !expectedValue.isEmpty()
                && expectedFormat.equals(scannedFormat) && expectedValue.equals(scannedValue);
    }
}
