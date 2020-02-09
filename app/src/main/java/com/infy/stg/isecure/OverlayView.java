package com.infy.stg.isecure;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.camera2.params.Face;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Arrays;

public class OverlayView extends View {


    private static final String TAG = OverlayView.class.getName();
    private Size size;
    private Float yScale;
    private Float xScale;

    private Face[] faces;
    private Paint paint;


    public OverlayView(Context context) {
        super(context);
        init();
    }

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(Color.GREEN);
    }


    @Override
    public void onDraw(Canvas canvas) {
//        canvas.drawLine(0, 0, getWidth(), getHeight(), paint);

        if (size != null) {
            xScale = getHeight() / (float) size.getWidth();
            yScale = getWidth() / (float) size.getHeight();
        }

        paint.setColor(Color.RED);

        if (faces != null) {
            Arrays.asList(faces).forEach(f -> {
                drawBounds(f, canvas);
            });
        }
    }

    public void drawBounds(Face face, Canvas canvas) {
//        Log.d(TAG, "DRAW BOUNDS");

        if (face == null) {
            return;
        }

        drawFaceCenter(face, canvas);
    }

    private void drawFaceCenter(Face face, Canvas canvas) {
//        Log.d(TAG, "DRAW CENTER");

        int x = face.getBounds().centerX();
        int y = face.getBounds().centerY();
        int w = face.getBounds().width();
        int h = face.getBounds().height();

        x *= xScale;
        y *= xScale;
        w *= xScale;
        h *= xScale;


        int X = getWidth() - y;
        int Y = x;
        int W = h;
        int H = w;

        if ((face.getBounds().width() * face.getBounds().height()) / (float) (size.getWidth() * size.getWidth()) > 0.15)
            paint.setColor(Color.GREEN);

//        canvas.drawCircle(X, Y, 20, paint);
        canvas.drawRect(X - W / 2, Y - H / 2, X + W / 2, Y + H / 2, paint);
    }


    public void updateSize(Size size) {
        this.size = size;
    }

    public void updateFace(Face[] faces) {
//        Log.e("FACE", "UPDATED");
        this.faces = faces;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);

//        event.x
    }

    public boolean isCapturePossible() {
        return paint.getColor() == Color.GREEN;
    }
}