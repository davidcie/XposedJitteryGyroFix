package net.davidcie.gyroscopebandaid.plugins;

import android.content.SharedPreferences;
import android.util.Log;

import net.davidcie.gyroscopebandaid.Const;
import net.davidcie.gyroscopebandaid.Util;

import java.util.Arrays;

public class SmoothingPlugin implements IEnginePlugin {
    @Override
    public void processReading(SharedPreferences preferences, float[][] history, float[] newReading) {
        if (!preferences.getBoolean(Const.PREF_SMOOTHING_ENABLED, false)) return;

        String defaultAlgorithm = Const.DEFAULT_SMOOTHING_ALGORITHM;
        String algorithm = preferences.getString(Const.PREF_SMOOTHING_ALGORITHM, defaultAlgorithm);
        for (int a = 0; a < Util.AXES; a++) {
            switch (algorithm) {
                case Const.PREF_SMOOTHING_ALGORITHM_MEAN:
                    float sum = 0.0f;
                    for (float val : history[a]) sum += val;
                    newReading[a] = sum / history[a].length;
                    break;
                case Const.PREF_SMOOTHING_ALGORITHM_MEDIAN:
                    float medianArray[] = new float[history[a].length];
                    System.arraycopy(history[a], 0, medianArray, 0, medianArray.length);
                    Arrays.sort(medianArray);
                    newReading[a] = medianArray[medianArray.length / 2];
                    break;
                case Const.PREF_SMOOTHING_ALGORITHM_LOWPASS:
                    if (history[a].length > 1) {
                        newReading[a] = Util.lowPass(
                                preferences.getFloat(Const.PREF_SMOOTHING_ALPHA, Const.DEFAULT_SMOOTHING_ALPHA),
                                newReading[a],
                                history[a][1]);
                    }
                    break;
                default:
                    Log.d(Util.LOG_TAG, "Unsupported smoothing algorithm '" + algorithm + "'");
            }
        }
    }
}
