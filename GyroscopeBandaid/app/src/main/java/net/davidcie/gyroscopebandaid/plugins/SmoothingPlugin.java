package net.davidcie.gyroscopebandaid.plugins;

import net.davidcie.gyroscopebandaid.EnginePreferences;
import net.davidcie.gyroscopebandaid.Util;

import java.util.Arrays;

public class SmoothingPlugin implements IEnginePlugin {
    @Override
    public void processReading(EnginePreferences preferences, float[][] history, float[] newReading) {
        if (!preferences.smoothing_enabled) return;

        for (int a = 0; a < EnginePreferences.AXES; a++) {
            switch (preferences.smoothing_algorithm) {
                case "mean":
                    float sum = 0.0f;
                    for (float val : history[a]) sum += val;
                    newReading[a] = sum / history[a].length;
                    break;
                case "median":
                    float medianArray[] = new float[history[a].length];
                    System.arraycopy(history[a], 0, medianArray, 0, medianArray.length);
                    Arrays.sort(medianArray);
                    newReading[a] = medianArray[medianArray.length / 2];
                    break;
                case "lowpass":
                    if (history[a].length > 1) {
                        newReading[a] = Util.lowPass(
                                preferences.smoothing_alpha,
                                newReading[a],
                                history[a][1]);
                    }
                    break;
            }
        }
    }
}
