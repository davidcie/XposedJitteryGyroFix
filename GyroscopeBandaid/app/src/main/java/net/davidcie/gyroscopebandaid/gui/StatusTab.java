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

    private final static int UPDATE_EVERY_MS = 100;
    private boolean mIsVisible = false;
    private CircularFifoQueue<float[]> mRawHistory = new CircularFifoQueue<>(3);
    private CircularFifoQueue<float[]> mCookedHistory = new CircularFifoQueue<>(3);
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
                    mRawHistory.add(raw);
                    mCookedHistory.add(cooked);
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

        Intent wantService = new Intent(getActivity(), GyroService.class);
        getActivity().bindService(wantService, mServiceConnection, Context.BIND_AUTO_CREATE);

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
        chart.setBackgroundColor(Color.WHITE);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setVisibleXRangeMaximum(5);

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
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);

        LineData data = new LineData();
        data.addDataSet(dataSet);
        data.addEntry(new Entry(0, 0), 0);
        chart.setData(data);
    }

    private void updateValues(float[] latestRaw, float[] latestCooked) {
        View view = getView();
        if (view == null) return;

        // Nicely print history
        // TODO: should really keep formatted values in memory
        boolean first = true;
        StringBuilder builderX = new StringBuilder();
        StringBuilder builderY = new StringBuilder();
        StringBuilder builderZ = new StringBuilder();
        for (float[] raw : mRawHistory) {
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
        updateValuesGraph(Util.limit(latestRaw[0], -1.0f, 1.0f), chartX);
        updateValuesGraph(Util.limit(latestRaw[1], -1.0f, 1.0f), chartY);
        updateValuesGraph(Util.limit(latestRaw[2], -1.0f, 1.0f), chartZ);
    }

    private void updateValuesGraph(float newValue, LineChart chart) {
        LineData data = chart.getData();
        if (data == null) return;
        ILineDataSet set = data.getDataSetByIndex(0);
        data.addEntry(new Entry(set.getEntryCount(), newValue), 0);
        chart.notifyDataSetChanged();
        chart.moveViewToAnimated(data.getEntryCount(), 0.0f, YAxis.AxisDependency.LEFT, UPDATE_EVERY_MS);
        chart.setVisibleXRangeMaximum(15);
    }
}
