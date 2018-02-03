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
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import net.davidcie.gyroscopebandaid.Engine;
import net.davidcie.gyroscopebandaid.Util;

public class GyroService extends Service {

    //region Supported messages

    public static final int REGISTER_CLIENT = 1;
    public static final int UNREGISTER_CLIENT = 2;
    public static final int BROADCAST_ENABLE = 3;
    public static final int BROADCAST_DISABLE = 4;
    public static final int PAUSE = 10;
    public static final int PLAY = 11;
    public static final int SEND_READING = 12;
    public static final int REQUEST_READING = 13;
    public static final String KEY_RAW_VALUES = "original";
    public static final String KEY_COOKED_VALUES = "processed";

    //endregion

    // Endpoint for clients to send messages to GyroService
    final Messenger mServiceMessenger = new Messenger(new IncomingHandler());

    // Endpoint for talking to the client
    Messenger mClientMessenger = null;

    // Flag if we should immediately update client when a new reading arrives
    boolean mBroadcast = false;

    final float[] mLastRawValue = new float[3];
    final float[] mLastCookedValue = new float[3];

    // Sensor management
    private Engine mEngine;
    private SensorManager mSensorManager;
    private Sensor mGyroscope;
    private SensorEventListener mGyroListener;
    private boolean mSensorActive = false;
    private HandlerThread mHandlerThread;
    Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();

        mEngine = new Engine(false, PreferenceManager.getDefaultSharedPreferences(this));
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //noinspection ConstantConditions
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mGyroListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                //Log.v(Util.LOG_TAG, "GyroService: Received SensorEvent " + Util.printArray(sensorEvent.values));
                System.arraycopy(sensorEvent.values, 0, mLastRawValue, 0, 3);
                mEngine.newReading(sensorEvent.values);
                System.arraycopy(sensorEvent.values, 0, mLastCookedValue, 0, 3);
                if (mBroadcast) notifyClient();
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };
        mHandlerThread = new HandlerThread("AccelerometerLogListener");
        mHandlerThread.start();
        handler = new Handler(mHandlerThread.getLooper());
    }

    private void notifyClient() {
        try {
            Bundle values = new Bundle();
            float[] lastRawCopy = new float[mLastRawValue.length];
            float[] lastCookedCopy = new float[mLastCookedValue.length];
            System.arraycopy(mLastRawValue, 0, lastRawCopy, 0, mLastRawValue.length);
            System.arraycopy(mLastCookedValue, 0, lastCookedCopy, 0, mLastCookedValue.length);
            values.putFloatArray(KEY_RAW_VALUES, lastRawCopy);
            values.putFloatArray(KEY_COOKED_VALUES, lastCookedCopy);
            Message message = Message.obtain(null, GyroService.SEND_READING);
            message.setData(values);
            mClientMessenger.send(message);
        } catch (RemoteException ignored) {
            // Client crashed or is not listening, no big deal!
        }
    }

    public void play() {
        if (!mSensorActive && mGyroscope != null) {
            Log.d(Util.LOG_TAG, "GyroService: Enabling sensor");
            mSensorManager.registerListener(mGyroListener, mGyroscope, SensorManager.SENSOR_DELAY_GAME, handler);
            mSensorActive = true;
        }
    }

    public void pause() {
        if (mSensorActive) {
            Log.d(Util.LOG_TAG, "GyroService: Disabling sensor");
            mSensorManager.unregisterListener(mGyroListener, mGyroscope);
            mSensorActive = false;
        }
    }

    /**
     * When anyone tries to bind to the service, return messenger
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
                    Log.d(Util.LOG_TAG, "GyroService: Received REGISTER_CLIENT");
                    mClientMessenger = msg.replyTo;
                    break;
                case GyroService.UNREGISTER_CLIENT:
                    Log.d(Util.LOG_TAG, "GyroService: Received UNREGISTER_CLIENT");
                    pause();
                    mClientMessenger = null;
                    break;
                case GyroService.PLAY:
                    Log.d(Util.LOG_TAG, "GyroService: Received PLAY");
                    play();
                    break;
                case GyroService.PAUSE:
                    Log.d(Util.LOG_TAG, "GyroService: Received PAUSE");
                    pause();
                    break;
                case GyroService.BROADCAST_ENABLE:
                    Log.d(Util.LOG_TAG, "GyroService: Received BROADCAST_ENABLE");
                    mBroadcast = true;
                    break;
                case GyroService.BROADCAST_DISABLE:
                    Log.d(Util.LOG_TAG, "GyroService: Received BROADCAST_DISABLE");
                    mBroadcast = false;
                    break;
                case GyroService.REQUEST_READING:
                    //Log.d(Util.LOG_TAG, "GyroService: Received REQUEST_READING");
                    notifyClient();
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
