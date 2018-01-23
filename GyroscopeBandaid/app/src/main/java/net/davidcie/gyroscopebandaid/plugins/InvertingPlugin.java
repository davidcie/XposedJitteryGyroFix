package net.davidcie.gyroscopebandaid.plugins;

import android.content.SharedPreferences;

import net.davidcie.gyroscopebandaid.Const;

import java.util.HashSet;
import java.util.Set;

public class InvertingPlugin implements IEnginePlugin {
    @Override
    public void processReading(SharedPreferences preferences, float[][] history, float[] newReading) {
        if (!preferences.getBoolean(Const.PREF_INVERSION_ENABLED, false)) return;
        Set<String> invertAxes = preferences.getStringSet(Const.PREF_INVERSION_AXES, new HashSet<String>());
        if (invertAxes.contains("x")) newReading[0] *= -1;
        if (invertAxes.contains("y")) newReading[1] *= -1;
        if (invertAxes.contains("z")) newReading[2] *= -1;
    }
}
