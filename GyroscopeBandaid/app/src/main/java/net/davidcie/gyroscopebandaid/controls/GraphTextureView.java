package net.davidcie.gyroscopebandaid.controls;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.TextureView;

import net.davidcie.gyroscopebandaid.FifoArray;
import net.davidcie.gyroscopebandaid.R;
import net.davidcie.gyroscopebandaid.Util;

public class GraphTextureView extends TextureView {

    private int mWidth = 0;
    private int mHeight = 0;
    private int mSize;
    private final GraphTextureView mView = this;
    private GraphRendererThread mGraphUpdaterThread;
    private FifoArray<Float> mCollectionOne;
    private FifoArray<Float> mCollectionTwo;
    private FifoArray<Float> mCollectionThree;
    private int mValueEveryMs;

    public GraphTextureView(Context context) {
        super(context);
        initialize();
    }

    public GraphTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public GraphTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    @SuppressWarnings("unused")
    public GraphTextureView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    private void initialize() {
        setOpaque(false);
        setSurfaceTextureListener(new GraphTextureListener());
    }

    public void setCollections(FifoArray<Float> one, FifoArray<Float> two, FifoArray<Float> three) {
        if (!(one.size() == two.size() && two.size() == three.size())) {
            throw new IllegalArgumentException("Collections have to be the same size.");
        }
        mCollectionOne = one;
        mCollectionTwo = two;
        mCollectionThree = three;
        mSize = mCollectionOne.size();
    }

    public void setUpdateFrequency(int valueEveryMs) {
        mValueEveryMs = valueEveryMs;
    }

    private class GraphTextureListener implements TextureView.SurfaceTextureListener {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d(Util.LOG_TAG, "onSurfaceTextureAvailable");
            mWidth = getWidth();
            mHeight = getHeight();
            mGraphUpdaterThread = new GraphRendererThread();
            mGraphUpdaterThread.start();
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
            mWidth = getWidth();
            mHeight = getHeight();
            mGraphUpdaterThread.resize();
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    }

    private class GraphRendererThread extends Thread {

        private static final int TARGET_FPS = 30;
        private static final long TARGET_MS_PER_FRAME = (long) (1f / TARGET_FPS * 1000f);
        private static final int RANGE_Y = 2;  // -1..1

        private final int mFramesPerUpdate;
        private final long mMsPerFrame;
        private volatile boolean mRunning = true;

        /**
         * How much we multiply by to go from source X value to screen X value.
         */
        private float scaleXaxis;

        /**
         * How much we multiply by to go from source Y value to screen Y value.
         */
        private float scaleYaxis;

        /**
         * The amount we shift by every animation frame.
         */
        private float deltaXaxis;

        GraphRendererThread() {
            // Calculate how many frames of animation we can fit between new values
            mFramesPerUpdate = (int) Math.floor(mValueEveryMs / TARGET_MS_PER_FRAME);
            // Adjust Thread.wait time to best fit between animation frames
            mMsPerFrame = (long) (1000f / (mFramesPerUpdate * (1000f / mValueEveryMs)));
        }

        @Override
        public void run() {
            resize();
            Log.d(Util.LOG_TAG, "GraphRendererThread: mFramesPerUpdate=" + mFramesPerUpdate + " mMsPerFrame=" + mMsPerFrame);

            Paint paintOne = getPaint(R.color.color_graph_xaxis);
            Paint paintTwo = getPaint(R.color.color_graph_yaxis);
            Paint paintThree = getPaint(R.color.color_graph_zaxis);

            int animationFrame = 0;
            float frameDeltaX;
            final Float[] oneCopy = new Float[mSize];
            final Float[] twoCopy = new Float[mSize];
            final Float[] threeCopy = new Float[mSize];

            while (mRunning && !Thread.interrupted()) {
                final Canvas canvas = mView.lockCanvas(null);
                try {
                    // Check if we should move on to another point by comparing pointers
                    // at collection heads; they will point to different Floats
                    //noinspection NumberEquality
                    if (oneCopy[0] != mCollectionOne.get(0)) {
                        Log.v(Util.LOG_TAG, "GraphRendererThread: new value detected");
                        System.arraycopy(mCollectionOne.getUnderlyingArray(), 0, oneCopy, 0, mSize);
                        System.arraycopy(mCollectionTwo.getUnderlyingArray(), 0, twoCopy, 0, mSize);
                        System.arraycopy(mCollectionThree.getUnderlyingArray(), 0, threeCopy, 0, mSize);
                        animationFrame = 0;
                    }

                    // Clear canvas every frame
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                    // Paint collections
                    frameDeltaX = animationFrame * deltaXaxis;
                    graphCollection(oneCopy, canvas, paintOne, frameDeltaX);
                    graphCollection(twoCopy, canvas, paintTwo, frameDeltaX);
                    graphCollection(threeCopy, canvas, paintThree, frameDeltaX);

                    // Update animation frame
                    animationFrame = Math.min(mFramesPerUpdate, animationFrame + 1);

                } finally {
                    mView.unlockCanvasAndPost(canvas);
                }

                try {
                    Thread.sleep(mMsPerFrame);
                } catch (InterruptedException e) {
                    // Interrupted
                }
            }

            // Will naturally finish execution here if mRunning is set to false
        }

        private void graphCollection(Float[] collection, Canvas canvas, Paint paint, float frameXOffset) {
            float currentX, currentY, nextX, nextY;

            // Calculate starting point
            currentX = translateX(0) - frameXOffset;
            currentY = translateY(collection[0]);

            // Draw paths between all points
            for (int v = 1; v < mSize; v++) {
                nextX = translateX(v) - frameXOffset;
                nextY = translateY(collection[v]);

                Path p = new Path();
                p.moveTo(currentX, currentY);
                p.lineTo(nextX, nextY);
                canvas.drawPath(p, paint);

                currentX = nextX;
                currentY = nextY;
            }
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

        void resize() {
            scaleXaxis = (float)mWidth / (mSize - 1);
            scaleYaxis = (float)mHeight / RANGE_Y;
            deltaXaxis = scaleXaxis / mFramesPerUpdate;
            Log.d(Util.LOG_TAG, "GraphRendererThread.resize: scaleXaxis=" + scaleXaxis
                                + " scaleYaxis=" + scaleYaxis + " deltaXaxis=" + deltaXaxis);
        }

        void stopRendering() {
            interrupt();
            mRunning = false;
        }

        private float translateY(float from) {
            return (-from + 1) * scaleYaxis;
        }

        private float translateX(float from) {
            return from * scaleXaxis;
        }
    }
}
