package net.davidcie.gyroscopebandaid.gui;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import net.davidcie.gyroscopebandaid.EnginePreferences;
import net.davidcie.gyroscopebandaid.R;
import net.davidcie.gyroscopebandaid.services.GyroService;

import java.util.Locale;

public class StatusTab extends Fragment {

    // Endpoint for services to send messages to StatusTab
    final Messenger mClientMessenger = new Messenger(new IncomingHandler());

    // Messanger for sending messages to gyroscope service over a connection
    Messenger mServiceMessenger = null;
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
            } catch (RemoteException e) {
                // Service crashed before we managed to connect?
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceMessenger = null;
            mServiceBound = false;
        }
    };

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (mServiceBound) {
            Message message = Message.obtain(null, isVisibleToUser ? GyroService.PLAY : GyroService.PAUSE);
            try {
                mServiceMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tab_status, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(EnginePreferences.LOG_TAG, "StatusTab: Trying to bind to service");
        Intent wantService = new Intent(getActivity(), GyroService.class);
        getActivity().bindService(wantService, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(EnginePreferences.LOG_TAG, "StatusTab: Unbinding from service");
        if (mServiceBound) {
            if (mServiceMessenger != null) {
                Message message = Message.obtain(null, GyroService.UNREGISTER_CLIENT);
                message.replyTo = mClientMessenger;
                try {
                    mServiceMessenger.send(message);
                } catch (RemoteException e) {
                    // Nothing we need to do, service crashed on its own.
                }
            }
            getActivity().unbindService(mServiceConnection);
            mServiceBound = false;
        }
    }

    private void updateValues(float[] original, float[] processed) {
        View view = getView();
        TextView temp;
        if (view == null) return;

        // X
        temp = view.findViewById(R.id.original_x);
        temp.setText(String.format(Locale.getDefault(), "%.10f", original[0]));
        // Y
        temp = view.findViewById(R.id.original_y);
        temp.setText(String.format(Locale.getDefault(), "%.10f", original[1]));
        // Z
        temp = view.findViewById(R.id.original_y);
        temp.setText(String.format(Locale.getDefault(), "%.10f", original[2]));
    }

    /**
     * Handler for incoming messages from the service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GyroService.NEW_READING:
                    Log.d(EnginePreferences.LOG_TAG, "StatusTab: Received NEW_READING");
                    Bundle data = msg.getData();
                    float[] original = data.getFloatArray(GyroService.KEY_ORIGINAL_VALUES);
                    float[] processed = data.getFloatArray(GyroService.KEY_PROCESSED_VALUES);
                    updateValues(original, processed);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
