package com.leo.camerroll;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by leo on 17/1/23.
 */

public class SurLoadingView extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private boolean isRunning = true;
    private Thread mDrawTread;


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
    private int progress;
    private int max;

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public SurLoadingView(Context context) {
        this(context, null);
    }

    public SurLoadingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SurLoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        mHolder = getHolder();
        mHolder.addCallback(this);
        //this.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setZOrderOnTop(true);

        getHolder().setFormat(PixelFormat.TRANSLUCENT);

        reachPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        reachPaint.setStrokeCap(Paint.Cap.ROUND);
        reachPaint.setStyle(Paint.Style.STROKE);
        unreachPaint = new Paint(reachPaint);
        reachPaint.setColor(DEFAULT_REACH_COLOR);
        unreachPaint.setColor(DEFAULT_UNREACH_COLOR);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        textPaint.setColor(DEFAULT_TEXT_COLOR);
        textPaint.setFakeBoldText(true);
        textPaint.setStyle(Paint.Style.FILL);

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
    public void surfaceCreated(SurfaceHolder holder) {
        mDrawTread = new Thread() {
            @Override
            public void run() {
                while (isRunning) {
                    synchronized (mHolder) {
                        Canvas canvas = mHolder.lockCanvas(null);
                        drawContent(canvas);
                        if ((mStartAngle+=10) == 360) {
                            mStartAngle = 0;
                        }
                        SystemClock.sleep(10);
                    }
                }
            }
        };
        mDrawTread.start();
    }

    private void drawContent(Canvas canvas) {
        try {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            //drawbackground transparent
            canvas.drawCircle(getWidth() / 2, getWidth() / 2, mRadius - mStrokeWidth, bgPaint);
            //draw reach
            drawProgressReach(canvas);
            //draw progress text
            drawProgressText(canvas);
        } catch (Exception e) {

        } finally {
            try {
                mHolder.unlockCanvasAndPost(canvas);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
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
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isRunning = false;
        if (mDrawTread != null) {
            mDrawTread = null;
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
