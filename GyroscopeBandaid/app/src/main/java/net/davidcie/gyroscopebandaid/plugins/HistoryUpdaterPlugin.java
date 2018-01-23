package net.davidcie.gyroscopebandaid.plugins;

import android.content.SharedPreferences;

import net.davidcie.gyroscopebandaid.Util;

public class HistoryUpdaterPlugin implements IEnginePlugin {
    @Override
    public void processReading(SharedPreferences preferences, float[][] history, float[] newReading) {
        for (int a = 0; a < Util.AXES; a++) {
            System.arraycopy(history[a], 0, history[a], 1, history.length - 1);
            history[a][0] = newReading[a];
        }
    }
}
