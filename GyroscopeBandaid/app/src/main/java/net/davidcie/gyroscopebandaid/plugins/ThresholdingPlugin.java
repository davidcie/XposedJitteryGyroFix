package net.davidcie.gyroscopebandaid.plugins;

import android.content.SharedPreferences;

import net.davidcie.gyroscopebandaid.Const;
import net.davidcie.gyroscopebandaid.Util;

public class ThresholdingPlugin implements IEnginePlugin {

    private boolean mAbsoluteMode;

    public ThresholdingPlugin(boolean absoluteMode) {
        mAbsoluteMode = absoluteMode;
    }

    @Override
    public void processReading(SharedPreferences preferences, float[][] history, float[] newReading) {
        if (!preferences.getBoolean(Const.PREF_THRESHOLDING_ENABLED, false)) return;

        // If all axes are exactly zero, they will meet every threshold
        boolean isZero = true;
        for (float axis : newReading) isZero &= axis == 0.0f;
        if (isZero && !mAbsoluteMode) return;

        // Fetch values from preferences
        float thresholdStatic = preferences.getFloat(Const.PREF_THRESHOLDING_STATIC, Const.DEFAULT_THRESHOLD_STATIC);
        float thresholdDynamic = preferences.getFloat(Const.PREF_THRESHOLDING_DYNAMIC, Const.DEFAULT_THRESHOLD_DYNAMIC);

        for (int a = 0; a < Util.AXES; a++) {
            float newReadingAbs = Math.abs(newReading[a]);

            if (thresholdStatic > 0.0f && !mAbsoluteMode) {
                if (newReadingAbs < thresholdStatic) {
                    newReading[a] = 0.0f;
                }
            } else if (thresholdStatic > 0.0f && mAbsoluteMode && history[a].length > 1) {
                float oldReadingAbs = Math.abs(history[a][1]);
                if (Math.abs(newReadingAbs - oldReadingAbs) < thresholdStatic) {
                    newReading[a] = history[a][1];
                }
            } else if (thresholdDynamic > 0.0f && !mAbsoluteMode && history[a].length > 1) {
                float oldReadingAbs = Math.abs(history[a][1]);
                if (Math.abs(newReadingAbs - oldReadingAbs) < thresholdDynamic) {
                    newReading[a] = 0.0f;
                }
            } else if (thresholdDynamic > 0.0f && mAbsoluteMode && history[a].length > 1) {
                float oldReadingAbs = Math.abs(history[a][1]);
                if (Math.abs(newReadingAbs - oldReadingAbs) < thresholdDynamic) {
                    newReading[a] = history[a][1];
                }
            }
        }
    }
}
