package net.davidcie.gyroscopebandaid;

import android.os.Build;
import android.util.Log;

import net.davidcie.gyroscopebandaid.hooks.SensorEventApi18;
import net.davidcie.gyroscopebandaid.hooks.SensorEventApi23;
import net.davidcie.gyroscopebandaid.hooks.SensorEventApi24;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedModule implements IXposedHookLoadPackage {

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
        if (lpparam.packageName.equals("net.davidcie.gyroscopebandaid")) {
            // A way to know that the module has been activated and loaded by Xposed
            XposedHelpers.findAndHookMethod(
                    "net.davidcie.gyroscopebandaid",
                    lpparam.classLoader,
                    "isModuleActivated",
                    XC_MethodReplacement.returnConstant(true));
            Log.d(EnginePreferences.LOG_TAG, "Skipping package " + lpparam.packageName);
            return;
        }

        Log.d(EnginePreferences.LOG_TAG, "Hooking into package " + lpparam.packageName);

        // Load preferences using Xposed, SharedPreferences() won't work inside the hook
        sPrefs = new XSharedPreferences(XposedModule.class.getPackage().getName(), "main_prefs");
        sPrefs.makeWorldReadable();

        // Hook into sensor readings
        hookGyroInterceptor(lpparam);
        hookCardboardInterceptor(lpparam);
    }

    private void hookGyroInterceptor(final XC_LoadPackage.LoadPackageParam lpparam) {
        // Since API 18 SensorEventQueue dispatches events via the same method but details change.
        XC_MethodHook dispatchHook;
        Engine engine = new Engine(false, true);

        if (Build.VERSION.SDK_INT >= 24) dispatchHook = new SensorEventApi24(engine);
        else if (Build.VERSION.SDK_INT == 23) dispatchHook = new SensorEventApi23(engine);
        else dispatchHook = new SensorEventApi18(engine);

        // dispatchSensorEvent(int handle, float[] values, int inAccuracy, long timestamp)
        XposedHelpers.findAndHookMethod(
                "android.hardware.SystemSensorManager$SensorEventQueue",
                lpparam.classLoader, "dispatchSensorEvent", int.class, float[].class, int.class, long.class,
                dispatchHook
        );
    }

    private void hookCardboardInterceptor(final XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class headTransformTMP = XposedHelpers.findClassIfExists("com.google.vrtoolkit.cardboard.HeadTransform", lpparam.classLoader);
            if (headTransformTMP != null) {
                XposedBridge.log("GyroBandaid: Found com.google.vrtoolkit.cardboard.HeadTransform in " + lpparam.packageName);
            } else {
                headTransformTMP = XposedHelpers.findClassIfExists("com.google.vr.sdk.base.HeadTransform", lpparam.classLoader);
                if (headTransformTMP != null) XposedBridge.log("GyroBandaid: Found com.google.vr.sdk.base.HeadTransform in " + lpparam.packageName);
                else return;
            }

            final Class headTransform = headTransformTMP;

            // getHeadView (float[] headView, int offset)
            XposedHelpers.findAndHookMethod(
                    headTransform.getName(),
                    lpparam.classLoader, "getHeadView", float[].class, int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log("GyroBandaid: getHeadView called");
                        }
                    }
            );
        } catch (Exception e) { e.printStackTrace(); }
    }
}
