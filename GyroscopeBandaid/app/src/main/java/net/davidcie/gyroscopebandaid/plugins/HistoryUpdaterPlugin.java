package net.davidcie.gyroscopebandaid.plugins;

import net.davidcie.gyroscopebandaid.EnginePreferences;

public class HistoryUpdaterPlugin implements IEnginePlugin {
    @Override
    public void processReading(EnginePreferences preferences, float[][] history, float[] newReading) {
        for (int a = 0; a < EnginePreferences.AXES; a++) {
            System.arraycopy(history[a], 0, history[a], 1, history.length - 1);
            history[a][0] = newReading[a];
        }
    }
}
