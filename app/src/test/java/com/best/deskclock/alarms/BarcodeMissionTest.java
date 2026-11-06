package com.best.deskclock.alarms;

/** SDK-free regression checks; run with javac and java -ea. */
public final class BarcodeMissionTest {
    public static void main(String[] args) {
        check(BarcodeMission.matches("QR_CODE", "secret", "QR_CODE", "secret"));
        check(!BarcodeMission.matches("QR_CODE", "secret", "QR_CODE", "Secret"));
        check(!BarcodeMission.matches("QR_CODE", "secret", "CODE_128", "secret"));
        check(!BarcodeMission.matches(null, "secret", "QR_CODE", "secret"));
        check(!BarcodeMission.matches("QR_CODE", "", "QR_CODE", ""));
        check(!BarcodeMission.matches("QR_CODE", "secret", null, null));
        check(BarcodeMission.matches("EAN_13", "0012345678905", "EAN_13", "0012345678905"));
        check(!BarcodeMission.matches("EAN_13", "0012345678905", "EAN_13", "12345678905"));
    }

    private static void check(boolean result) {
        if (!result) throw new AssertionError("Barcode mission match regression");
    }
}
