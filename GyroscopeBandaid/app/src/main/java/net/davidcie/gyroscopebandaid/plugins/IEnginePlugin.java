package net.davidcie.gyroscopebandaid.plugins;

import android.content.SharedPreferences;

public interface IEnginePlugin {
    public void processReading(SharedPreferences preferences, float[][] history, float[] newReading);
}
