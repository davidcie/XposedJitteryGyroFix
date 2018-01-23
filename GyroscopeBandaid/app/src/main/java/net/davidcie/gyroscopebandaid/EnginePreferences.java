package net.davidcie.gyroscopebandaid;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("WeakerAccess")
public class EnginePreferences {

    public static final String LOG_TAG = "GyroBandaid";
    public static final int AXES = 3;

    public boolean enabled;
    public boolean absoluteMode;

    public boolean calibration_enabled;
    public float calibration_value_x;
    public float calibration_value_y;
    public float calibration_value_z;

    public boolean inversion_enabled;
    public Set<String> inversion_axes;

    public boolean smoothing_enabled;
    public String smoothing_algorithm;
    public int smoothing_sample;
    public float smoothing_alpha;

    public boolean thresholding_enabled;
    public float thresholding_static;
    public float thresholding_dynamic;

    public boolean rounding_enabled;
    public int rounding_decimalplaces;

    public EnginePreferences(boolean _absoluteMode) {
        absoluteMode = _absoluteMode;
    }

    void reload() {
        XposedModule.sPrefs.reload();
        enabled = XposedModule.sPrefs.getBoolean("global_enabled", true);
        calibration_enabled = XposedModule.sPrefs.getBoolean("calibration_enabled", false);
        calibration_value_x = XposedModule.sPrefs.getFloat("calibration_value_x", 0.0f);
        calibration_value_y = XposedModule.sPrefs.getFloat("calibration_value_y", 0.0f);
        calibration_value_z = XposedModule.sPrefs.getFloat("calibration_value_z", 0.0f);
        inversion_enabled = XposedModule.sPrefs.getBoolean("inversion_enabled", false);
        inversion_axes = XposedModule.sPrefs.getStringSet("inversion_axes", new HashSet<String>());
        smoothing_enabled = XposedModule.sPrefs.getBoolean("smoothing_enabled", false);
        smoothing_algorithm = XposedModule.sPrefs.getString("smoothing_algorithm", "median");
        smoothing_sample = XposedModule.sPrefs.getInt("smoothing_sample", 10);
        smoothing_alpha = XposedModule.sPrefs.getFloat("smoothing_alpha", 0.5f);
        thresholding_enabled = XposedModule.sPrefs.getBoolean("thresholding_enabled", false);
        thresholding_static = XposedModule.sPrefs.getFloat("thresholding_static", 0.0f);
        thresholding_dynamic = XposedModule.sPrefs.getFloat("thresholding_dynamic", 0.0f);
        rounding_enabled = XposedModule.sPrefs.getBoolean("rounding_enabled", false);
        rounding_decimalplaces = XposedModule.sPrefs.getInt("rounding_decimalplaces", 0);
    }
}
