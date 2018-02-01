package net.davidcie.gyroscopebandaid.gui;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.TextView;

import net.davidcie.gyroscopebandaid.Const;
import net.davidcie.gyroscopebandaid.FifoArray;
import net.davidcie.gyroscopebandaid.R;
import net.davidcie.gyroscopebandaid.Util;
import net.davidcie.gyroscopebandaid.controls.GraphTextureView;
import net.davidcie.gyroscopebandaid.services.GyroService;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class StatusTab extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    // Settings
    private final static int UPDATE_EVERY_MS = 100;
    private final static int GRAPH_VALUES = 21;
    private final static NumberFormat plusMinus = new DecimalFormat("+00.000000000;-00.000000000"); //minus
    //private final static NumberFormat plusMinus = new DecimalFormat("+00.000000000;–00.000000000"); //ndash

    // Collections
    private FifoArray<Float> rawX = new FifoArray<>(GRAPH_VALUES);
    private FifoArray<Float> rawY = new FifoArray<>(GRAPH_VALUES);
    private FifoArray<Float> rawZ = new FifoArray<>(GRAPH_VALUES);

    // Utility variables
    private View mView;
    private boolean mIsVisible = false;
    private Handler mUpdaterThread = new Handler();
    private Runnable mRequestReadingTask = new Runnable() {
        @Override
        public void run() {
            if (mServiceBound) {
                Message message = Message.obtain(null, GyroService.REQUEST_READING);
                try { mServiceMessenger.send(message); }
                catch (RemoteException ignored) { }
            }
            mUpdaterThread.postDelayed(this, UPDATE_EVERY_MS);
        }
    };

    //region Service interaction

    /**
     * Endpoint for services to send messages to this Fragment.
     */
    final Messenger mClientMessenger = new Messenger(new IncomingHandler());

    /**
     * Messanger for sending messages to service over a connection.
     */
    Messenger mServiceMessenger = null;

    /**
     * Lets us know if we are tied to the service.
     */
    boolean mServiceBound = false;

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mServiceMessenger = new Messenger(service);
            mServiceBound = true;

            try {
                Message message = Message.obtain(null, GyroService.REGISTER_CLIENT);
                message.replyTo = mClientMessenger;
                mServiceMessenger.send(message);
                setServicePlayback(mIsVisible ? GyroService.PLAY : GyroService.PAUSE);
            } catch (RemoteException ignored) {
                // Service crashed before we managed to connect?
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceMessenger = null;
            mServiceBound = false;
        }
    };

    /**
     * Handler for incoming messages from the service.
     */
    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GyroService.SEND_READING:
                    Bundle data = msg.getData();
                    float[] raw = data.getFloatArray(GyroService.KEY_RAW_VALUES);
                    float[] cooked = data.getFloatArray(GyroService.KEY_COOKED_VALUES);
                    updateValues(raw, cooked);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    private void setServicePlayback(int playOrPause) {
        if (mServiceBound) {
            Message message = Message.obtain(null, playOrPause);
            try { mServiceMessenger.send(message); }
            catch (RemoteException ignored) { }
        }
    }

    //endregion

    //region Fragment overrides

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mView = inflater.inflate(R.layout.status, container, false);

        Intent wantService = new Intent(getActivity(), GyroService.class);
        getActivity().bindService(wantService, mServiceConnection, Context.BIND_AUTO_CREATE);

        // Initialize collections
        for (int i = 0; i < rawX.size(); i++) {
            rawX.add(0f);
            rawY.add(0f);
            rawZ.add(0f);
        }

        GraphTextureView mRawGraphView = mView.findViewById(R.id.graph_raw);
        mRawGraphView.setCollections(rawX, rawY, rawZ);
        mRawGraphView.setUpdateFrequency(UPDATE_EVERY_MS);

        initializeFilterStatus();

        return mView;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (mView == null) return;
        CheckedTextView s;
        switch (key) {
            case Const.PREF_CALIBRATION_ENABLED:
                s = mView.findViewById(R.id.calibration_status);
                s.setChecked(sharedPreferences.getBoolean(key, false));
                break;
            case Const.PREF_INVERSION_ENABLED:
                s = mView.findViewById(R.id.inversion_status);
                s.setChecked(sharedPreferences.getBoolean(key, false));
                break;
            case Const.PREF_SMOOTHING_ENABLED:
                s = mView.findViewById(R.id.smoothing_status);
                s.setChecked(sharedPreferences.getBoolean(key, false));
                break;
            case Const.PREF_THRESHOLDING_ENABLED:
                s = mView.findViewById(R.id.thresholding_status);
                s.setChecked(sharedPreferences.getBoolean(key, false));
                break;
            case Const.PREF_ROUNDING_ENABLED:
                s = mView.findViewById(R.id.rounding_status);
                s.setChecked(sharedPreferences.getBoolean(key, false));
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
        if (mIsVisible) setServicePlayback(GyroService.PLAY);
        mUpdaterThread.post(mRequestReadingTask);
    }

    @Override
    public void onPause() {
        mUpdaterThread.removeCallbacks(mRequestReadingTask);
        setServicePlayback(GyroService.PAUSE);
        PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mServiceBound) {
            if (mServiceMessenger != null) {
                Message message = Message.obtain(null, GyroService.UNREGISTER_CLIENT);
                message.replyTo = mClientMessenger;
                try {
                    mServiceMessenger.send(message);
                } catch (RemoteException e) {
                    // Nothing we need to do, service crashed on its own?
                }
            }
            getActivity().unbindService(mServiceConnection);
            mServiceBound = false;
        }
        super.onDestroy();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.v(Util.LOG_TAG, "StatusTab: setUserVisibleHint(" + isVisibleToUser + ")");
        mIsVisible = isVisibleToUser;
        if (mIsVisible) mUpdaterThread.post(mRequestReadingTask);
        else mUpdaterThread.removeCallbacks(mRequestReadingTask);
        setServicePlayback(mIsVisible ? GyroService.PLAY : GyroService.PAUSE);
    }


    //endregion

    private void initializeFilterStatus() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        CheckedTextView s;
        if (prefs == null || mView == null) return;
        s = mView.findViewById(R.id.calibration_status);
        s.setChecked(prefs.getBoolean(Const.PREF_CALIBRATION_ENABLED, false));
        s = mView.findViewById(R.id.inversion_status);
        s.setChecked(prefs.getBoolean(Const.PREF_INVERSION_ENABLED, false));
        s = mView.findViewById(R.id.smoothing_status);
        s.setChecked(prefs.getBoolean(Const.PREF_SMOOTHING_ENABLED, false));
        s = mView.findViewById(R.id.thresholding_status);
        s.setChecked(prefs.getBoolean(Const.PREF_THRESHOLDING_ENABLED, false));
        s = mView.findViewById(R.id.rounding_status);
        s.setChecked(prefs.getBoolean(Const.PREF_ROUNDING_ENABLED, false));
    }

    @SuppressLint("SetTextI18n")
    private void updateValues(float[] latestRaw, float[] latestCooked) {
        updateText(R.id.raw_x, latestRaw[0]);
        updateText(R.id.raw_y, latestRaw[1]);
        updateText(R.id.raw_z, latestRaw[2]);

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
