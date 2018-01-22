package net.davidcie.gyroscopebandaid.hooks;

import android.hardware.Sensor;

import net.davidcie.gyroscopebandaid.Engine;

import java.util.HashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class SensorEventApi24 extends XC_MethodHook {

    private Engine mEngine;

    public SensorEventApi24(Engine engine) {
        mEngine = engine;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        int handle = (Integer) param.args[0];
        Object mgr = XposedHelpers.getObjectField(param.thisObject, "mManager");
        HashMap<Integer, Sensor> sensors = (HashMap<Integer, Sensor>) XposedHelpers.getObjectField(mgr, "mHandleToSensor");
        Sensor sensor = sensors.get(handle);

        if (sensor.getType() == Sensor.TYPE_GYROSCOPE ||
            sensor.getType() == Sensor.TYPE_GYROSCOPE_UNCALIBRATED) {
            mEngine.newReading((float[]) param.args[1]);
        }
    }
}