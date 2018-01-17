package net.davidcie.gyroscopebandaid.hooks;

import android.hardware.Sensor;
import android.util.SparseArray;

import net.davidcie.gyroscopebandaid.GyroscopeFilter;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class SensorEventApi23 extends XC_MethodHook {

    private GyroscopeFilter mFilter = new GyroscopeFilter(3, 10, false);

    @SuppressWarnings("unchecked")
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        int handle = (Integer) param.args[0];
        Object mgr = XposedHelpers.getObjectField(param.thisObject, "mManager");
        SparseArray<Sensor> sensors = (SparseArray<Sensor>) XposedHelpers.getObjectField(mgr, "mHandleToSensor");
        Sensor sensor = sensors.get(handle);

        if (sensor.getType() == Sensor.TYPE_GYROSCOPE ||
            sensor.getType() == Sensor.TYPE_GYROSCOPE_UNCALIBRATED) {
            mFilter.newReading((float[]) param.args[1]);
        }
    }
}