package net.davidcie.gyroscopebandaid.gui;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
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
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import net.davidcie.gyroscopebandaid.R;
import net.davidcie.gyroscopebandaid.Util;
import net.davidcie.gyroscopebandaid.services.GyroService;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.Locale;

public class StatusTab extends Fragment {

    private boolean isVisible = false;
    private CircularFifoQueue<float[]> rawHistory = new CircularFifoQueue<>(3);
    private CircularFifoQueue<float[]> processedHistory = new CircularFifoQueue<>(3);

    // Endpoint for services to send messages to StatusTab
    final Messenger mClientMessenger = new Messenger(new IncomingHandler());

    // Messanger for sending messages to gyroscope service over a connection
    Messenger mServiceMessenger = null;
    boolean mServiceBound = false;

    // Charts
    LineChart chartX;
    LineChart chartY;
    LineChart chartZ;

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
                setServicePlayback(isVisible ? GyroService.PLAY : GyroService.PAUSE);
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
        Log.v(Util.LOG_TAG, "StatusTab: setUserVisibleHint(" + isVisibleToUser + ") ");
        super.setUserVisibleHint(isVisibleToUser);
        isVisible = isVisibleToUser;
        setServicePlayback(isVisible ? GyroService.PLAY : GyroService.PAUSE);
    }

    private void setServicePlayback(int playOrPause) {
        if (mServiceBound) {
            Message message = Message.obtain(null, playOrPause);
            try {
                mServiceMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /*gridLayout = (GridLayout) findViewById(R.id.gridlayout_main);
        gridLayout.setUseDefaultMargins(false);
        gridLayout.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        gridLayout.setRowOrderPreserved(false); remove grid padding*/
        View view = inflater.inflate(R.layout.tab_status, container, false);
        chartX = view.findViewById(R.id.chart_x);
        chartY = view.findViewById(R.id.chart_y);
        chartZ = view.findViewById(R.id.chart_z);

        setupChart(chartX);
        setupChart(chartY);
        setupChart(chartZ);
        return view;
    }

    private void setupChart(LineChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.setPinchZoom(false);
        chart.setAutoScaleMinMaxEnabled(false);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.WHITE);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);

        YAxis y = chart.getAxisLeft();
        y.setAxisMinimum(-1.0f);
        y.setAxisMaximum(1.0f);
        y.setDrawLabels(false);
        y.setDrawGridLines(false);
        y.setDrawAxisLine(false);

        XAxis x = chart.getXAxis();
        x.setDrawLabels(false);
        x.setDrawGridLines(false);
        x.setDrawAxisLine(false);

        LineData data = new LineData();
        chart.setData(data);

    }

    @Override
    public void onStart() {
        Log.d(Util.LOG_TAG, "StatusTab: onStart");
        super.onStart();
        Log.d(Util.LOG_TAG, "StatusTab: Trying to bind to service");
        Intent wantService = new Intent(getActivity(), GyroService.class);
        getActivity().bindService(wantService, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        Log.d(Util.LOG_TAG, "StatusTab: onStop");
        super.onStop();
    }

    @Override
    public void onPause() {
        Log.d(Util.LOG_TAG, "StatusTab: onPause");
        super.onPause();
        setServicePlayback(GyroService.PAUSE);
    }

    @Override
    public void onResume() {
        Log.d(Util.LOG_TAG, "StatusTab: onResume");
        super.onResume();
        if (isVisible) setServicePlayback(GyroService.PLAY);
    }

    @Override
    public void onDestroy() {
        Log.d(Util.LOG_TAG, "StatusTab: onDestroy");
        Log.d(Util.LOG_TAG, "StatusTab: Unbinding from service");
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
        super.onDestroy();
    }

    private void updateValues(float[] newOriginal, float[] newProcessed) {
        View view = getView();
        if (view == null) return;

        // Nicely print history
        // TODO: should really keep formatted values in memory
        boolean first = true;
        StringBuilder builderX = new StringBuilder();
        StringBuilder builderY = new StringBuilder();
        StringBuilder builderZ = new StringBuilder();
        for (float[] raw : rawHistory) {
            if (first) {
                first = false;
            }
            else {
                builderX.append("\n");
                builderY.append("\n");
                builderZ.append("\n");
            }
            builderX.append(String.format(Locale.getDefault(), "%.10f", raw[0]));
            builderY.append(String.format(Locale.getDefault(), "%.10f", raw[1]));
            builderZ.append(String.format(Locale.getDefault(), "%.10f", raw[2]));
        }

        // Assign formatted history values to text boxes
        ((TextView) view.findViewById(R.id.original_x)).setText(builderX.toString());
        ((TextView) view.findViewById(R.id.original_y)).setText(builderY.toString());
        ((TextView) view.findViewById(R.id.original_z)).setText(builderZ.toString());

        // Update charts
        LineData dataX = chartX.getData();
        float newValueX = Util.limit(newOriginal[0], -1.0f, 1.0f);
        if (dataX != null) {
            ILineDataSet setX = dataX.getDataSetByIndex(0);
            if (setX == null) {
                LineDataSet set = new LineDataSet(null, null);
                set.setDrawValues(false);
                set.setLineWidth(2f);
                set.setDrawCircles(false);
                dataX.addDataSet(set);
                setX = set;
            }
            //noinspection SuspiciousNameCombination
            dataX.addEntry(new Entry(setX.getEntryCount(), newValueX), 0);
            dataX.notifyDataChanged();
            chartX.notifyDataSetChanged();
            chartX.setVisibleXRangeMaximum(15);
            chartX.moveViewToX(dataX.getEntryCount());
        }
    }

    /**
     * Handler for incoming messages from the service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GyroService.NEW_READING:
                    Bundle data = msg.getData();
                    float[] newOriginal = data.getFloatArray(GyroService.KEY_ORIGINAL_VALUES);
                    float[] newProcessed = data.getFloatArray(GyroService.KEY_PROCESSED_VALUES);
                    rawHistory.add(newOriginal);
                    processedHistory.add(newProcessed);
                    updateValues(newOriginal, newProcessed);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
