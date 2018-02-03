package net.davidcie.gyroscopebandaid.gui;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.os.TraceCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.davidcie.gyroscopebandaid.Engine;
import net.davidcie.gyroscopebandaid.FifoArray;
import net.davidcie.gyroscopebandaid.R;
import net.davidcie.gyroscopebandaid.Util;
import net.davidcie.gyroscopebandaid.controls.GraphTextureView;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import static android.content.Context.SENSOR_SERVICE;

public class StatusTab extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    // Sensor management
    private Engine mEngine;
    private SensorManager mSensorManager;
    private Sensor mGyroscope;
    private SensorEventListener mGyroListener;
    private boolean mSensorActive = false;
    private HandlerThread mHandlerThread;
    Handler handler;
    final float[] mLastRawValue = new float[3];
    final float[] mLastCookedValue = new float[3];
    final Object mValueLock = new Object();

    // Settings
    private final static int UPDATE_EVERY_MS = 100;
    private final static int GRAPH_VALUES = 50;
    private final static NumberFormat plusMinus = new DecimalFormat("+00.000000000;-00.000000000"); //minus
    //private final static NumberFormat plusMinus = new DecimalFormat("+00.000000000;–00.000000000"); //ndash

    // Collections
    private FifoArray<Float> rawX = new FifoArray<>(GRAPH_VALUES);
    private FifoArray<Float> rawY = new FifoArray<>(GRAPH_VALUES);
    private FifoArray<Float> rawZ = new FifoArray<>(GRAPH_VALUES);

    // Utility variables
    private View mView;
    private boolean mIsVisible = false;

    // Update task
    private Handler mUpdaterThread = new Handler();
    private Runnable mRequestReadingTask = new Runnable() {
        @Override
        public void run() {
            TraceCompat.beginSection("mRequestReadingTask");
            mUpdaterThread.postDelayed(this, UPDATE_EVERY_MS);
            if (mSensorActive) {
                float[] newRaw = new float[3];
                float[] newCooked = new float[3];
                synchronized (mValueLock) {
                    System.arraycopy(mLastRawValue, 0, newRaw, 0, 3);
                    System.arraycopy(mLastCookedValue, 0, newCooked, 0, 3);
                }
                updateValues(newRaw, newCooked);
            }
            TraceCompat.endSection();
        }
    };


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mView = inflater.inflate(R.layout.status, container, false);

        // Initialize collections
        for (int i = 0; i < rawX.size(); i++) {
            rawX.add(0f);
            rawY.add(0f);
            rawZ.add(0f);
        }

        GraphTextureView mRawGraphView = mView.findViewById(R.id.graph_raw);
        mRawGraphView.initialize(rawX, rawY, rawZ, UPDATE_EVERY_MS);

        initializeFilterStatus();

        //mEngine = new Engine(false, PreferenceManager.getDefaultSharedPreferences(getActivity()));
        mSensorManager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
        //noinspection ConstantConditions
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mGyroListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                synchronized (mValueLock) {
                    System.arraycopy(sensorEvent.values, 0, mLastRawValue, 0, 3);
                    //mEngine.newReading(sensorEvent.values);
                    //System.arraycopy(sensorEvent.values, 0, mLastCookedValue, 0, 3);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };
        mHandlerThread = new HandlerThread("GyroUpdater", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();
        handler = new Handler(mHandlerThread.getLooper());

        return mView;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }

    @Override
    public void onResume() {
        Log.d(Util.LOG_TAG, "onResume");
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
        if (!mSensorActive && mGyroscope != null) {
            Log.d(Util.LOG_TAG, "GyroService: Enabling sensor");
            mSensorManager.registerListener(mGyroListener, mGyroscope, SensorManager.SENSOR_DELAY_GAME, handler);
            mSensorActive = true;
        }
        mUpdaterThread.post(mRequestReadingTask);
    }

    @Override
    public void onPause() {
        Log.d(Util.LOG_TAG, "onPause");
        mUpdaterThread.removeCallbacks(mRequestReadingTask);
        if (mSensorActive) {
            Log.d(Util.LOG_TAG, "GyroService: Disabling sensor");
            mSensorManager.unregisterListener(mGyroListener, mGyroscope);
            mSensorActive = false;
        }
        PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mHandlerThread != null) mHandlerThread.quit();
        super.onDestroy();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.v(Util.LOG_TAG, "StatusTab: setUserVisibleHint(" + isVisibleToUser + ")");
        mIsVisible = isVisibleToUser;
        //if (mIsVisible) mUpdaterThread.post(mRequestReadingTask);
        //else mUpdaterThread.removeCallbacks(mRequestReadingTask);
        //setServicePlayback(mIsVisible ? GyroService.PLAY : GyroService.PAUSE);
    }


    //endregion

    private void initializeFilterStatus() {

    }

    @SuppressLint("SetTextI18n")
    private void updateValues(float[] latestRaw, float[] latestCooked) {
        //updateText(R.id.raw_x, latestRaw[0]);
        //updateText(R.id.raw_y, latestRaw[1]);
        //updateText(R.id.raw_z, latestRaw[2]);

        rawX.add(Util.limit(latestRaw[0], -1f, 1f));
        rawY.add(Util.limit(latestRaw[1], -1f, 1f));
        rawZ.add(Util.limit(latestRaw[2], -1f, 1f));
    }

    private void updateText(int id, float value) {
        if (mView == null) return;
        TextView text = mView.findViewById(id);
        if (text == null) return;
        text.setText(plusMinus.format(value));
    }
}
