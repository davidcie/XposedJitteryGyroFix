package net.davidcie.gyroscopebandaid.gui;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;

import net.davidcie.gyroscopebandaid.R;
import net.davidcie.gyroscopebandaid.Util;
import net.davidcie.gyroscopebandaid.controls.VerticalScrollingTextView;
import net.davidcie.gyroscopebandaid.services.GyroService;

import java.util.Locale;

public class StatusTab extends Fragment {

    private final static int UPDATE_EVERY_MS = 250;

    private TextureView mTextureView;
    private RenderThread mThread;
    private int mWidth;
    private int mHeight;


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

        mTextureView = view.findViewById(R.id.myTexture);
        mTextureView.setSurfaceTextureListener(new CanvasListener());
        mTextureView.setOpaque(false);

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

    @SuppressLint("SetTextI18n")
    private void updateValues(float[] latestRaw, float[] latestCooked) {
        // Update running history
        updateValueList(historyViewX, latestRaw[0]);
        updateValueList(historyViewY, latestRaw[1]);
        updateValueList(historyViewZ, latestRaw[2]);
    }

    private void updateValueList(VerticalScrollingTextView view, float newValue) {
        StringBuilder builder = new StringBuilder(view.getText());
        if (builder.length() > 0) builder.append("\n");
        builder.append(String.format(Locale.getDefault(), "%.10f", newValue));
        //view.setText(builder.toString());
        //view.scroll();
    }


    private class RenderThread extends Thread {
        private static final long FPS = (long) (1f / 60f * 1000f);
        private volatile boolean mRunning = true;

        private static final int RANGE_X = 20; // 0..20
        private static final int RANGE_Y = 2;  // -1..1

        private float scaleX;
        private float scaleY;
        private float[] values = new float[20];

        @Override
        public void run() {

            scaleX = (float)mWidth / RANGE_X;
            scaleY = (float)mHeight / RANGE_Y;

            Paint paint = new Paint();
            paint.setColor(0xff00ff00);
            paint.setColor(Color.RED);

            // Initialize graph
            for (int v = 0; v < values.length; v++) {
                values[v] = (float) (Math.random() * 2 - 1);
            }

            while (mRunning && !Thread.interrupted()) {
                final Canvas canvas = mTextureView.lockCanvas(null);
                try {
                    canvas.drawColor(0x00000000, PorterDuff.Mode.CLEAR);
                    paint.setStrokeWidth(5);
                    paint.setStyle(Paint.Style.STROKE);

                    float currentX, currentY, nextX, nextY;
                    currentX = 0;
                    currentY = values[0] * scaleY;

                    for (int v = 1; v < values.length; v++) {
                        nextX = v * scaleX;
                        nextY = values[v] * scaleY;

                        Path p = new Path();
                        p.moveTo(currentX, currentY);
                        p.lineTo(nextX, nextY);
                        canvas.drawPath(p, paint);

                        currentX = nextX;
                        currentY = nextY;
                    }

                } finally {
                    mTextureView.unlockCanvasAndPost(canvas);
                }

                try {
                    Thread.sleep(FPS);
                } catch (InterruptedException e) {
                    // Interrupted
                }
            }
        }

        public void stopRendering() {
            interrupt();
            mRunning = false;
        }

    }


    private class CanvasListener implements TextureView.SurfaceTextureListener {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                              int width, int height) {
            Log.d(Util.LOG_TAG, "onSurfaceTextureAvailable");
            mThread = new RenderThread();
            mThread.start();
            mWidth = mTextureView.getWidth();
            mHeight = mTextureView.getHeight();
            Log.d(Util.LOG_TAG, "width: " + mWidth + " height: " + height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.d(Util.LOG_TAG, "onSurfaceTextureDestroyed");
            if (mThread != null) {
                mThread.stopRendering();
            }
            return true;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                                int width, int height) {
            Log.d(Util.LOG_TAG, "onSurfaceTextureSizeChanged");
            mWidth = mTextureView.getWidth();
            mHeight = mTextureView.getHeight();
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            //Log.d(Util.LOG_TAG, "onSurfaceTextureUpdated");
        }
    }
}
