package net.davidcie.gyroscopebandaid.services;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import net.davidcie.gyroscopebandaid.Engine;
import net.davidcie.gyroscopebandaid.EnginePreferences;

public class GyroService extends Service {

    // Available commands
    public static final int REGISTER_CLIENT = 1;
    public static final int UNREGISTER_CLIENT = 2;
    public static final int PAUSE = 10;
    public static final int PLAY = 11;
    public static final int NEW_READING = 12;
    public static final String KEY_ORIGINAL_VALUES = "original";
    public static final String KEY_PROCESSED_VALUES = "processed";

    // Endpoint for clients to send messages to GyroService
    final Messenger mServiceMessenger = new Messenger(new IncomingHandler());

    // Endpoint for talking to the client
    Messenger mClientMessenger = null;

    // Sensor management
    private Engine mEngine;
    private SensorManager mSensorManager;
    private Sensor mGyroscope;
    private SensorEventListener mGyroListener;
    private boolean mSensorActive = false;

    @Override
    public void onCreate() {
        super.onCreate();

        mEngine = new Engine(false);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mGyroListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                Log.d(EnginePreferences.LOG_TAG, "Received SensorEvent on Status tab");
                float[] originalValues = new float[3];
                System.arraycopy(sensorEvent.values, 0, originalValues, 0, 3);
                mEngine.newReading(sensorEvent.values);
                notifyClient(originalValues, sensorEvent.values);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };
    }

    private void notifyClient(float[] original, float[] processed) {
        try {
            Bundle values = new Bundle();
            values.putFloatArray(KEY_ORIGINAL_VALUES, original);
            values.putFloatArray(KEY_PROCESSED_VALUES, processed);
            Message message = Message.obtain(null, GyroService.NEW_READING);
            message.setData(values);
            mClientMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void play() {
        if (!mSensorActive) mSensorManager.registerListener(mGyroListener, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void pause() {
        if (mSensorActive) mSensorManager.unregisterListener(mGyroListener);
    }

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mServiceMessenger.getBinder();
    }

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GyroService.REGISTER_CLIENT:
                    Log.d(EnginePreferences.LOG_TAG, "GyroService: Received REGISTER_CLIENT");
                    mClientMessenger = msg.replyTo;
                    break;
                case GyroService.UNREGISTER_CLIENT:
                    Log.d(EnginePreferences.LOG_TAG, "GyroService: Received UNREGISTER_CLIENT");
                    mClientMessenger = null;
                    break;
                case GyroService.PLAY:
                    Log.d(EnginePreferences.LOG_TAG, "GyroService: Received PLAY");
                    play();
                    break;
                case GyroService.PAUSE:
                    Log.d(EnginePreferences.LOG_TAG, "GyroService: Received PAUSE");
                    pause();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    public class GyroBinder extends Binder {
        GyroService getService() {
            return GyroService.this;
        }
    }
}
