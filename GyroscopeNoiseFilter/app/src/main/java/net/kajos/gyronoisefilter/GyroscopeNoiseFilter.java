package net.kajos.gyronoisefilter;

import android.os.Build;

import net.kajos.gyronoisefilter.hooks.SensorEventApi18;
import net.kajos.gyronoisefilter.hooks.SensorEventApi23;
import net.kajos.gyronoisefilter.hooks.SensorEventApi24;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class GyroscopeNoiseFilter implements IXposedHookLoadPackage {

    @SuppressWarnings("WeakerAccess")
    public static XSharedPreferences sPrefs;

    /**
     * Entry point into the module.
     *
     * @param lpparam Package information.
     * @throws Throwable Any errors.
     */
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log("GyroFilter: loading package " + lpparam.packageName);

        // Load preferences using Xposed, SharedPreferences() won't work inside the hook
        sPrefs = new XSharedPreferences(GyroscopeNoiseFilter.class.getPackage().getName(), "pref_median");
        sPrefs.makeWorldReadable();

        hookGyroInterceptor(lpparam);
        hookCardboardInterceptor(lpparam);
    }

    private void hookGyroInterceptor(final XC_LoadPackage.LoadPackageParam lpparam) {
        // Since API18 SensorEventQueue dispatches events via the same method:
        // dispatchSensorEvent(int handle, float[] values, int inAccuracy, long timestamp)

        if (Build.VERSION.SDK_INT >= 24) {
            XposedHelpers.findAndHookMethod(
                    "android.hardware.SystemSensorManager$SensorEventQueue",
                    lpparam.classLoader, "dispatchSensorEvent", int.class, float[].class, int.class, long.class,
                    new SensorEventApi24()
            );
        } else if (Build.VERSION.SDK_INT == 23) {
            XposedHelpers.findAndHookMethod(
                    "android.hardware.SystemSensorManager$SensorEventQueue",
                    lpparam.classLoader, "dispatchSensorEvent", int.class, float[].class, int.class, long.class,
                    new SensorEventApi23()
            );
        } else if (Build.VERSION.SDK_INT >= 18) {
            XposedHelpers.findAndHookMethod(
                    "android.hardware.SystemSensorManager$SensorEventQueue",
                    lpparam.classLoader, "dispatchSensorEvent", int.class, float[].class, int.class, long.class,
                    new SensorEventApi18()
            );
        } else {
            XposedBridge.log("GyroFilter: using an unsupported API " + Build.VERSION.SDK_INT);
        }
    }

    private void hookCardboardInterceptor(final XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class headTransformTMP = XposedHelpers.findClassIfExists("com.google.vrtoolkit.cardboard.HeadTransform", lpparam.classLoader);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
