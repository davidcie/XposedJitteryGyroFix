package net.davidcie.gyroscopebandaid.controls;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.os.TraceCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import net.davidcie.gyroscopebandaid.FifoArray;
import net.davidcie.gyroscopebandaid.R;
import net.davidcie.gyroscopebandaid.Util;

public class GraphTextureView extends View {

    private volatile int mWidth = 0;
    private volatile int mHeight = 0;
    private volatile int mSize;
    private volatile FifoArray<Float> mCollectionOne;
    private volatile FifoArray<Float> mCollectionTwo;
    private volatile FifoArray<Float> mCollectionThree;
    private int mValueEveryMs;
    private UpdateViewRunnable updateViewRunnable = new UpdateViewRunnable();
    private HandlerThread mHandlerThread;
    private volatile Handler handler;
    private volatile boolean mUpdateView = false;
    private volatile boolean mInitialized = false;

    // Migrated
    private static final int RANGE_Y = 2;  // -1..1
    private volatile float scaleXaxis;
    private volatile float scaleYaxis;
    private volatile float deltaXaxis;
    private volatile float frameDeltaX;
    private volatile Float[] oneCopy;
    private volatile Float[] twoCopy;
    private volatile Float[] threeCopy;
    private volatile float[] mTempPointsOne;
    private volatile float[] mTempPointsTwo;
    private volatile float[] mTempPointsThree;
    private volatile int animationFrame = 0;
    private final int mFramesPerUpdate = 6;
    private final Paint paintOne = getPaint(R.color.color_graph_xaxis);
    private final Paint paintTwo = getPaint(R.color.color_graph_yaxis);
    private final Paint paintThree = getPaint(R.color.color_graph_zaxis);
    private Paint white;
    private volatile Rect screen;

    public GraphTextureView(Context context) {
        super(context);
    }

    public GraphTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GraphTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressWarnings("unused")
    public GraphTextureView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onAttachedToWindow() {
        Log.d(Util.LOG_TAG, "onAttachedToWindow");
        super.onAttachedToWindow();
        mUpdateView = true;

        mHandlerThread = new HandlerThread("GraphUpdater", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();
        handler = new Handler(mHandlerThread.getLooper());
        handler.post(updateViewRunnable);
        //post(updateViewRunnable);
    }

    @Override
    protected void onDetachedFromWindow() {
        Log.d(Util.LOG_TAG, "onDetachedFromWindow");
        mUpdateView = false;
        if (mHandlerThread != null) mHandlerThread.quit();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //Rect zonk = canvas.getClipBounds();
        //Log.v(Util.LOG_TAG, "GraphTextureView.onDraw isHardwareAccelerated=" + canvas.isHardwareAccelerated());
        //Log.v(Util.LOG_TAG, "GraphTextureView.onDraw getClipBounds=(" + zonk.left + "," + zonk.top + "," + zonk.right + "," + zonk.bottom + ")");

        TraceCompat.beginSection("GraphTextureView.onDraw");
        //canvas.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);
        //canvas.drawRect(0, 0, mWidth, mHeight, white);
        canvas.drawRect(screen, white);
        canvas.drawLines(mTempPointsOne, paintOne);
        canvas.drawLines(mTempPointsTwo, paintTwo);
        canvas.drawLines(mTempPointsThree, paintThree);
        TraceCompat.endSection();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = getWidth();
        mHeight = getHeight();
        scaleXaxis = (float)mWidth / (mSize - 1);
        scaleYaxis = (float)mHeight / RANGE_Y;
        deltaXaxis = scaleXaxis / mFramesPerUpdate;
        screen = new Rect(0, 0, mWidth, mHeight);
    }

    public void initialize(FifoArray<Float> one, FifoArray<Float> two, FifoArray<Float> three, int valueEveryMs) {
        if (!(one.size() == two.size() && two.size() == three.size())) {
            throw new IllegalArgumentException("Collections have to be the same size.");
        }
        mCollectionOne = one;
        mCollectionTwo = two;
        mCollectionThree = three;
        mSize = mCollectionOne.size();

        oneCopy = new Float[mSize];
        twoCopy = new Float[mSize];
        threeCopy = new Float[mSize];
        mTempPointsOne = new float[mSize * 4];
        mTempPointsTwo = new float[mSize * 4];
        mTempPointsThree = new float[mSize * 4];

        mValueEveryMs = valueEveryMs;

        white = new Paint();
        white.setStyle(Paint.Style.FILL);
        white.setColor(Color.WHITE);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        mInitialized = true;
    }

    private Paint getPaint(int colorResource) {
        TypedValue alpha = new TypedValue();
        getResources().getValue(R.string.color_graph_alpha, alpha, true);

        Paint paint = new Paint();
        paint.setColor(getResources().getColor(colorResource));
        paint.setStrokeWidth(6);
        paint.setAlpha((int) (alpha.getFloat() * 100));
        paint.setStyle(Paint.Style.STROKE);
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setAntiAlias(true);
        return paint;
    }

    private float translateY(float from) {
        return (-from + 1) * scaleYaxis;
    }

    private float translateX(float from) {
        return from * scaleXaxis;
    }

    private void prepareCollection(Float[] collection, float[] tempPoints, float xOffset) {
        int basePtr;
        for (int v = 0; v < mSize - 1; v++) {
            basePtr = v * 4;
            tempPoints[basePtr] = translateX(v) - xOffset; //x0
            tempPoints[basePtr + 1] = translateY(collection[v]); //y0
            tempPoints[basePtr + 2] = translateX(v+1) - xOffset; //x1
            tempPoints[basePtr + 3] = translateY(collection[v+1]); //y1
        }
    }

    private class UpdateViewRunnable implements Runnable {
        public void run() {

            if (mUpdateView) handler.postDelayed(this, 16);
            if (!mInitialized || screen == null) return;

            TraceCompat.beginSection("UpdateViewRunnable prepare collections");
            boolean repaint = false;
            //noinspection NumberEquality
            if (oneCopy[0] != mCollectionOne.get(0)) {
                System.arraycopy(mCollectionOne.getUnderlyingArray(), 0, oneCopy, 0, mSize);
                System.arraycopy(mCollectionTwo.getUnderlyingArray(), 0, twoCopy, 0, mSize);
                System.arraycopy(mCollectionThree.getUnderlyingArray(), 0, threeCopy, 0, mSize);
                animationFrame = 0;
                repaint = true;
            } else if (animationFrame < mFramesPerUpdate) {
                animationFrame = Math.min(mFramesPerUpdate, animationFrame + 1);
                repaint = true;
            }

            TraceCompat.endSection();

            if (repaint) {
                frameDeltaX = animationFrame * deltaXaxis;
                prepareCollection(oneCopy, mTempPointsOne, frameDeltaX);
                prepareCollection(twoCopy, mTempPointsTwo, frameDeltaX);
                prepareCollection(threeCopy, mTempPointsThree, frameDeltaX);
                //invalidate(0, 0, mWidth, mHeight);
                //invalidate(screen);
                postInvalidate(0, 0, mWidth, mHeight);
            }
        }
    }
}
