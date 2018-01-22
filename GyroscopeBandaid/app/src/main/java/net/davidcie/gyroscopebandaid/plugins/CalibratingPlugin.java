package net.davidcie.gyroscopebandaid.plugins;

import net.davidcie.gyroscopebandaid.EnginePreferences;

public class CalibratingPlugin implements IEnginePlugin {
    @Override
    public void processReading(EnginePreferences preferences, float[][] history, float[] newReading) {
        if (!preferences.calibration_enabled) return;
        newReading[0] += preferences.calibration_value_x;
        newReading[1] += preferences.calibration_value_y;
        newReading[2] += preferences.calibration_value_z;
    }
}
