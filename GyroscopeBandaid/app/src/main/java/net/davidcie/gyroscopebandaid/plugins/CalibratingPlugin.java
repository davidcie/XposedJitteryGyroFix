package net.davidcie.gyroscopebandaid.plugins;

import android.content.SharedPreferences;

import net.davidcie.gyroscopebandaid.Const;

public class CalibratingPlugin implements IEnginePlugin {
    @Override
    public void processReading(SharedPreferences preferences, float[][] history, float[] newReading) {
        if (!preferences.getBoolean(Const.PREF_CALIBRATION_ENABLED, false)) return;
        newReading[0] += preferences.getFloat(Const.PREF_CALIBRATION_VALUE_X, Const.DEFAULT_CALIBRATION_X);
        newReading[1] += preferences.getFloat(Const.PREF_CALIBRATION_VALUE_Y, Const.DEFAULT_CALIBRATION_Y);
        newReading[2] += preferences.getFloat(Const.PREF_CALIBRATION_VALUE_Z, Const.DEFAULT_CALIBRATION_Z);
    }
}
