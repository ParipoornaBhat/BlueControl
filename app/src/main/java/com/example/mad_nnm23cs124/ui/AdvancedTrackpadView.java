package com.example.mad_nnm23cs124.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.mad_nnm23cs124.utils.ThemeManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.P)
public class AdvancedTrackpadView extends View {

    // Swipe direction constants for multi-finger gestures
    public static final int SWIPE_DIRECTION_LEFT = 1;
    public static final int SWIPE_DIRECTION_RIGHT = 2;
    public static final int SWIPE_DIRECTION_UP = 3;
    public static final int SWIPE_DIRECTION_DOWN = 4;

    // Haptic feedback types
    public static final int HAPTIC_TAP = 1;
    public static final int HAPTIC_CLICK = 2;
    public static final int HAPTIC_HEAVY_CLICK = 3;
    public static final int HAPTIC_SWIPE = 4;
    public static final int HAPTIC_TICK = 5;

    // Gesture constants
    private static final long TAP_TIMEOUT_MS = 250;
    private static final long TRAIL_LIFETIME_MS = 280;
    private static final float SMOOTHING_FACTOR = 0.65f;
    private static final float INERTIA_FRICTION = 0.92f;
    private static final int MAX_POOL_SIZE = 120;

    // Visual Paints
    private final Paint trailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dragHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF boundsRect = new RectF();

    // Touch & Trail Pool (zero allocations in hot loops)
    private final TouchPoint[] pointPool = new TouchPoint[MAX_POOL_SIZE];
    private final List<TouchPoint> activeTrails = new ArrayList<>(MAX_POOL_SIZE);
    private int poolIndex = 0;

    // Tap Ripple
    private final RippleEffect tapRipple = new RippleEffect();

    // Active pointer tracking
    private final float[] activePointerX = new float[10];
    private final float[] activePointerY = new float[10];
    private final int[] activePointerIds = new int[10];
    private int activePointerCount = 0;

    // Gesture detection state
    private float touchDownX, touchDownY;
    private long touchDownTime = 0;
    private int maxPointersInGesture = 0;
    private boolean movedBeyondSlop = false;
    private float touchSlop;
    private float swipeThreshold;
    private float scrollStepPx;

    // Tap-and-Hold Drag (Double-Tap to Drag / Resize window)
    private static final long DOUBLE_TAP_TIMEOUT_MS = 340;
    private long lastTapUpTime = 0;
    private float lastTapUpX = 0f, lastTapUpY = 0f;
    private boolean isDragMode = false;
    private boolean isDoubleTapPending = false;
    private float doubleTapSlop;

    // 1-Finger cursor movement & acceleration
    private float lastX, lastY;
    private long lastMoveTime = 0;
    private float smoothedDx = 0f, smoothedDy = 0f;
    private float accumulatedX = 0f, accumulatedY = 0f;
    private float sensitivity = 1.0f;

    // 2-Finger scroll state
    private float lastScrollMidX, lastScrollMidY;
    private float accumulatedScrollY = 0f, accumulatedScrollX = 0f;
    private boolean invertScroll = true;

    // 3-Finger swipe state
    private float threeFingerStartX, threeFingerStartY;
    private boolean threeFingerSwipeConsumed = false;

    // Inertia physics
    private VelocityTracker velocityTracker;
    private float inertiaVx = 0f, inertiaVy = 0f;
    private boolean isInertiaRunning = false;
    private long lastInertiaTime = 0;
    private float minFlingVelocity;
    private float maxFlingVelocity;

    // Haptics & Configuration
    private boolean hapticEnabled = true;
    private TrackpadEventListener listener;

    public interface TrackpadEventListener {
        void onMouseMove(byte dx, byte dy);
        default void onMouseDragStart() {}
        default void onMouseDrag(byte dx, byte dy) {
            onMouseMove(dx, dy);
        }
        default void onMouseDragEnd() {}
        void onMouseScroll(byte wheel);
        default void onMouseScroll(byte vWheel, byte hWheel) {
            onMouseScroll(vWheel);
        }
        void onLeftClick();
        void onRightClick();
        void onThreeFingerSwipe();
        default void onThreeFingerSwipe(int direction) {
            onThreeFingerSwipe();
        }
    }

    public AdvancedTrackpadView(Context context) {
        super(context);
        init(context);
    }

    public AdvancedTrackpadView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AdvancedTrackpadView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        ViewConfiguration vc = ViewConfiguration.get(context);
        touchSlop = vc.getScaledTouchSlop();
        minFlingVelocity = vc.getScaledMinimumFlingVelocity() * 0.7f;
        maxFlingVelocity = vc.getScaledMaximumFlingVelocity();

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        swipeThreshold = 70f * dm.density;
        scrollStepPx = 14f * dm.density;
        doubleTapSlop = touchSlop * 2.5f;

        // Pre-allocate point pool
        for (int i = 0; i < MAX_POOL_SIZE; i++) {
            pointPool[i] = new TouchPoint();
        }

        // Initialize paints
        int glowColor = ThemeManager.getTrackpadGlowColor(context);

        trailPaint.setColor(glowColor);
        trailPaint.setStyle(Paint.Style.FILL);

        dotPaint.setColor(glowColor);
        dotPaint.setStyle(Paint.Style.FILL);

        haloPaint.setColor(glowColor);
        haloPaint.setStyle(Paint.Style.FILL);

        borderPaint.setColor(ThemeManager.getTrackpadGridColor(context));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.5f * dm.density);

        ripplePaint.setColor(glowColor);
        ripplePaint.setStyle(Paint.Style.STROKE);
        ripplePaint.setStrokeWidth(2.5f * dm.density);

        dragHaloPaint.setColor(glowColor);
        dragHaloPaint.setStyle(Paint.Style.STROKE);
        dragHaloPaint.setStrokeWidth(2.5f * dm.density);
    }

    public void setTrackpadEventListener(TrackpadEventListener listener) {
        this.listener = listener;
    }

    public void setSensitivity(float sensitivity) {
        this.sensitivity = Math.max(0.2f, Math.min(3.0f, sensitivity));
    }

    public void setInvertScroll(boolean invert) {
        this.invertScroll = invert;
    }

    public boolean isInvertScroll() {
        return invertScroll;
    }

    public void setHapticEnabled(boolean enabled) {
        this.hapticEnabled = enabled;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float inset = borderPaint.getStrokeWidth() / 2f + 4f;
        boundsRect.set(inset, inset, w - inset, h - inset);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        long currentTime = SystemClock.uptimeMillis();
        boolean needsInvalidate = false;

        // 1. Subtle perimeter boundary
        canvas.drawRoundRect(boundsRect, 16f, 16f, borderPaint);

        // 2. Fading touch trails
        Iterator<TouchPoint> iterator = activeTrails.iterator();
        while (iterator.hasNext()) {
            TouchPoint p = iterator.next();
            long age = currentTime - p.timestamp;
            if (age > TRAIL_LIFETIME_MS) {
                p.active = false;
                iterator.remove();
            } else {
                float progress = 1.0f - ((float) age / (float) TRAIL_LIFETIME_MS);
                trailPaint.setAlpha((int) (progress * 90));
                float radius = (12f * progress + 3f);
                canvas.drawCircle(p.x, p.y, radius, trailPaint);
                needsInvalidate = true;
            }
        }

        // 3. Active touch points (subtle glowing halo + core dot)
        for (int i = 0; i < activePointerCount; i++) {
            float px = activePointerX[i];
            float py = activePointerY[i];

            if (isDragMode && i == 0) {
                // Outer accented drag latch ring
                dragHaloPaint.setAlpha(200);
                canvas.drawCircle(px, py, 34f, dragHaloPaint);
            }

            // Outer soft glow halo
            haloPaint.setAlpha(isDragMode ? 85 : 45);
            canvas.drawCircle(px, py, 26f, haloPaint);

            // Inner core dot
            dotPaint.setAlpha(220);
            canvas.drawCircle(px, py, 9f, dotPaint);

            needsInvalidate = true;
        }

        // 4. Expanding tap ripple
        if (tapRipple.active) {
            long age = currentTime - tapRipple.startTime;
            if (age > 240) {
                tapRipple.active = false;
            } else {
                float progress = (float) age / 240f;
                float radius = tapRipple.maxRadius * progress;
                ripplePaint.setAlpha((int) ((1.0f - progress) * 160));
                canvas.drawCircle(tapRipple.x, tapRipple.y, radius, ripplePaint);
                needsInvalidate = true;
            }
        }

        // Only invalidate if animations or touches are actively running (zero idle CPU drain)
        if (needsInvalidate || isInertiaRunning) {
            postInvalidateOnAnimation();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerCount = event.getPointerCount();
        long eventTime = SystemClock.uptimeMillis();

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);

        // Track max pointers involved in this gesture
        if (pointerCount > maxPointersInGesture) {
            maxPointersInGesture = pointerCount;
        }

        // Update active pointer positions for drawing glowing points
        activePointerCount = Math.min(pointerCount, activePointerX.length);
        for (int i = 0; i < activePointerCount; i++) {
            activePointerX[i] = event.getX(i);
            activePointerY[i] = event.getY(i);
            activePointerIds[i] = event.getPointerId(i);
            addTrailPoint(activePointerX[i], activePointerY[i], activePointerIds[i], eventTime);
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                stopInertia();
                touchDownX = event.getX();
                touchDownY = event.getY();
                touchDownTime = eventTime;
                lastX = touchDownX;
                lastY = touchDownY;
                lastMoveTime = eventTime;
                maxPointersInGesture = 1;
                movedBeyondSlop = false;
                threeFingerSwipeConsumed = false;
                accumulatedX = 0f;
                accumulatedY = 0f;
                smoothedDx = 0f;
                smoothedDy = 0f;
                lastScrollMidX = 0f;
                lastScrollMidY = 0f;

                // Check if this tap down is part of a double-tap-to-drag sequence
                long timeSinceLastTap = eventTime - lastTapUpTime;
                float distFromLastTap = (float) Math.hypot(touchDownX - lastTapUpX, touchDownY - lastTapUpY);
                if (timeSinceLastTap < DOUBLE_TAP_TIMEOUT_MS && distFromLastTap < doubleTapSlop) {
                    isDoubleTapPending = true;
                    isDragMode = false;
                } else {
                    isDoubleTapPending = false;
                    if (isDragMode) {
                        isDragMode = false;
                        if (listener != null) listener.onMouseDragEnd();
                    }
                }

                invalidate();
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                stopInertia();
                if (isDragMode) {
                    isDragMode = false;
                    isDoubleTapPending = false;
                    if (listener != null) listener.onMouseDragEnd();
                }
                if (pointerCount == 2) {
                    lastScrollMidX = (event.getX(0) + event.getX(1)) / 2f;
                    lastScrollMidY = (event.getY(0) + event.getY(1)) / 2f;
                    accumulatedScrollX = 0f;
                    accumulatedScrollY = 0f;
                } else if (pointerCount >= 3) {
                    threeFingerStartX = (event.getX(0) + event.getX(1) + event.getX(2)) / 3f;
                    threeFingerStartY = (event.getY(0) + event.getY(1) + event.getY(2)) / 3f;
                    threeFingerSwipeConsumed = false;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                float primaryX = event.getX();
                float primaryY = event.getY();

                // Check touch slop
                if (!movedBeyondSlop) {
                    float totalDisplacement = (float) Math.hypot(primaryX - touchDownX, primaryY - touchDownY);
                    if (totalDisplacement > touchSlop) {
                        movedBeyondSlop = true;
                    }
                }

                if (pointerCount == 1) {
                    // If double-tap is pending, latch into DRAG mode upon movement or hold
                    if (isDoubleTapPending && !isDragMode) {
                        float dragDisplacement = (float) Math.hypot(primaryX - touchDownX, primaryY - touchDownY);
                        if (dragDisplacement > (touchSlop * 0.35f) || (eventTime - touchDownTime > 120)) {
                            isDragMode = true;
                            performHaptic(HAPTIC_HEAVY_CLICK);
                            if (listener != null) {
                                listener.onMouseDragStart();
                            }
                        }
                    }

                    // Single finger move -> Smooth accelerated cursor motion
                    processCursorMovement(primaryX, primaryY, eventTime);
                    lastX = primaryX;
                    lastY = primaryY;
                } else if (pointerCount == 2) {
                    if (isDragMode) {
                        isDragMode = false;
                        isDoubleTapPending = false;
                        if (listener != null) listener.onMouseDragEnd();
                    }
                    // Two finger move -> Fluid vertical & horizontal scrolling
                    float midX = (event.getX(0) + event.getX(1)) / 2f;
                    float midY = (event.getY(0) + event.getY(1)) / 2f;
                    if (lastScrollMidX != 0f || lastScrollMidY != 0f) {
                        processScrollMovement(midX - lastScrollMidX, midY - lastScrollMidY);
                    }
                    lastScrollMidX = midX;
                    lastScrollMidY = midY;
                } else if (pointerCount >= 3) {
                    if (isDragMode) {
                        isDragMode = false;
                        isDoubleTapPending = false;
                        if (listener != null) listener.onMouseDragEnd();
                    }
                    // Three finger swipe -> Win+Tab / App Switcher
                    float midX = (event.getX(0) + event.getX(1) + event.getX(2)) / 3f;
                    float midY = (event.getY(0) + event.getY(1) + event.getY(2)) / 3f;
                    processThreeFingerSwipe(midX, midY);
                }
                invalidate();
                break;

            case MotionEvent.ACTION_POINTER_UP:
                int upIndex = event.getActionIndex();
                // If secondary finger lifted in 2-finger scroll, reset scroll baseline
                if (pointerCount - 1 == 1) {
                    int remainingIndex = (upIndex == 0) ? 1 : 0;
                    lastX = event.getX(remainingIndex);
                    lastY = event.getY(remainingIndex);
                    lastMoveTime = eventTime;
                    smoothedDx = 0f;
                    smoothedDy = 0f;
                }
                break;

            case MotionEvent.ACTION_UP:
                long gestureDuration = eventTime - touchDownTime;

                if (isDragMode) {
                    // Finished drag & pull -> release latch!
                    isDragMode = false;
                    isDoubleTapPending = false;
                    lastTapUpTime = 0;
                    performHaptic(HAPTIC_TAP);
                    if (listener != null) {
                        listener.onMouseDragEnd();
                    }
                } else if (isDoubleTapPending && !movedBeyondSlop) {
                    // Double tap without pulling -> Standard Double Click
                    isDoubleTapPending = false;
                    lastTapUpTime = 0;
                    performHaptic(HAPTIC_CLICK);
                    spawnTapRipple(event.getX(), event.getY());
                    if (listener != null) {
                        listener.onLeftClick();
                    }
                } else if (!movedBeyondSlop && gestureDuration < TAP_TIMEOUT_MS) {
                    // 1st Tap detection
                    if (maxPointersInGesture == 1) {
                        lastTapUpTime = eventTime;
                        lastTapUpX = event.getX();
                        lastTapUpY = event.getY();
                        performHaptic(HAPTIC_CLICK);
                        spawnTapRipple(event.getX(), event.getY());
                        if (listener != null) {
                            listener.onLeftClick();
                        }
                    } else if (maxPointersInGesture == 2) {
                        lastTapUpTime = 0;
                        performHaptic(HAPTIC_HEAVY_CLICK);
                        spawnTapRipple(event.getX(), event.getY());
                        if (listener != null) {
                            listener.onRightClick();
                        }
                    }
                } else if (maxPointersInGesture == 1 && movedBeyondSlop) {
                    // 1-Finger Fling -> Fluid Inertia
                    lastTapUpTime = 0;
                    velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity);
                    float vx = velocityTracker.getXVelocity();
                    float vy = velocityTracker.getYVelocity();
                    startInertia(vx, vy);
                } else {
                    lastTapUpTime = 0;
                }

                resetGestureState();
                performClick();
                invalidate();
                break;

            case MotionEvent.ACTION_CANCEL:
                if (isDragMode) {
                    isDragMode = false;
                    if (listener != null) listener.onMouseDragEnd();
                }
                isDoubleTapPending = false;
                lastTapUpTime = 0;
                resetGestureState();
                stopInertia();
                invalidate();
                break;
        }

        return true;
    }

    private void processCursorMovement(float currentX, float currentY, long eventTime) {
        float rawDx = currentX - lastX;
        float rawDy = currentY - lastY;

        long dt = eventTime - lastMoveTime;
        if (dt <= 0) dt = 16;
        if (dt > 80) dt = 80;
        lastMoveTime = eventTime;

        // Exponential Moving Average (EMA) to smooth digitizer jitter
        smoothedDx = SMOOTHING_FACTOR * rawDx + (1.0f - SMOOTHING_FACTOR) * smoothedDx;
        smoothedDy = SMOOTHING_FACTOR * rawDy + (1.0f - SMOOTHING_FACTOR) * smoothedDy;

        // Calculate velocity (pixels / ms)
        float dist = (float) Math.hypot(smoothedDx, smoothedDy);
        float speed = dist / (float) dt;

        // Precision non-linear acceleration curve
        float accelMultiplier;
        if (speed < 0.25f) {
            accelMultiplier = 0.85f; // Precision mode for micro-targeting
        } else if (speed < 1.2f) {
            accelMultiplier = 0.85f + (speed - 0.25f) * 0.9f; // Linear ramp
        } else {
            accelMultiplier = Math.min(3.2f, 1.7f + (speed - 1.2f) * 1.0f); // High-speed glide
        }

        float accDx = smoothedDx * accelMultiplier * sensitivity;
        float accDy = smoothedDy * accelMultiplier * sensitivity;

        // Sub-pixel accumulator preserves sub-pixel movement without stepping
        accumulatedX += accDx;
        accumulatedY += accDy;

        int sendX = (int) accumulatedX;
        int sendY = (int) accumulatedY;

        accumulatedX -= sendX;
        accumulatedY -= sendY;

        // Clamp to signed byte
        sendX = Math.max(-127, Math.min(127, sendX));
        sendY = Math.max(-127, Math.min(127, sendY));

        if ((sendX != 0 || sendY != 0) && listener != null) {
            if (isDragMode) {
                listener.onMouseDrag((byte) sendX, (byte) sendY);
            } else {
                listener.onMouseMove((byte) sendX, (byte) sendY);
            }
        }
    }

    private void processScrollMovement(float deltaX, float deltaY) {
        accumulatedScrollY += deltaY;
        accumulatedScrollX += deltaX;

        byte vWheel = 0;
        byte hWheel = 0;

        if (Math.abs(accumulatedScrollY) >= scrollStepPx) {
            int stepsY = (int) (accumulatedScrollY / scrollStepPx);
            accumulatedScrollY -= stepsY * scrollStepPx;
            // Configurable scroll direction: invertScroll = true gives natural touch scrolling
            vWheel = (byte) (invertScroll ? stepsY : -stepsY);
        }

        if (Math.abs(accumulatedScrollX) >= scrollStepPx) {
            int stepsX = (int) (accumulatedScrollX / scrollStepPx);
            accumulatedScrollX -= stepsX * scrollStepPx;
            hWheel = (byte) stepsX;
        }

        if ((vWheel != 0 || hWheel != 0) && listener != null) {
            performHaptic(HAPTIC_TICK);
            listener.onMouseScroll(vWheel, hWheel);
        }
    }

    private void processThreeFingerSwipe(float midX, float midY) {
        if (threeFingerSwipeConsumed) return;

        float diffX = midX - threeFingerStartX;
        float diffY = midY - threeFingerStartY;

        if (Math.abs(diffX) > swipeThreshold && Math.abs(diffX) > Math.abs(diffY) * 1.25f) {
            threeFingerSwipeConsumed = true;
            int direction = (diffX > 0) ? SWIPE_DIRECTION_RIGHT : SWIPE_DIRECTION_LEFT;
            performHaptic(HAPTIC_SWIPE);
            if (listener != null) {
                listener.onThreeFingerSwipe(direction);
            }
        } else if (Math.abs(diffY) > swipeThreshold && Math.abs(diffY) > Math.abs(diffX) * 1.25f) {
            threeFingerSwipeConsumed = true;
            int direction = (diffY > 0) ? SWIPE_DIRECTION_DOWN : SWIPE_DIRECTION_UP;
            performHaptic(HAPTIC_SWIPE);
            if (listener != null) {
                listener.onThreeFingerSwipe(direction);
            }
        }
    }

    private void startInertia(float vx, float vy) {
        stopInertia();
        // Convert px/sec to px/frame (~16ms)
        inertiaVx = vx * 0.016f;
        inertiaVy = vy * 0.016f;

        float speed = (float) Math.hypot(inertiaVx, inertiaVy);
        if (speed < 1.2f) return;

        isInertiaRunning = true;
        lastInertiaTime = SystemClock.uptimeMillis();
        postOnAnimation(inertiaRunnable);
    }

    private void stopInertia() {
        if (isInertiaRunning) {
            isInertiaRunning = false;
            removeCallbacks(inertiaRunnable);
        }
    }

    private final Runnable inertiaRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isInertiaRunning) return;

            long now = SystemClock.uptimeMillis();
            long dt = now - lastInertiaTime;
            if (dt <= 0) dt = 16;
            lastInertiaTime = now;

            inertiaVx *= INERTIA_FRICTION;
            inertiaVy *= INERTIA_FRICTION;

            accumulatedX += inertiaVx;
            accumulatedY += inertiaVy;

            int sendX = (int) accumulatedX;
            int sendY = (int) accumulatedY;

            accumulatedX -= sendX;
            accumulatedY -= sendY;

            sendX = Math.max(-127, Math.min(127, sendX));
            sendY = Math.max(-127, Math.min(127, sendY));

            if ((sendX != 0 || sendY != 0) && listener != null) {
                listener.onMouseMove((byte) sendX, (byte) sendY);
            }

            if (Math.hypot(inertiaVx, inertiaVy) > 0.35f) {
                postOnAnimation(this);
            } else {
                stopInertia();
            }
        }
    };

    private void spawnTapRipple(float x, float y) {
        tapRipple.x = x;
        tapRipple.y = y;
        tapRipple.maxRadius = 38f;
        tapRipple.startTime = SystemClock.uptimeMillis();
        tapRipple.active = true;
        invalidate();
    }

    private void addTrailPoint(float x, float y, int pointerId, long timestamp) {
        TouchPoint point = pointPool[poolIndex];
        poolIndex = (poolIndex + 1) % MAX_POOL_SIZE;

        point.x = x;
        point.y = y;
        point.pointerId = pointerId;
        point.timestamp = timestamp;
        point.active = true;

        if (!activeTrails.contains(point)) {
            activeTrails.add(point);
        }
    }

    private void resetGestureState() {
        activePointerCount = 0;
        maxPointersInGesture = 0;
        lastScrollMidX = 0f;
        lastScrollMidY = 0f;
        threeFingerStartX = 0f;
        threeFingerStartY = 0f;
        threeFingerSwipeConsumed = false;
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    private void performHaptic(int type) {
        if (!hapticEnabled) return;
        try {
            switch (type) {
                case HAPTIC_TICK:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    }
                    break;
                case HAPTIC_TAP:
                case HAPTIC_CLICK:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    } else {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    }
                    break;
                case HAPTIC_HEAVY_CLICK:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                    } else {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    }
                    break;
                case HAPTIC_SWIPE:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                    } else {
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    }
                    break;
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopInertia();
        resetGestureState();
        activeTrails.clear();
    }

    private static class TouchPoint {
        float x, y;
        int pointerId;
        long timestamp;
        boolean active;
    }

    private static class RippleEffect {
        float x, y;
        float maxRadius;
        long startTime;
        boolean active;
    }
}
