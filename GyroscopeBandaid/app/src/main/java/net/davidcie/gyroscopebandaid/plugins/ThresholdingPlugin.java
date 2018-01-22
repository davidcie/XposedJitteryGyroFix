package net.davidcie.gyroscopebandaid.plugins;

import net.davidcie.gyroscopebandaid.EnginePreferences;

public class ThresholdingPlugin implements IEnginePlugin {
    @Override
    public void processReading(EnginePreferences preferences, float[][] history, float[] newReading) {
        if (!preferences.thresholding_enabled) return;
        boolean isZero = true;
        for (float axis : newReading) isZero &= axis == 0.0f;
        if (isZero && !preferences.absoluteMode) return;

        for (int a = 0; a < EnginePreferences.AXES; a++) {
            float newReadingAbs = Math.abs(newReading[a]);

            if (preferences.thresholding_static > 0.0f && !preferences.absoluteMode) {
                if (newReadingAbs < preferences.thresholding_static) {
                    newReading[a] = 0.0f;
                }
            } else if (preferences.thresholding_static > 0.0f && preferences.absoluteMode && history[a].length > 1) {
                float oldReadingAbs = Math.abs(history[a][1]);
                if (Math.abs(newReadingAbs - oldReadingAbs) < preferences.thresholding_static) {
                    newReading[a] = history[a][1];
                }
            } else if (preferences.thresholding_dynamic > 0.0f && !preferences.absoluteMode && history[a].length > 1) {
                float oldReadingAbs = Math.abs(history[a][1]);
                if (Math.abs(newReadingAbs - oldReadingAbs) < preferences.thresholding_dynamic) {
                    newReading[a] = 0.0f;
                }
            } else if (preferences.thresholding_dynamic > 0.0f && preferences.absoluteMode && history[a].length > 1) {
                float oldReadingAbs = Math.abs(history[a][1]);
                if (Math.abs(newReadingAbs - oldReadingAbs) < preferences.thresholding_dynamic) {
                    newReading[a] = history[a][1];
                }
            }
        }
    }
}
