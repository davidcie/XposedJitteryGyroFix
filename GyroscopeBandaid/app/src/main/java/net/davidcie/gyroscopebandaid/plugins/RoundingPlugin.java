package net.davidcie.gyroscopebandaid.plugins;

import net.davidcie.gyroscopebandaid.EnginePreferences;
import net.davidcie.gyroscopebandaid.Util;

public class RoundingPlugin implements IEnginePlugin {
    @Override
    public void processReading(EnginePreferences preferences, float[][] history, float[] newReading) {
        if (!preferences.rounding_enabled) return;

        for (int a = 0; a < EnginePreferences.AXES; a++) {
            newReading[a] = Util.round(newReading[a], preferences.rounding_decimalplaces);
        }
    }
}
