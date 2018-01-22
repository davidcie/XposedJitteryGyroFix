package net.davidcie.gyroscopebandaid.plugins;

import net.davidcie.gyroscopebandaid.EnginePreferences;

public class InvertingPlugin implements IEnginePlugin {
    @Override
    public void processReading(EnginePreferences preferences, float[][] history, float[] newReading) {
        if (!preferences.inversion_enabled) return;
        if (preferences.inversion_axes.contains("x")) newReading[0] *= -1;
        if (preferences.inversion_axes.contains("y")) newReading[1] *= -1;
        if (preferences.inversion_axes.contains("z")) newReading[2] *= -1;
    }
}
