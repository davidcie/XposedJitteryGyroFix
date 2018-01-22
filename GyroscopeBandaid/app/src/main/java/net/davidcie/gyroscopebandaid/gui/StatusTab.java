package net.davidcie.gyroscopebandaid.gui;

import android.app.Fragment;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.davidcie.gyroscopebandaid.Engine;
import net.davidcie.gyroscopebandaid.EnginePreferences;
import net.davidcie.gyroscopebandaid.R;

import static android.content.Context.SENSOR_SERVICE;

public class StatusTab extends Fragment {

    private Engine mEngine;
    private SensorManager mSensorManager;
    private Sensor mGyroscope;
    private SensorEventListener mGyroListener;

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) mSensorManager.registerListener(mGyroListener, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        else mSensorManager.unregisterListener(mGyroListener);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mEngine = new Engine(false);
        mSensorManager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mGyroListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                Log.d(EnginePreferences.LOG_TAG, "Received SensorEvent on Status tab");
                mEngine.newReading(sensorEvent.values);
                // update UI
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };

        return inflater.inflate(R.layout.tab_status, container, false);
    }
}
