package net.kajos.gyronoisefilter;

import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;
import android.util.SparseArray;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;

import static de.robv.android.xposed.XposedHelpers.findClass;
import android.hardware.Sensor;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;


public class GyroscopeNoiseFilter implements IXposedHookLoadPackage {
    @SuppressWarnings("WeakerAccess")
    public static XSharedPreferences prefs;
    private static final String TAG = "GyroFilter";

    private static float[][] processSensorReadings(boolean absoluteMode, float[] reading, float[][] rawReadings, float[] previousReading) {
        try {
            int axes = rawReadings.length;
            int filterSize = rawReadings[0].length;

            Log.d(TAG, "processSensorReadings called with " + printReading(reading));

            // We are inside the hook so normally we have no access to the module's preferences.
            prefs.makeWorldReadable();
            // Just in case the user changed preferences without rebooting.
            prefs.reload();

            String algorithm = prefs.getString("filter_type", "median");
            float alpha = Float.parseFloat(prefs.getString("filter_alpha", "0.5"));
            float threshold = Float.parseFloat(prefs.getString("filter_min_change", "0.0"));
            float stationaryThreshold = Float.parseFloat(prefs.getString("filter_stationary_min_change", "0.0"));
            int roundingPrecision = Integer.parseInt(prefs.getString("filter_round_precision", "0"));

            // If user changed filter size, increase the size of the array
            int newFilterSize = Integer.parseInt(prefs.getString("filter_size", "10"));
            rawReadings = resizeFilter(rawReadings, newFilterSize);

            // Process the new gyroscope reading
            for (int a = 0; a < axes; a++) {
                Log.v(TAG, "Processing axis " + a);

                // Step 1: apply sensor calibration and initialize final reading
                float calibration = 0.035154797f;
                Log.v(TAG, "Applying calibration of " + calibration);
                reading[a] += calibration;
                float computedReading = reading[a];

                // Step 2: add to FIFO queue of all raw values
                System.arraycopy(rawReadings[a], 0, rawReadings[a], 1, filterSize - 1);
                rawReadings[a][0] = reading[a];

                // Step 3: apply filtering
                if (algorithm.equals("none")) {
                    computedReading = reading[a];
                } else if (algorithm.equals("median")) {
                    float medianArray[] = new float[filterSize];
                    System.arraycopy(rawReadings[a], 0, medianArray, 0, medianArray.length);
                    Arrays.sort(medianArray);
                    computedReading = medianArray[medianArray.length / 2];
                } else if (algorithm.equals("mean")) {
                    float sum = 0.0f;
                    for (float val : rawReadings[a]) sum += val;
                    computedReading = sum / rawReadings[a].length;
                } else if (algorithm.equals("lowpass")) {
                    computedReading = lowPass(alpha, reading[a], previousReading[a]);
                } else if (algorithm.equals("addsmooth")) {
                    // Additive smoothing (TODO: check the math)
                    float sum = 0.0f;
                    for (float val : rawReadings[a]) sum += val;
                    computedReading = (reading[a] + alpha) / (sum + alpha * rawReadings.length);
                }
                Log.v(TAG, "Filtering using '" + algorithm + "', result is " + computedReading);

                // Step 4: apply thresholding vs previously computed value
                if (absoluteMode || reading[a] != 0.0f) {
                    float newReadingAbs = Math.abs(reading[a]);
                    float oldReadingAbs = Math.abs(previousReading[a]);

                    if (stationaryThreshold > 0.0f && !absoluteMode) {
                        if (newReadingAbs < stationaryThreshold) {
                            computedReading = 0.0f;
                            Log.d(TAG, "Below stationary threshold (axis: " + a + ", newReadingAbs: " + reading[a] + ")");
                        }
                    } else if (stationaryThreshold > 0.0f && absoluteMode) {
                        if (Math.abs(newReadingAbs - oldReadingAbs) < stationaryThreshold) {
                            computedReading = previousReading[a];
                        }
                    } else if (threshold > 0.0f && !absoluteMode) {
                        if (Math.abs(newReadingAbs - oldReadingAbs) < threshold) {
                            computedReading = 0.0f;
                        }
                    } else if (threshold > 0.0f && absoluteMode) {
                        if (Math.abs(newReadingAbs - oldReadingAbs) < threshold) {
                            computedReading = previousReading[a];
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
            System.arraycopy(reading, 0, previousReading, 0, axes);
            Log.d(TAG, "processSensorReadings returning " + printReading(reading));
        } catch(Throwable t) {
            Log.e(TAG, "Exception in processSensorReadings: " + t.getMessage());
        }

        // Return the array just in case its size has changed and cannot be updated by reference.
        return rawReadings;
    }

    private void hookSensorValues(final XC_LoadPackage.LoadPackageParam lpparam) {

        if (Build.VERSION.SDK_INT >= 24)
            // dispatchSensorEvent(int handle, float[] values, int inAccuracy, long timestamp)
            XposedHelpers.findAndHookMethod(
                    "android.hardware.SystemSensorManager$SensorEventQueue",
                    lpparam.classLoader, "dispatchSensorEvent", int.class, float[].class, int.class, long.class,
                    new XC_MethodHook() {
                        // Set the number of axes to process
                        int axes = 3;
                        int filterSize = 10;

                        // values[] contains the current sensor's value for each axis.
                        // The values are measured in rad/s, which is standard since Android 2.3.
                        // To get values of about 1.0 you have to turn at a speed of almost 60 deg/s.
                        float rawReadings[][] = new float[axes][filterSize]; // stores the last sensor's values in each dimension (3D so 3 dimensions)
                        float previousReading[] = new float[axes]; // stores the previous sensor's values to restore them if needed

                        // This is where we instruct Xposesd to call another method whnever a new sensor
                        // value is spit out by the gyroscope to the package.
                        @SuppressWarnings("unchecked")
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            int handle = (Integer) param.args[0];
                            Object mgr = XposedHelpers.getObjectField(param.thisObject, "mManager");
                            HashMap<Integer, Sensor> sensors = (HashMap<Integer, Sensor>) XposedHelpers.getObjectField(mgr, "mHandleToSensor");
                            Sensor sensor = sensors.get(handle);

                            if (sensor.getType() == Sensor.TYPE_GYROSCOPE ||
                                sensor.getType() == Sensor.TYPE_GYROSCOPE_UNCALIBRATED) {
                                hijackSensorEvent((float[]) param.args[1]);
                            }
                        }

                        private void hijackSensorEvent(float[] gyroReading) {
                            // Update reference just in case array size was changed
                            rawReadings = processSensorReadings(false, gyroReading, rawReadings, previousReading);
                        }
                    }
            );
    }

    /**
     * Entry point into the module.
     *
     * @param lpparam Package information.
     * @throws Throwable Any errors.
     */
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        // Load preferences using Xposed, SharedPreferences() won't work inside the hook
        prefs = new XSharedPreferences(GyroscopeNoiseFilter.class.getPackage().getName(), "pref_median");
        prefs.makeWorldReadable();

        hookSensorValues(lpparam);

        //hookIntoCardboard(lpparam);

        // -- Cardboard SDK hook: HeadTransform
        // This is an optional hook (ie, it will hook only if the lib is used in the app), hence the try/catch
        /*
        try {
            final Class<?> cla = findClass(
                    "com.google.vrtoolkit.cardboard.HeadTransform",
                    lpparam.classLoader);

            XposedBridge.hookAllMethods(cla, "getHeadView", new
                    XC_MethodHook() {

                        // Pre-process for this hook

                        // Set the number of values to anti-jitter
                        // You should edit this for each hook
                        int nbaxis = 13;

                        // Init the arrays
                        int filter_size = 10;
                        float medianValues[][] = new float[nbaxis][filter_size]; // stores the last sensor's values in each dimension (3D so 3 dimensions)
                        float prevValues[] = new float[nbaxis]; // stores the previous sensor's values to restore them if needed

                        private void changeSensorEvent(float[] values) {
                            // Note about values[]:
                            // values[] contains the current sensor's value for each axis (there are 3 since it's in 3D).
                            // The values are measured in rad/s, which is standard since Android 2.3 (before, some phones can return values in deg/s).
                            // To get values of about 1.0 you have to turn at a speed of almost 60 deg/s.

                            // Anti-jitter the values!
                            // Externalizing the processSensorReadings() function (ie, putting it outside of the hook) allows us to reuse the same function for several hooks.
                            // However, the previous values and the history of the median values will be different for different hooks (because the values are different), so we need to preprocess the values and to store them in different arrays for each hook. That's why we do this pre-processing here (and above this function).
                            List<Object> retlist = processSensorReadings(true, values, medianValues, prevValues);

                            // Update the local arrays for this hook
                            medianValues = (float[][])retlist.get(0);
                            prevValues = (float[])retlist.get(1);
                        }

                        // Hook caller
                        // This is where we tell what we should do when the hook is triggered (ie, when the hooked function/method is called)
                        // Basically, we just check a few stuffs about the sensor's values and then we call our changeSensorEvent() to do the rest
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws
                                Throwable {
                            Log.d(TAG, "Hook 1!");
                            super.afterHookedMethod(param);
                            float[] values = (float[])param.args[0];
                            Log.d(TAG, "BOBO1");
                            for (int i = 0;i<values.length;i++) {
                                Log.d(TAG, "BOBO before values: "+i+" : "+values[i]);
                                //values[i] = 0.0f;
                            }
                            changeSensorEvent(values);
                            for (int i = 0;i<values.length;i++) {
                                Log.d(TAG, "BOBO after values: "+i+" : "+values[i]);
                                //values[i] = 0.0f;
                            }
                            Log.d(TAG, "BOBO2");
                        }
                    });

            Log.d(TAG, "Installed cardboard head patch in: " + lpparam.packageName);

        } catch (Throwable t) {
            Log.e(TAG, "Exception in Cardboard Head hook: "+t.getMessage());
            // Do nothing
        }
        */

        // -- Cardboard SDK hook: Eye
        // This is an optional hook (ie, it will hook only if the lib is used in the app), hence the try/catch
        /*try {
            final Class<?> cla = findClass(
                    "com.google.vrtoolkit.cardboard.Eye",
                    lpparam.classLoader);

            XposedBridge.hookAllMethods(cla, "getEyeView", new
                    XC_MethodHook() {

                        // Pre-process for this hook

                        // Set the number of values to anti-jitter
                        // You should edit this for each hook
                        int nbaxis = 13;

                        // Init the arrays
                        int filter_size = 10;
                        float rawReadings[][] = new float[nbaxis][filter_size]; // stores the last sensor's values in each dimension (3D so 3 dimensions)
                        float previousReading[] = new float[nbaxis]; // stores the previous sensor's values to restore them if needed

                        private void changeSensorEvent(float[] values) {
                            // Note about values[]:
                            // values[] contains the current sensor's value for each axis (there are 3 since it's in 3D).
                            // The values are measured in rad/s, which is standard since Android 2.3 (before, some phones can return values in deg/s).
                            // To get values of about 1.0 you have to turn at a speed of almost 60 deg/s.

                            // Anti-jitter the values!
                            // Externalizing the processSensorReadings() function (ie, putting it outside of the hook) allows us to reuse the same function for several hooks.
                            // However, the previous values and the history of the median values will be different for different hooks (because the values are different), so we need to preprocess the values and to store them in different arrays for each hook. That's why we do this pre-processing here (and above this function).
                            rawReadings = processSensorReadings(true, values, rawReadings, previousReading);
                        }

                        // Hook caller
                        // This is where we tell what we should do when the hook is triggered (ie, when the hooked function/method is called)
                        // Basically, we just check a few stuffs about the sensor's values and then we call our changeSensorEvent() to do the rest

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws
                                Throwable {
                            Log.d(TAG, "Hook 2!");
                            super.afterHookedMethod(param);
                            float[] values = (float[])param.getResult();
                            Log.d(TAG, "BABA1");
                            for (int i = 0;i<values.length;i++) {
                                Log.d(TAG, "BABA before values: "+i+" : "+values[i]);
                                //values[i] = 0.0f;
                            }
                            changeSensorEvent(values);
                            for (int i = 0;i<values.length;i++) {
                                Log.d(TAG, "BABA after values: "+i+" : "+values[i]);
                                //values[i] = 0.0f;
                            }
                            Log.d(TAG, "BABA2");
                        }
                    });

            Log.d(TAG, "Installed cardboard eye patch in: " + lpparam.packageName);

        } catch (Throwable t) {
            Log.e(TAG, "Exception in Cardboard Eye hook: "+t.getMessage());
        }*/
    }

    /**
     * Low-pass filter implementation.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Low-pass_filter#Algorithmic_implementation">Algorithmic_implementation</a>
     * @see <a href="http://en.wikipedia.org/wiki/Low-pass_filter#Simple_infinite_impulse_response_filter">Simple infinite impulse response filter</a>
     * @see <a href="http://en.wikipedia.org/wiki/Low-pass_filter#Discrete-time_realization">Discrete-time realization</a>
     * Alpha is the time smoothing constant for the low-pass filter. 0 <= alpha <= 1;
     * a smaller value basically means more smoothing. All credits go to Thom Nichols Thom Nichols.
     * See <a href="http://blog.thomnichols.org/2011/08/smoothing-sensor-data-with-a-low-pass-filter">his article</a>
     * and <a href="http://stackoverflow.com/a/5780505/1121352">StackOverflow answer</a>.
     */
    private static float lowPass(float alpha, float current, float prev) {
        //if ( prev == null ) return current;

        //for ( int i=0; i<input.length; i++ ) {
        //}
        return prev + alpha * (current - prev);
    }

    private static float[][] resizeFilter(float[][] currentFilter, int newSize) {
        int axes = currentFilter.length;
        if (axes < 1 || currentFilter[0].length < 1 || currentFilter[0].length == newSize || newSize < 1)
            return currentFilter;

        int oldSize = currentFilter[0].length;
        float newMedianValues[][] = new float[axes][newSize];

        for (int a = 0; a < axes; a++) {
            System.arraycopy(
                    currentFilter[a], 0,
                    newMedianValues[a], 0,
                    Math.min(newSize, oldSize));
        }

        Log.d(TAG, "Filter size changed from " + oldSize + " to " + newSize);

        return newMedianValues;
    }

    private static String printReading(float[] reading) {
        StringBuilder resul = new StringBuilder();
        for (int a = 0; a < reading.length; a++) {
            resul.append(" [");
            resul.append(a);
            resul.append("]=");
            resul.append(reading[a]);
        }
        return resul.toString().trim();
    }
}
