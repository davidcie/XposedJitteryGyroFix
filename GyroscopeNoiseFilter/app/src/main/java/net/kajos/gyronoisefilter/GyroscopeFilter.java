package net.kajos.gyronoisefilter;

import android.util.Log;

import java.util.Arrays;

public class GyroscopeFilter {
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
        //GyroscopeNoiseFilter.sPrefs.makeWorldReadable();
        // Just in case the user changed preferences without rebooting.
        GyroscopeNoiseFilter.sPrefs.reload();

        String algorithm = GyroscopeNoiseFilter.sPrefs.getString("filter_type", "median");
        float alpha = Float.parseFloat(GyroscopeNoiseFilter.sPrefs.getString("filter_alpha", "0.5"));
        float threshold = Float.parseFloat(GyroscopeNoiseFilter.sPrefs
                                                   .getString("filter_min_change", "0.0"));
        float stationaryThreshold = Float.parseFloat(GyroscopeNoiseFilter.sPrefs
                                                             .getString("filter_stationary_min_change", "0.0"));
        int roundingPrecision = Integer.parseInt(GyroscopeNoiseFilter.sPrefs
                                                         .getString("filter_round_precision", "0"));

        // If user changed filter size, increase the size of the array
        int newFilterSize = Integer.parseInt(GyroscopeNoiseFilter.sPrefs
                                                     .getString("filter_size", "10"));
        mRawReadings = Util.resizeSecondDimension(mRawReadings, newFilterSize);

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
            if (algorithm.equals("none")) {
                computedReading = reading[a];
            } else if (algorithm.equals("median")) {
                float medianArray[] = new float[aFilterSize];
                System.arraycopy(mRawReadings[a], 0, medianArray, 0, medianArray.length);
                Arrays.sort(medianArray);
                computedReading = medianArray[medianArray.length / 2];
            } else if (algorithm.equals("mean")) {
                float sum = 0.0f;
                for (float val : mRawReadings[a]) sum += val;
                computedReading = sum / mRawReadings[a].length;
            } else if (algorithm.equals("lowpass")) {
                computedReading = Util.lowPass(alpha, reading[a], mPreviousReading[a]);
            } else if (algorithm.equals("addsmooth")) {
                // Additive smoothing (TODO: check the math)
                float sum = 0.0f;
                for (float val : mRawReadings[a]) sum += val;
                computedReading = (reading[a] + alpha) / (sum + alpha * mRawReadings.length);
            }
            Log.v(TAG, "Filtering using '" + algorithm + "', result is " + computedReading);

            // Step 4: apply thresholding vs previously computed value
            if (mAbsoluteMode || reading[a] != 0.0f) {
                float newReadingAbs = Math.abs(reading[a]);
                float oldReadingAbs = Math.abs(mPreviousReading[a]);

                if (stationaryThreshold > 0.0f && !mAbsoluteMode) {
                    if (newReadingAbs < stationaryThreshold) {
                        computedReading = 0.0f;
                        Log.d(TAG, "Below stationary threshold (axis: " + a + ", newReadingAbs: " + reading[a] + ")");
                    }
                } else if (stationaryThreshold > 0.0f && mAbsoluteMode) {
                    if (Math.abs(newReadingAbs - oldReadingAbs) < stationaryThreshold) {
                        computedReading = mPreviousReading[a];
                    }
                } else if (threshold > 0.0f && !mAbsoluteMode) {
                    if (Math.abs(newReadingAbs - oldReadingAbs) < threshold) {
                        computedReading = 0.0f;
                    }
                } else if (threshold > 0.0f && mAbsoluteMode) {
                    if (Math.abs(newReadingAbs - oldReadingAbs) < threshold) {
                        computedReading = mPreviousReading[a];
                    }
                }

                Log.v(TAG, "After thresholding the result is " + computedReading);
            }

            // Step 5: apply rounding
            if (roundingPrecision > 0 && computedReading > 0.0f) {
                String rounded = String.format("%." + roundingPrecision + "f", computedReading);
                computedReading = Float.valueOf(rounded);
                Log.v(TAG, "Applying rounding to " + roundingPrecision + "dp, result is " + computedReading);
            }

            // Step 6: replace sensor reading
            reading[a] = computedReading;
        }

        // Remember the calculated reading as baseline for next calculation.
        System.arraycopy(reading, 0, mPreviousReading, 0, mAxes);
        Log.d(TAG, "processSensorReadings returning " + Util.printArray(reading));
    }
}
