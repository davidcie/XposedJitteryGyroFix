package net.davidcie.gyroscopebandaid.plugins;

import net.davidcie.gyroscopebandaid.EnginePreferences;

public interface IEnginePlugin {
    public void processReading(EnginePreferences preferences, float[][] history, float[] newReading);
}
