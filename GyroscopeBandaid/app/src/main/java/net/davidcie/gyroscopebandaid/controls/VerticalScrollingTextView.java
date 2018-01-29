package net.davidcie.gyroscopebandaid.controls;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.Nullable;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;
import android.widget.TextView;

import net.davidcie.gyroscopebandaid.Util;

@SuppressWarnings({"unused", "SameParameterValue"})
@SuppressLint("AppCompatCustomView")
public class VerticalScrollingTextView extends TextView {

    private static final float DEFAULT_LINES_PER_SECOND = 0.5f;
    private float mLinesPerSecond = DEFAULT_LINES_PER_SECOND;
    private Scroller mLinearScroller;

    private int mLineHeight = 0;
    private int mStartY = 0;
    private boolean mIsFirstScroll = true;
    private int mPreviousLineCount = 0;

    public VerticalScrollingTextView(Context context) {
        super(context);
        initialize(context);
    }

    public VerticalScrollingTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public VerticalScrollingTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    private void initialize(Context context) {
        mLinearScroller = new Scroller(context, new LinearInterpolator());
        setScroller(mLinearScroller);
        setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int viewHeight = getHeight();
        int visibleHeight = viewHeight - getPaddingBottom() - getPaddingTop();
        mLineHeight = getLineHeight();
        mStartY = visibleHeight * -1;
        Log.d("Hmm", "onSizeChanged viewHeight=" + viewHeight + " visibleHeight=" + visibleHeight + " mStartY=" + mStartY + " mLineHeight=" + mLineHeight);

        if (!mLinearScroller.isFinished() && oldh != h) {
            int diff = h - oldh;
            int oldY = mLinearScroller.getFinalY();
            int newY = oldY - diff;
            mLinearScroller.setFinalY(newY);
            Log.d("Hmm", "  diff=" + diff + " oldY=" + oldY + " newY=" + newY);
        }
        // if scrolling, subtract from distance if view taller because we now see more of the text
    }

    public void scroll() {

        // if not scrolling and is first scroll, start from -view
        // if not scrolling, startScroll from current scroll position
        // if scrolling, compute a new finalY (lineHeight*lineCount) and extend duration

        if (mLinearScroller.isFinished() && mIsFirstScroll) {
            Log.d("Hmm", "scroll mIsFirstScroll");
            mIsFirstScroll = false;
            int distanceY = (mStartY*-1) + getLineCount() * mLineHeight;
            int duration = computeScrollDuration(getLineCount());
            Log.d("Hmm", "  lines=" + getLineCount() + " distanceY=" + distanceY + " duration=" + duration);
            mLinearScroller.startScroll(0, mStartY, 0, distanceY, duration);
            Log.d("Hmm", "  getFinalY=" + mLinearScroller.getFinalY());
        } else if (mLinearScroller.isFinished()) {
            Log.d("Hmm", "scroll isFinished");
            int newLines = getLineCount() - mPreviousLineCount;
            int distance = newLines * mLineHeight;
            int duration = computeScrollDuration(newLines);
            Log.d("Hmm", "  newLines=" + newLines + " distance=" + distance + " duration" + duration);
            mLinearScroller.startScroll(0, mLinearScroller.getCurrY(), 0, distance, duration);
        } else {
            Log.d("Hmm", "scroll !isFinished");
            int finalY = getLineCount() * mLineHeight;
            int extraDuration = computeScrollDuration(getLineCount() - mPreviousLineCount);
            Log.d("Hmm", "  finalY=" + finalY + " extraDuration=" + extraDuration);
            mLinearScroller.setFinalY(finalY);
            mLinearScroller.extendDuration(extraDuration);
        }

        mPreviousLineCount = getLineCount();
    }

    public void pauseScroll() {
        //mLinearScroller.pause
    }

    private void restartScroll() {
        // if stopped, call .startScroll
        // else .extendDuration & setFinalY
    }

    private int computeScrollDuration(int linesToScroll) {
        return (int) (linesToScroll / mLinesPerSecond * 1000);
    }

    public void setLinesPerSecond(float linesPerSecond) {
        mLinesPerSecond = linesPerSecond;
    }

    public float getLinesPerSecond() {
        return mLinesPerSecond;
    }
}
