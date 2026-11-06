package com.best.deskclock.alarms;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MathQuestionCountTest {
    @Test public void clampsUntrustedValuesAndPreservesValidCounts() {
        assertEquals(1, MathQuestionCount.clamp(Integer.MIN_VALUE));
        assertEquals(1, MathQuestionCount.clamp(0));
        assertEquals(1, MathQuestionCount.clamp(1));
        assertEquals(7, MathQuestionCount.clamp(7));
        assertEquals(10, MathQuestionCount.clamp(10));
        assertEquals(10, MathQuestionCount.clamp(Integer.MAX_VALUE));
    }
    @Test public void fallbackIsHarderButNeverExceedsLimit() {
        assertEquals(3, MathQuestionCount.fallback(1));
        assertEquals(8, MathQuestionCount.fallback(7));
        assertEquals(10, MathQuestionCount.fallback(10));
        assertEquals(10, MathQuestionCount.fallback(Integer.MAX_VALUE));
    }
}
