package net.davidcie.gyroscopebandaid.controls;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.Nullable;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;
import android.widget.TextView;

@SuppressWarnings({"unused", "SameParameterValue"})
@SuppressLint("AppCompatCustomView")
public class VerticalScrollingTextView extends TextView {

    private static final float DEFAULT_LINES_PER_SECOND = 0.5f;

    private float mLinesPerSecond = DEFAULT_LINES_PER_SECOND;
    private Scroller mLinearScroller;
    private int mLineHeight = 0;
    private int mPreviousLineCount = 0;
    private int mStartY = 0;
    private boolean mIsFirstScroll = true;

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
        // Disable extra top & bottom padding that's there to make room for accents
        setIncludeFontPadding(false);
        // Disable scrollbars even if user forgot to do so
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
        // Prepare scrolling
        mLinearScroller = new Scroller(context, new LinearInterpolator());
        setScroller(mLinearScroller);
        setMovementMethod(new ScrollingMovementMethod());

        // Marquee test
        setVerticalFadingEdgeEnabled(true);
        setFadingEdgeLength(50);
        setEllipsize(null);
    }

    /**
     * Prevents a small visual glitch whereby at the very beginning when there are
     * values coming from the bottom but none are up top, a bottom fading edge
     * would be drawn and then disappear after a few seconds.
     */
    @Override
    protected float getBottomFadingEdgeStrength() {
        return 0.0f;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int viewHeight = getHeight();
        int visibleHeight = viewHeight - getPaddingBottom() - getPaddingTop();
        mLineHeight = getLineHeight();
        mStartY = visibleHeight * -1;

        // Adjust distance if view taller because we now see more of the text
        if (!mLinearScroller.isFinished() && oldh != h) {
            int diff = h - oldh;
            int oldY = mLinearScroller.getFinalY();
            int newY = oldY - diff;
            mLinearScroller.setFinalY(newY);
        }
    }

    public void scroll() {

        if (mLinearScroller.isFinished()) {
            int startPos = mIsFirstScroll ? mStartY : mLinearScroller.getCurrY();
            int newLines = getLineCount() - mPreviousLineCount;
            int distance = newLines * mLineHeight;
            int duration = computeScrollDuration(newLines);
            mLinearScroller.startScroll(0, startPos, 0, distance, duration);
            mIsFirstScroll = false;
        } else {
            int newPos = mStartY + getLineCount() * mLineHeight;
            int extraDuration = computeScrollDuration(getLineCount() - mPreviousLineCount);
            mLinearScroller.setFinalY(newPos);
            mLinearScroller.extendDuration(extraDuration);
        }

        mPreviousLineCount = getLineCount();
    }

    private int computeScrollDuration(int linesToScroll) {
        return (int) ((1/mLinesPerSecond) * linesToScroll * 1000);
    }

    public void setLinesPerSecond(float linesPerSecond) {
        mLinesPerSecond = linesPerSecond;
    }

    public float getLinesPerSecond() {
        return mLinesPerSecond;
    }
}
