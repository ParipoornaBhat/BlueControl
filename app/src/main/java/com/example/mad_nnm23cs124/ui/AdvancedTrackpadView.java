package com.example.mad_nnm23cs124.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.P)
public class AdvancedTrackpadView extends View {

    private final List<TouchPoint> touchPoints = new ArrayList<>();
    private final Paint trailPaint = new Paint();

    private float lastX, lastY;
    private long lastTouchTime = 0;
    private int maxPointersInGesture = 0;
    private TrackpadEventListener listener;

    public interface TrackpadEventListener {
        void onMouseMove(byte dx, byte dy);
        void onMouseScroll(byte wheel);
        void onLeftClick();
        void onRightClick();
        void onThreeFingerSwipe();
    }

    public AdvancedTrackpadView(Context context) {
        super(context);
        init();
    }

    public AdvancedTrackpadView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AdvancedTrackpadView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        trailPaint.setColor(0x6638BDF8); // Light glowing blue tail
        trailPaint.setStyle(Paint.Style.FILL);
        trailPaint.setAntiAlias(true);
    }

    public void setTrackpadEventListener(TrackpadEventListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        long currentTime = System.currentTimeMillis();
        Iterator<TouchPoint> iterator = touchPoints.iterator();
        while (iterator.hasNext()) {
            TouchPoint p = iterator.next();
            long age = currentTime - p.timestamp;
            if (age > 300) {
                iterator.remove();
            } else {
                float alpha = 1.0f - (age / 300f);
                trailPaint.setAlpha((int) (alpha * 120));
                float radius = 15f * alpha;
                canvas.drawCircle(p.x, p.y, radius, trailPaint);
            }
        }

        if (!touchPoints.isEmpty()) {
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        if (pointerCount > maxPointersInGesture) {
            maxPointersInGesture = pointerCount;
        }

        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = x;
                lastY = y;
                lastTouchTime = System.currentTimeMillis();
                maxPointersInGesture = 1;
                touchPoints.add(new TouchPoint(x, y, System.currentTimeMillis()));
                invalidate();
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                lastX = x;
                lastY = y;
                break;

            case MotionEvent.ACTION_MOVE:
                touchPoints.add(new TouchPoint(x, y, System.currentTimeMillis()));
                invalidate();

                if (pointerCount == 1) {
                    // Single finger move -> Cursor motion
                    byte dx = (byte) (x - lastX);
                    byte dy = (byte) (y - lastY);
                    if ((dx != 0 || dy != 0) && listener != null) {
                        listener.onMouseMove(dx, dy);
                    }
                } else if (pointerCount == 2) {
                    // Two-finger slide -> Scroll wheel
                    byte dy = (byte) (y - lastY);
                    if (dy != 0 && listener != null) {
                        listener.onMouseScroll((byte) (-dy / 4));
                    }
                }
                lastX = x;
                lastY = y;
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                long duration = System.currentTimeMillis() - lastTouchTime;
                if (duration < 250) {
                    if (maxPointersInGesture == 1 && listener != null) {
                        // Single finger tap -> Left Click
                        listener.onLeftClick();
                    } else if (maxPointersInGesture == 2 && listener != null) {
                        // Two finger tap -> Right Click (Laptop touchpad gesture!)
                        listener.onRightClick();
                    } else if (maxPointersInGesture >= 3 && listener != null) {
                        // Three finger tap/swipe -> App Switcher shortcut
                        listener.onThreeFingerSwipe();
                    }
                }
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    maxPointersInGesture = 0;
                }
                performClick();
                break;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private static class TouchPoint {
        float x, y;
        long timestamp;

        TouchPoint(float x, float y, long timestamp) {
            this.x = x;
            this.y = y;
            this.timestamp = timestamp;
        }
    }
}
