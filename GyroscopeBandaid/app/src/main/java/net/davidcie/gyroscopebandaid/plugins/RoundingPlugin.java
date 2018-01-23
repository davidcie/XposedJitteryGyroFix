package net.davidcie.gyroscopebandaid.plugins;

import android.content.SharedPreferences;

import net.davidcie.gyroscopebandaid.Const;
import net.davidcie.gyroscopebandaid.Util;

public class RoundingPlugin implements IEnginePlugin {
    @Override
    public void processReading(SharedPreferences preferences, float[][] history, float[] newReading) {
        if (!preferences.getBoolean(Const.PREF_ROUNDING_ENABLED, false)) return;

        int decimalPlaces = preferences.getInt(Const.PREF_ROUNDING_DECIMALPLACES, Const.DEFAULT_ROUNDING_DECIMALPLACES);
        for (int a = 0; a < Util.AXES; a++) {
            newReading[a] = Util.round(newReading[a], decimalPlaces);
        }
    }
}
