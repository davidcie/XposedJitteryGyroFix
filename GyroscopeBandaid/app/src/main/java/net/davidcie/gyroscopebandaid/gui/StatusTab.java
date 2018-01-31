package net.davidcie.gyroscopebandaid.gui;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.Switch;
import android.widget.TextView;

import net.davidcie.gyroscopebandaid.Const;
import net.davidcie.gyroscopebandaid.FifoArray;
import net.davidcie.gyroscopebandaid.R;
import net.davidcie.gyroscopebandaid.Util;
import net.davidcie.gyroscopebandaid.services.GyroService;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class StatusTab extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    protected final static int UPDATE_EVERY_MS = 100;
    private final static int GRAPH_VALUES = 21;

    private TextureView mRawGraphView;
    private GraphRenderer mGraphUpdaterThread;
    private int mWidth;
    private int mHeight;

    private FifoArray<Float> rawX = new FifoArray<>(GRAPH_VALUES);
    private FifoArray<Float> rawY = new FifoArray<>(GRAPH_VALUES);
    private FifoArray<Float> rawZ = new FifoArray<>(GRAPH_VALUES);

    private NumberFormat plusMinus = new DecimalFormat("+00.000000000;–00.000000000");
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

    private void initializeFilterStatus(View view) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        CheckedTextView s;
        if (prefs == null || view == null) return;
        s = view.findViewById(R.id.calibration_status);
        s.setChecked(prefs.getBoolean(Const.PREF_CALIBRATION_ENABLED, false));
        s = view.findViewById(R.id.inversion_status);
        s.setChecked(prefs.getBoolean(Const.PREF_INVERSION_ENABLED, false));
        s = view.findViewById(R.id.smoothing_status);
        s.setChecked(prefs.getBoolean(Const.PREF_SMOOTHING_ENABLED, false));
        s = view.findViewById(R.id.thresholding_status);
        s.setChecked(prefs.getBoolean(Const.PREF_THRESHOLDING_ENABLED, false));
        s = view.findViewById(R.id.rounding_status);
        s.setChecked(prefs.getBoolean(Const.PREF_ROUNDING_ENABLED, false));
    }

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

        for (int i = 0; i < GRAPH_VALUES; i++) {
            rawX.add(0f);
            rawY.add(0f);
            rawZ.add(0f);
        }

        mRawGraphView = view.findViewById(R.id.graph_raw);
        mRawGraphView.setSurfaceTextureListener(new CanvasListener());
        mRawGraphView.setOpaque(false);

        initializeFilterStatus(view);

        return view;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        View view = getView();
        if (view == null) return;
        CheckedTextView s;
        switch (key) {
            case Const.PREF_CALIBRATION_ENABLED:
                s = view.findViewById(R.id.calibration_status);
                s.setChecked(sharedPreferences.getBoolean(key, false));
                break;
            case Const.PREF_INVERSION_ENABLED:
                s = view.findViewById(R.id.inversion_status);
                s.setChecked(sharedPreferences.getBoolean(key, false));
                break;
            case Const.PREF_SMOOTHING_ENABLED:
                s = view.findViewById(R.id.smoothing_status);
                s.setChecked(sharedPreferences.getBoolean(key, false));
                break;
            case Const.PREF_THRESHOLDING_ENABLED:
                s = view.findViewById(R.id.thresholding_status);
                s.setChecked(sharedPreferences.getBoolean(key, false));
                break;
            case Const.PREF_ROUNDING_ENABLED:
                s = view.findViewById(R.id.rounding_status);
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
        //mThread.run();
    }

    @Override
    public void onPause() {
        //mThread.interrupt();
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
        View view = getView();
        if (view == null) return;
        TextView text = view.findViewById(id);
        if (text == null) return;
        text.setText(plusMinus.format(value));
    }

    private class GraphRenderer extends Thread {

        private static final int TARGET_FPS = 30;
        private static final long TARGET_MS_PER_FRAME = (long) (1f / TARGET_FPS * 1000f);
        private int FRAMES_PER_UPDATE;
        private long OPTIMAL_MS_PER_FRAME;

        private volatile boolean mRunning = true;

        private static final int RANGE_X = 20; // 0..20
        private static final int RANGE_Y = 2;  // -1..1

        private float scaleX;
        private float scaleY;
        private float deltaX;

        FifoArray<Float> collection;

        public GraphRenderer(FifoArray<Float> collection) {
            // Calculate how many frames of animation we can fit between new values
            FRAMES_PER_UPDATE = (int) Math.floor(UPDATE_EVERY_MS / TARGET_MS_PER_FRAME);
            OPTIMAL_MS_PER_FRAME = (long) (1000f / (FRAMES_PER_UPDATE * (1000f / UPDATE_EVERY_MS)));
            this.collection = collection;
        }

        @Override
        public void run() {

            scaleX = (float)mWidth / RANGE_X;
            scaleY = (float)mHeight / RANGE_Y;
            deltaX = scaleX / FRAMES_PER_UPDATE; // we shift by this amount every animation frame

            Log.d(Util.LOG_TAG, "TARGET_MS_PER_FRAME=" + TARGET_MS_PER_FRAME + " FRAMES_PER_UPDATE="
                                + FRAMES_PER_UPDATE + " OPTIMAL_MS_PER_FRAME=" + OPTIMAL_MS_PER_FRAME
                                + " deltaX=" + deltaX + " scaleX=" + scaleX + " UPDATE_EVERY_MS=" + UPDATE_EVERY_MS
                                + " scaleY=" + scaleY);

            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStrokeWidth(5);
            paint.setStyle(Paint.Style.STROKE);
            paint.setFlags(Paint.ANTI_ALIAS_FLAG);
            paint.setAntiAlias(true);

            //int animationCountdown = 0;
            // we have 16.6ms per frame
            // new values appear every 100ms
            // wa have at most 100/16 = 6 frames
            // let's give ourself half of the most optimistic case

            int animationFrame = 0;
            float currentX, currentY, nextX, nextY, frameDeltaX;
            final Float[] valueCopy = new Float[GRAPH_VALUES];

            while (mRunning && !Thread.interrupted()) {
                final Canvas canvas = mRawGraphView.lockCanvas(null);
                try {
                    // Check if we should move on to another point by comparing pointers
                    // at collection heads; they will point to different Floats
                    //noinspection NumberEquality
                    if (valueCopy[0] != collection.get(0)) {
                        Log.v(Util.LOG_TAG, "Switching to a new value");
                        System.arraycopy(collection.getUnderlyingArray(), 0, valueCopy, 0, GRAPH_VALUES);
                        animationFrame = 0;
                    }

                    // Clear canvas every frame
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                    // Calculate starting point
                    frameDeltaX = animationFrame * deltaX;
                    currentX = translateX(0) - frameDeltaX;
                    currentY = translateY(collection.get(0));

                    for (int v = 1; v < collection.size(); v++) {
                        nextX = translateX(v) - frameDeltaX;
                        nextY = translateY(collection.get(v));

                        Path p = new Path();
                        p.moveTo(currentX, currentY);
                        p.lineTo(nextX, nextY);
                        canvas.drawPath(p, paint);

                        currentX = nextX;
                        currentY = nextY;
                    }

                    // Update animation frame
                    animationFrame = Math.min(FRAMES_PER_UPDATE, animationFrame + 1);

                } finally {
                    mRawGraphView.unlockCanvasAndPost(canvas);
                }

                try {
                    Thread.sleep(TARGET_MS_PER_FRAME);
                } catch (InterruptedException e) {
                    // Interrupted
                }
            }
        }

        public void resize() {
            Log.d(Util.LOG_TAG, "Thread.resize baby");
            scaleX = (float)mWidth / RANGE_X;
            scaleY = (float)mHeight / RANGE_Y;
        }

        public void stopRendering() {
            interrupt();
            mRunning = false;
        }

        private float translateY(float from) {
            float result = (-from + 1) * scaleY;
            //if (result != 0.0f) Log.v(Util.LOG_TAG, "from=" + from + " to=" + result);
            return result;
        }

        private float translateX(float from) {
            return from * scaleX;
        }
    }


    private class CanvasListener implements TextureView.SurfaceTextureListener {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d(Util.LOG_TAG, "onSurfaceTextureAvailable");
            mWidth = mRawGraphView.getWidth();
            mHeight = mRawGraphView.getHeight();
            mGraphUpdaterThread = new GraphRenderer(rawX);
            mGraphUpdaterThread.start();
            Log.d(Util.LOG_TAG, "width: " + mWidth + " height: " + height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.d(Util.LOG_TAG, "onSurfaceTextureDestroyed");
            if (mGraphUpdaterThread != null) {
                mGraphUpdaterThread.stopRendering();
            }
            return true;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.d(Util.LOG_TAG, "onSurfaceTextureSizeChanged");
            mWidth = mRawGraphView.getWidth();
            mHeight = mRawGraphView.getHeight();
            mGraphUpdaterThread.resize();
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            //Log.d(Util.LOG_TAG, "onSurfaceTextureUpdated");
        }
    }
}
