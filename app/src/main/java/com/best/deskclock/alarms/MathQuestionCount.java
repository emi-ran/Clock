// SPDX-License-Identifier: GPL-3.0-only
package com.best.deskclock.alarms;

/** Shared limit for user input and untrusted persisted alarm data. */
public final class MathQuestionCount {
    public static final int MAX = 10;
    private MathQuestionCount() { }

    public static int clamp(int count) {
        return Math.max(1, Math.min(MAX, count));
    }

    public static int fallback(int count) {
        return Math.min(MAX, Math.max(3, clamp(count) + 1));
    }
}
