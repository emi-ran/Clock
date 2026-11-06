// SPDX-License-Identifier: GPL-3.0-only
package com.best.deskclock.alarms;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.best.deskclock.R;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.CameraPreview;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.BarcodeView;

import java.util.List;

/** In-activity scanner: no external capture activity can dismiss the ringing screen. */
final class BarcodeMissionScanner {
    interface Callback { void onMatched(); void onFallback(); }
    private static final int CAMERA_REQUEST = 7031;
    private final AlarmActivity activity;
    private final Callback callback;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AlertDialog dialog;
    private BarcodeView scanner;
    private String format, value;
    private boolean active, resumed;

    BarcodeMissionScanner(AlarmActivity activity, Callback callback) {
        this.activity = activity;
        this.callback = callback;
    }

    boolean isActive() { return active; }

    void start(String expectedFormat, String expectedValue) {
        if (active) return;
        active = true;
        format = expectedFormat;
        value = expectedValue;
        if (format == null || format.isEmpty() || value == null || value.isEmpty()) {
            fallback();
        } else if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            try {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
            } catch (RuntimeException e) { fallback(); }
        } else {
            showScanner();
        }
    }

    void onPermissionResult(int requestCode, int[] results) {
        if (requestCode != CAMERA_REQUEST || !active) return;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) showScanner();
        else fallback();
    }

    private void showScanner() {
        if (!active || activity.isFinishing()) return;
        try {
            LinearLayout layout = new LinearLayout(activity);
            layout.setOrientation(LinearLayout.VERTICAL);
            TextView instruction = new TextView(activity);
            instruction.setText(R.string.barcode_mission_instruction);
            int padding = (int) (16 * activity.getResources().getDisplayMetrics().density);
            instruction.setPadding(padding, padding, padding, padding);
            layout.addView(instruction);
            scanner = new BarcodeView(activity);
            scanner.addStateListener(new CameraPreview.StateListener() {
                @Override public void previewSized() { }
                @Override public void previewStarted() { }
                @Override public void previewStopped() { }
                @Override public void cameraError(Exception error) { fallback(); }
                @Override public void cameraClosed() { }
            });
            layout.addView(scanner, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (300 * activity.getResources().getDisplayMetrics().density)));
            dialog = new AlertDialog.Builder(activity).setTitle(R.string.barcode_mission_scan)
                .setView(layout).setNegativeButton(android.R.string.cancel, (d, w) -> close())
                .setNeutralButton(R.string.barcode_mission_fallback, (d, w) -> fallback()).create();
            dialog.setOnCancelListener(d -> close());
            dialog.setOnDismissListener(d -> { if (active) close(); });
            dialog.show();
            scanner.decodeContinuous(new BarcodeCallback() {
                @Override public void barcodeResult(BarcodeResult result) {
                    if (active && result != null && result.getBarcodeFormat() != null &&
                            BarcodeMission.matches(format, value, result.getBarcodeFormat().toString(), result.getText())) {
                        close();
                        callback.onMatched();
                    } else if (active) {
                        instruction.setText(R.string.barcode_mission_mismatch);
                    }
                }
                @Override public void possibleResultPoints(List<ResultPoint> points) { }
            });
            if (resumed) scanner.resume();
        } catch (RuntimeException e) { fallback(); }
    }

    void onResume() {
        resumed = true;
        if (active && scanner != null) {
            try { scanner.resume(); } catch (RuntimeException e) { fallback(); }
        }
    }
    void onPause() {
        resumed = false;
        if (scanner != null) scanner.pause();
    }
    private void fallback() {
        if (!active) return;
        close();
        // Post so the scanner dialog has finished dismissing before math opens.
        handler.post(callback::onFallback);
    }
    void close() {
        active = false;
        if (scanner != null) { scanner.pause(); scanner = null; }
        if (dialog != null) { AlertDialog old = dialog; dialog = null; old.dismiss(); }
    }
}
