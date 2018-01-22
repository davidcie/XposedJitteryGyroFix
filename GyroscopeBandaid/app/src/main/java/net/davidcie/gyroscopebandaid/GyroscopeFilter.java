package net.davidcie.gyroscopebandaid;

import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GyroscopeFilter {

    public class GyroBandaidPreferences {
        boolean enabled;

        boolean calibration_enabled;
        float calibration_value_x;
        float calibration_value_y;
        float calibration_value_z;

        boolean inversion_enabled;
        Set<String> inversion_axes;

        boolean smoothing_enabled;
        String smoothing_algorithm;
        int smoothing_sample;
        float smoothing_alpha;

        boolean thresholding_enabled;
        float thresholding_static;
        float thresholding_dynamic;

        boolean rounding_enabled;
        int rounding_decimalplaces;

        void reload() {
            XposedModule.sPrefs.reload();
            enabled = XposedModule.sPrefs.getBoolean("global_enabled", true);
            calibration_enabled = XposedModule.sPrefs.getBoolean("calibration_enabled", false);
            calibration_value_x = XposedModule.sPrefs.getFloat("", 0.0f);
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
    private static final String TAG = "GyroFilter";

    // values[] contains the current sensor's value for each axis.
    // The values are measured in rad/s, which is standard since Android 2.3.
    // To get values of about 1.0 you have to turn at a speed of almost 60 deg/s.

    private float[][] mRawReadings;
    private float[] mPreviousReading;
    private int mAxes;
    private int aFilterSize;
    private boolean mAbsoluteMode;

    public GyroscopeFilter(int axes, int filterSize, boolean absoluteMode) {
        mAxes = axes;
        mPreviousReading = new float[axes];
        aFilterSize = filterSize;
        mAbsoluteMode = absoluteMode;
    }

    public void newReading(float[] reading) {
        Log.d(TAG, "processSensorReadings called with " + Util.printArray(reading));

        // We are inside the hook so normally we have no access to the module's preferences.
        //XposedModule.sPrefs.makeWorldReadable();
        // Just in case the user changed preferences without rebooting.
        XposedModule.sPrefs.reload();

        // Get current preferences
        // TODO: replace with a listener that changes without polling
        GyroBandaidPreferences prefs = new GyroBandaidPreferences();
        prefs.reload();

        // If user changed filter size, increase the size of the array
        mRawReadings = Util.resizeSecondDimension(mRawReadings, prefs.smoothing_sample);

        // Process the new gyroscope reading
        for (int a = 0; a < mAxes; a++) {
            Log.v(TAG, "Processing axis " + a);

            // Step 1: apply sensor calibration and initialize final reading
            float calibration = 0.035154797f;
            Log.v(TAG, "Applying calibration of " + calibration);
            reading[a] += calibration;
            float computedReading = reading[a];

            // Step 2: add to FIFO queue of all raw values
            System.arraycopy(mRawReadings[a], 0, mRawReadings[a], 1, aFilterSize - 1);
            mRawReadings[a][0] = reading[a];

            // Step 3: apply filtering
            if (prefs.smoothing_algorithm.equals("none")) {
                computedReading = reading[a];
            } else if (prefs.smoothing_algorithm.equals("median")) {
                float medianArray[] = new float[aFilterSize];
                System.arraycopy(mRawReadings[a], 0, medianArray, 0, medianArray.length);
                Arrays.sort(medianArray);
                computedReading = medianArray[medianArray.length / 2];
            } else if (prefs.smoothing_algorithm.equals("mean")) {
                float sum = 0.0f;
                for (float val : mRawReadings[a]) sum += val;
                computedReading = sum / mRawReadings[a].length;
            } else if (prefs.smoothing_algorithm.equals("lowpass")) {
                computedReading = Util.lowPass(prefs.smoothing_alpha, reading[a], mPreviousReading[a]);
            }
            Log.v(TAG, "Filtered using '" + prefs.smoothing_algorithm + "', result is " + computedReading);

            // Step 4: apply thresholding vs previously computed value
            if (mAbsoluteMode || reading[a] != 0.0f) {
                float newReadingAbs = Math.abs(reading[a]);
                float oldReadingAbs = Math.abs(mPreviousReading[a]);

                if (prefs.thresholding_static > 0.0f && !mAbsoluteMode) {
                    if (newReadingAbs < prefs.thresholding_static) {
                        computedReading = 0.0f;
                        Log.d(TAG, "Below stationary threshold (axis: " + a + ", newReadingAbs: " + reading[a] + ")");
                    }
                } else if (prefs.thresholding_static > 0.0f && mAbsoluteMode) {
                    if (Math.abs(newReadingAbs - oldReadingAbs) < prefs.thresholding_static) {
                        computedReading = mPreviousReading[a];
                    }
                } else if (prefs.thresholding_dynamic > 0.0f && !mAbsoluteMode) {
                    if (Math.abs(newReadingAbs - oldReadingAbs) < prefs.thresholding_dynamic) {
                        computedReading = 0.0f;
                    }
                } else if (prefs.thresholding_dynamic > 0.0f && mAbsoluteMode) {
                    if (Math.abs(newReadingAbs - oldReadingAbs) < prefs.thresholding_dynamic) {
                        computedReading = mPreviousReading[a];
                    }
                }

                Log.v(TAG, "After thresholding the result is " + computedReading);
            }

            // Step 5: apply rounding
            if (prefs.rounding_decimalplaces > 0 && computedReading > 0.0f) {
                computedReading = Util.round(computedReading, prefs.rounding_decimalplaces);
                Log.v(TAG, "Applying rounding to " + prefs.rounding_decimalplaces + "dp, result is " + computedReading);
            }

            // Step 6: replace sensor reading
            reading[a] = computedReading;
        }

        // Remember the calculated reading as baseline for next calculation.
        System.arraycopy(reading, 0, mPreviousReading, 0, mAxes);
        Log.d(TAG, "processSensorReadings returning " + Util.printArray(reading));
    }
}
