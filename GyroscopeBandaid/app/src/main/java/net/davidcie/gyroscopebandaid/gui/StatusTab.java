package net.davidcie.gyroscopebandaid.gui;

import android.annotation.SuppressLint;
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
import android.widget.GridLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import net.davidcie.gyroscopebandaid.R;
import net.davidcie.gyroscopebandaid.Util;
import net.davidcie.gyroscopebandaid.controls.VerticalScrollingTextView;
import net.davidcie.gyroscopebandaid.services.GyroService;

import java.util.Locale;

public class StatusTab extends Fragment {

    private final static int UPDATE_EVERY_MS = 250;
    private final static int CHART_VALUES = 15;

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

    // Charts
    LineChart chartX;
    LineChart chartY;
    LineChart chartZ;
    VerticalScrollingTextView historyViewX;
    VerticalScrollingTextView historyViewY;
    VerticalScrollingTextView historyViewZ;

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
        View view = inflater.inflate(R.layout.status, container, false);

        Intent wantService = new Intent(getActivity(), GyroService.class);
        getActivity().bindService(wantService, mServiceConnection, Context.BIND_AUTO_CREATE);

        // Remove grid padding
        GridLayout gridRaw = view.findViewById(R.id.grid_raw);
        gridRaw.setUseDefaultMargins(false);
        gridRaw.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        gridRaw.setRowOrderPreserved(false);

        chartX = view.findViewById(R.id.chart_x);
        chartY = view.findViewById(R.id.chart_y);
        chartZ = view.findViewById(R.id.chart_z);
        setupChart(chartX);
        setupChart(chartY);
        setupChart(chartZ);

        historyViewX = view.findViewById(R.id.original_x);
        historyViewX.setLinesPerSecond(1000.0f/UPDATE_EVERY_MS);
        historyViewY = view.findViewById(R.id.original_y);
        historyViewY.setLinesPerSecond(1000.0f/UPDATE_EVERY_MS);
        historyViewZ = view.findViewById(R.id.original_z);
        historyViewZ.setLinesPerSecond(1000.0f/UPDATE_EVERY_MS);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mIsVisible) setServicePlayback(GyroService.PLAY);
        mUpdaterThread.post(mRequestReadingTask);
    }

    @Override
    public void onPause() {
        mUpdaterThread.removeCallbacks(mRequestReadingTask);
        setServicePlayback(GyroService.PAUSE);
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

    private void setupChart(LineChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.setPinchZoom(false);
        chart.setAutoScaleMinMaxEnabled(false);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);
        chart.setViewPortOffsets(0f, 0f, 0f, 0f);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);

        YAxis y = chart.getAxisLeft();
        y.setAxisMinimum(-1.1f);
        y.setAxisMaximum(1.1f);
        y.setDrawLabels(false);
        y.setDrawGridLines(false);
        y.setDrawAxisLine(false);

        XAxis x = chart.getXAxis();
        x.setDrawLabels(false);
        x.setDrawGridLines(false);
        x.setDrawAxisLine(false);

        LineDataSet dataSet = new LineDataSet(null, null);
        dataSet.setDrawValues(false);
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        //dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        //dataSet.setCubicIntensity(0.2f);
        for (int e = 0; e < CHART_VALUES; e++) dataSet.addEntry(new Entry(e, 0.0f));

        LineData data = new LineData();
        data.addDataSet(dataSet);
        chart.setData(data);
    }

    @SuppressLint("SetTextI18n")
    private void updateValues(float[] latestRaw, float[] latestCooked) {
        // Update running history
        updateValueList(historyViewX, latestRaw[0]);
        updateValueList(historyViewY, latestRaw[1]);
        updateValueList(historyViewZ, latestRaw[2]);

        // Update charts
        updateValueGraph(chartX, Util.limit(latestRaw[0], -1.0f, 1.0f));
        updateValueGraph(chartY, Util.limit(latestRaw[1], -1.0f, 1.0f));
        updateValueGraph(chartZ, Util.limit(latestRaw[2], -1.0f, 1.0f));
    }

    private void updateValueList(VerticalScrollingTextView view, float newValue) {
        StringBuilder builder = new StringBuilder(view.getText());
        if (builder.length() > 0) builder.append("\n");
        builder.append(String.format(Locale.getDefault(), "%.10f", newValue));
        view.setText(builder.toString());
        view.scroll();
    }

    private void updateValueGraph(LineChart chart, float newValue) {
        LineData data = chart.getData();
        if (data == null) return;
        ILineDataSet set = data.getDataSetByIndex(0);
        set.addEntry(new Entry(set.getEntryCount(), newValue));
        //data.addEntry(new Entry(set.getEntryCount(), newValue), 0);
        data.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.setVisibleXRangeMaximum(CHART_VALUES);
        //chart.moveViewToX(data.getEntryCount());
        chart.moveViewToAnimated(data.getEntryCount(), 0.0f, YAxis.AxisDependency.LEFT, 20);
    }
}
