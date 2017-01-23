package com.leo.camerroll;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;

/**
 * Created by leo on 17/1/22.
 */

public class LoadingView extends ProgressBar {
    private int DEFAULT_RADIUS = dp2px(15);
    private int DEFAULT_REACH_COLOR = 0XFFFFFFFF;
    private int DEFAULT_UNREACH_COLOR = 0X88000000;
    private int DEFAULT_TEXT_COLOR = 0XFFFFFFFF;
    private long ANIM_DURATION = 1000;
    private String BASE_TEXT = "00%";
    private boolean isStop;

    private int mRadius = DEFAULT_RADIUS;
    private int mStrokeWidth;
    private Paint reachPaint;
    private Paint unreachPaint;
    private Paint textPaint;
    private Paint bgPaint;

    private int mStartAngle = 0;
    private float mSweepAngle = 360 * 0.382f;

    private ValueAnimator anim;

    public LoadingView(Context context) {
        this(context, null);
    }

    public LoadingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LoadingView, defStyleAttr, 0);
        mStartAngle = (int) (360 * a.getFloat(R.styleable.LoadingView_load_range, 0.382f));
        DEFAULT_REACH_COLOR = a.getColor(R.styleable.LoadingView_reached_color, DEFAULT_REACH_COLOR);
        DEFAULT_UNREACH_COLOR = a.getColor(R.styleable.LoadingView_unreadch_color, DEFAULT_UNREACH_COLOR);
        DEFAULT_TEXT_COLOR = a.getColor(R.styleable.LoadingView_text_color, DEFAULT_TEXT_COLOR);
        a.recycle();
        initView();
    }

    private void initView() {
        reachPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        reachPaint.setStrokeCap(Paint.Cap.ROUND);
        reachPaint.setStyle(Paint.Style.STROKE);
        unreachPaint = new Paint(reachPaint);
        reachPaint.setColor(DEFAULT_REACH_COLOR);
        unreachPaint.setColor(DEFAULT_UNREACH_COLOR);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        textPaint.setStyle(Paint.Style.STROKE);
        textPaint.setColor(DEFAULT_TEXT_COLOR);
        textPaint.setFakeBoldText(true);

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        bgPaint.setStrokeCap(Paint.Cap.ROUND);
        bgPaint.setColor(Color.argb(44, 0, 0, 0));
        setMax(100);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int defWidth = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int defHeight = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        int expectSize = Math.min(defHeight, defWidth);
        if (expectSize <= 0) {
            expectSize = mRadius * 2;
        } else {
            mRadius = expectSize / 2;
        }
        mStrokeWidth = mRadius / 5;
        reachPaint.setStrokeWidth(mStrokeWidth);
        unreachPaint.setStrokeWidth(mStrokeWidth);

        setMeasuredDimension(expectSize, expectSize);

        float textSize = 0;
        while (true) {
            textSize += 0.1;
            textPaint.setTextSize(textSize);
            if (textPaint.measureText(BASE_TEXT, 0, BASE_TEXT.length()) >= mRadius) {
                break;
            }
        }
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        if (isStop) {
            setVisibility(View.GONE);
            return;
        }
        //drawbackground transparent
        canvas.drawCircle(getWidth() / 2, getWidth() / 2, mRadius - mStrokeWidth, bgPaint);
        //draw reach
        drawProgressReach(canvas);
        //draw progress text
        drawProgressText(canvas);
    }

    private void drawProgressText(Canvas canvas) {
        String text = String.valueOf((int) (getProgress() * 1.0f / getMax() * 100)) + "%";
        int centerX = getWidth() / 2;
        int centerY = getWidth() / 2;
        int baseX = (int) (centerX - textPaint.measureText(text, 0, text.length()) / 2);
        int baseY = (int) (centerY - (textPaint.getFontMetrics().ascent + textPaint.getFontMetrics().descent) / 2);
        canvas.drawText(text, baseX, baseY, textPaint);
    }

    private void drawProgressReach(Canvas canvas) {
        canvas.drawArc(new RectF(0 + mStrokeWidth / 2, 0 + mStrokeWidth / 2, mRadius * 2 - mStrokeWidth / 2, mRadius * 2 - mStrokeWidth / 2), mStartAngle, mSweepAngle, false, reachPaint);
        //drawonreach
        canvas.drawArc(new RectF(0 + mStrokeWidth / 2, 0 + mStrokeWidth / 2, mRadius * 2 - mStrokeWidth / 2, mRadius * 2 - mStrokeWidth / 2), mStartAngle + mSweepAngle, 360 - mSweepAngle, false, unreachPaint);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (anim == null) {
            anim = ValueAnimator.ofInt(0, 360);
            anim.setInterpolator(new LinearInterpolator());
            anim.setDuration(ANIM_DURATION);
            anim.setRepeatCount(Animation.INFINITE);
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (animation != null && animation.getAnimatedValue() != null) {
                        int startAngle = (int) animation.getAnimatedValue();
                        mStartAngle = startAngle;
                        postInvalidate();
                    }
                }
            });
        } else {
            anim.cancel();
            anim.removeAllUpdateListeners();
        }
        anim.start();
    }

    public void loadCompleted() {
        isStop = true;
        if (anim != null) {
            anim.cancel();
            anim.removeAllUpdateListeners();
            this.setVisibility(View.GONE);
        }
    }

    /**
     * @param size
     * @return px
     */
    private int dp2px(int size) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, getContext().getResources().getDisplayMetrics());
    }
}
