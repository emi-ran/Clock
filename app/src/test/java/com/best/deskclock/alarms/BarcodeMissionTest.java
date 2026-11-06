package com.best.deskclock.alarms;

import org.junit.Test;
import static org.junit.Assert.*;

public class BarcodeMissionTest {
    @Test public void requiresExactFormatAndValue() {
        assertTrue(BarcodeMission.matches("QR_CODE", "secret", "QR_CODE", "secret"));
        assertFalse(BarcodeMission.matches("QR_CODE", "secret", "QR_CODE", "Secret"));
        assertFalse(BarcodeMission.matches("QR_CODE", "secret", "CODE_128", "secret"));
        assertFalse(BarcodeMission.matches("EAN_13", "0012345678905", "EAN_13", "12345678905"));
    }
    @Test public void rejectsMissingValues() {
        assertFalse(BarcodeMission.matches(null, "secret", "QR_CODE", "secret"));
        assertFalse(BarcodeMission.matches("QR_CODE", "", "QR_CODE", ""));
        assertFalse(BarcodeMission.matches("QR_CODE", "secret", null, null));
    }
}
