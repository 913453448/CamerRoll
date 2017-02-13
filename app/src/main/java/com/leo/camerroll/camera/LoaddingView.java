package com.leo.camerroll.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Created by leo on 17/2/10.
 */

public class LoaddingView extends View {
    private static final float EYE_PERCENT_W = 0.35F;
    private static final float EYE_PERCENT_H = 0.38F;
    private static final float MOUCH_PERCENT_H = 0.55F;
    private static final float MOUCH_PERCENT_H2 = 0.7F;
    private static final float MOUCH_PERCENT_W = 0.23F;
    private static final float DURATION_AREA = 0.15F;
    Animation a=new Animation() {
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float offset=interpolatedTime*DURATION_AREA;
            mMouchH=MOUCH_PERCENT_H+offset;
            mMouchH2=MOUCH_PERCENT_H2+offset;
            mEyesH=EYE_PERCENT_H+offset;
            postInvalidate();
        }
    };
    private Paint reachedPaint;
    private Paint unreachedPaint;
    private Path reachedPath;
    private Path unreachedPath;
    private Path mouthPath=new Path();

    private float mProgress=0.1f;
    private float lineWidth=dp2px(2);

    private float mRadius;
    private float mMouchH=MOUCH_PERCENT_H;
    private float mMouchH2=MOUCH_PERCENT_H2;
    private float mEyesH=EYE_PERCENT_H;
    public LoaddingView(Context context) {
        this(context, null);
    }

    public LoaddingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoaddingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void startAni() {
        a.setDuration(500);
        a.setRepeatCount(Animation.INFINITE);
        a.setRepeatMode(Animation.REVERSE);
        startAnimation(a);
    }

    private void initView() {
        reachedPaint=new Paint(Paint.ANTI_ALIAS_FLAG| Paint.DITHER_FLAG);
        reachedPaint.setStyle(Paint.Style.STROKE);
        reachedPaint.setStrokeWidth(lineWidth);
        reachedPaint.setColor(Color.WHITE);
        reachedPaint.setStrokeJoin(Paint.Join.ROUND);
        reachedPaint.setStrokeCap(Paint.Cap.ROUND);


        unreachedPaint=new Paint(reachedPaint);
        unreachedPaint.setColor(Color.GRAY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);
        canvas.save();
        //draw face
        drawFace(canvas);
        //drawreached rect
        drawReachedRect(canvas);
        canvas.restore();
    }

    /**
     * draw face
     */
    private void drawFace(Canvas canvas) {
        unreachedPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(getWidth()*EYE_PERCENT_W,getHeight()*mEyesH-mRadius,mRadius,unreachedPaint);
        canvas.drawCircle(getWidth()*(1-EYE_PERCENT_W),getHeight()*mEyesH-mRadius,mRadius,unreachedPaint);
        mouthPath.reset();
        mouthPath.moveTo(getWidth()*MOUCH_PERCENT_W,getHeight()*mMouchH);
        mouthPath.quadTo(getWidth()/2,getHeight()*mMouchH2,getWidth()*(1-MOUCH_PERCENT_W),getHeight()*mMouchH);
        unreachedPaint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(mouthPath,unreachedPaint);
    }
    private boolean isStart=true;
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if(isStart){
            startAni();
            isStart=false;
        }
        mRadius=getWidth()/7F/2;
        if(unreachedPath==null){
            unreachedPath=new Path();
        }
        unreachedPath.addRoundRect(new RectF(lineWidth,lineWidth,w-lineWidth,h-lineWidth),w/6,w/6, Path.Direction.CCW);
        if(reachedPath==null){
            reachedPath=new Path();
        }
        reachedPath.addRoundRect(new RectF(lineWidth,lineWidth,w-lineWidth,h-lineWidth),w/6,w/6, Path.Direction.CW);
    }

    private void drawReachedRect(Canvas canvas) {
        unreachedPaint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(unreachedPath,unreachedPaint);
        PathMeasure measure=new PathMeasure(reachedPath,false);
        float length = measure.getLength();
        float currLength=length*mProgress;
        Path path=new Path();
        measure.getSegment(length*1/3f,currLength+length*1/3f,path,true);
        canvas.drawPath(path,reachedPaint);

        if(mProgress>=2/3f){
            Path path2=new Path();
            measure.getSegment(0,length*(mProgress-2/3f),path2,true);
            canvas.drawPath(path2,reachedPaint);
        }

    }

    public float dp2px(float dpValue){
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,dpValue,getResources().getDisplayMetrics());
    }
    public void setProgress(float progress){
        if(progress<mProgress){
            return;
        }
        this.mProgress=progress;
        if(Looper.myLooper()==Looper.getMainLooper()){
            invalidate();
        }else{
            postInvalidate();
        }
    }

    public void loadComplete() {
        a.cancel();
        clearAnimation();
        setVisibility(View.GONE);
    }
}
