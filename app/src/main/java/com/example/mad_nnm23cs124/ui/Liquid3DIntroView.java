package com.example.mad_nnm23cs124.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

/**
 * High-fidelity 3D liquid chrome floating intro view inspired by Mobbin's Corner app.
 * Renders glossy 3D chrome typography and floating ambient objects with real-time 3D parallax bobbing.
 */
public class Liquid3DIntroView extends View {

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textChromePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint orbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint orbHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint beaconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float progress = 0f; // 0 to 1 entrance progress
    private float exitProgress = 0f; // 0 to 1 exit zoom progress
    private long startTime = 0;
    private ValueAnimator floatAnimator;

    // Floating item states
    private static class FloatingItem {
        float relX, relY;
        float radius;
        float phase;
        float speed;
        float ampX, ampY;
        int color;
        String label;

        FloatingItem(float relX, float relY, float radius, float phase, float speed, float ampX, float ampY, int color, String label) {
            this.relX = relX;
            this.relY = relY;
            this.radius = radius;
            this.phase = phase;
            this.speed = speed;
            this.ampX = ampX;
            this.ampY = ampY;
            this.color = color;
            this.label = label;
        }
    }

    private final FloatingItem[] floatingItems = new FloatingItem[] {
            new FloatingItem(0.22f, 0.28f, 32f, 0.0f, 1.8f, 10f, 16f, Color.parseColor("#38BDF8"), "⚡ BLE"),
            new FloatingItem(0.78f, 0.30f, 36f, 1.4f, 1.5f, -12f, 18f, Color.parseColor("#818CF8"), "🎯 GYRO"),
            new FloatingItem(0.20f, 0.72f, 30f, 2.7f, 1.9f, 14f, -15f, Color.parseColor("#34D399"), "🖱 MOUSE"),
            new FloatingItem(0.80f, 0.70f, 34f, 4.1f, 1.6f, -10f, -18f, Color.parseColor("#F472B6"), "⌨ KEYS")
    };

    public Liquid3DIntroView(Context context) {
        super(context);
        init();
    }

    public Liquid3DIntroView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Liquid3DIntroView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        startTime = SystemClock.uptimeMillis();
        textChromePaint.setTextAlign(Paint.Align.CENTER);
        textChromePaint.setFakeBoldText(true);
        textHighlightPaint.setTextAlign(Paint.Align.CENTER);
        textHighlightPaint.setFakeBoldText(true);
        textGlowPaint.setTextAlign(Paint.Align.CENTER);
        textGlowPaint.setFakeBoldText(true);

        floatAnimator = ValueAnimator.ofFloat(0f, 1f);
        floatAnimator.setDuration(10000);
        floatAnimator.setRepeatCount(ValueAnimator.INFINITE);
        floatAnimator.setInterpolator(new LinearInterpolator());
        floatAnimator.addUpdateListener(anim -> invalidate());
        floatAnimator.start();
    }

    public void setEntranceProgress(float p) {
        this.progress = Math.max(0f, Math.min(1f, p));
        invalidate();
    }

    public void setExitProgress(float p) {
        this.exitProgress = Math.max(0f, Math.min(1f, p));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float time = (SystemClock.uptimeMillis() - startTime) / 1000f;

        // 1. Ambient Background with Soft Radial Glow
        float cx = w * 0.5f;
        float cy = h * 0.45f;
        RadialGradient bgGlow = new RadialGradient(
                cx, cy, w * 0.75f,
                new int[] { Color.parseColor("#172033"), Color.parseColor("#0C0F17"), Color.parseColor("#06070A") },
                new float[] { 0.0f, 0.55f, 1.0f },
                Shader.TileMode.CLAMP
        );
        bgPaint.setShader(bgGlow);
        canvas.drawRect(0, 0, w, h, bgPaint);

        canvas.save();

        // Exit Zoom & Fade
        if (exitProgress > 0f) {
            float scale = 1.0f + exitProgress * 0.15f;
            canvas.scale(scale, scale, cx, cy);
            canvas.saveLayerAlpha(0, 0, w, h, (int) ((1f - exitProgress) * 255));
        }

        // Entrance Spring Scale
        float enterScale = progress;
        canvas.scale(enterScale, enterScale, cx, cy);

        // 2. Draw Floating 3D Ambient Items (Corner Mobbin Parallax Style)
        for (FloatingItem item : floatingItems) {
            drawFloatingItem(canvas, item, w, h, time);
        }

        // 3. Central 3D Liquid Chrome Typography ("BLUE" & "CONTROL")
        drawLiquidTypography(canvas, cx, cy, time);

        if (exitProgress > 0f) {
            canvas.restore();
        }

        canvas.restore();
    }

    private void drawLiquidTypography(Canvas canvas, float cx, float cy, float time) {
        float titleSize = getWidth() * 0.13f;
        textChromePaint.setTextSize(titleSize);
        textHighlightPaint.setTextSize(titleSize);
        textGlowPaint.setTextSize(titleSize);

        // Subtle organic breathing float
        float bobY = (float) Math.sin(time * 2.2f) * 6f;
        float y1 = cy - titleSize * 0.15f + bobY;
        float y2 = y1 + titleSize * 1.05f;

        // Chrome Gradient for text body
        LinearGradient chromeGradient = new LinearGradient(
                cx, y1 - titleSize, cx, y2,
                new int[] {
                        Color.parseColor("#FFFFFF"),
                        Color.parseColor("#E2E8F0"),
                        Color.parseColor("#94A3B8"),
                        Color.parseColor("#38BDF8"),
                        Color.parseColor("#1E293B"),
                        Color.parseColor("#64748B"),
                        Color.parseColor("#FFFFFF")
                },
                new float[] { 0.0f, 0.18f, 0.35f, 0.52f, 0.70f, 0.88f, 1.0f },
                Shader.TileMode.CLAMP
        );

        // A. Deep 3D Ambient Drop Shadow
        textGlowPaint.setColor(Color.parseColor("#050811"));
        textGlowPaint.setShadowLayer(28f, 0f, 16f, Color.parseColor("#38BDF8"));
        canvas.drawText("BLUE", cx, y1 + 6f, textGlowPaint);
        canvas.drawText("CONTROL", cx, y2 + 6f, textGlowPaint);
        textGlowPaint.clearShadowLayer();

        // B. Liquid Chrome Body Fill
        textChromePaint.setShader(chromeGradient);
        canvas.drawText("BLUE", cx, y1, textChromePaint);
        canvas.drawText("CONTROL", cx, y2, textChromePaint);

        // C. Specular Liquid Highlights (White Top Crest)
        textHighlightPaint.setColor(Color.parseColor("#80FFFFFF"));
        textHighlightPaint.setStyle(Paint.Style.STROKE);
        textHighlightPaint.setStrokeWidth(2.5f);
        canvas.drawText("BLUE", cx, y1 - 2f, textHighlightPaint);
        canvas.drawText("CONTROL", cx, y2 - 2f, textHighlightPaint);
        textHighlightPaint.setStyle(Paint.Style.FILL);
    }

    private void drawFloatingItem(Canvas canvas, FloatingItem item, int w, int h, float time) {
        float x = w * item.relX + (float) Math.cos(time * item.speed + item.phase) * item.ampX;
        float y = h * item.relY + (float) Math.sin(time * item.speed + item.phase) * item.ampY;
        float r = item.radius;

        // Outer Glow
        orbPaint.setStyle(Paint.Style.FILL);
        orbPaint.setColor(item.color);
        orbPaint.setAlpha(35);
        canvas.drawCircle(x, y, r * 1.55f, orbPaint);

        // 3D Sphere/Pill Shading
        RadialGradient sphereGrad = new RadialGradient(
                x - r * 0.35f, y - r * 0.35f, r * 1.2f,
                new int[] { Color.parseColor("#FFFFFF"), item.color, Color.parseColor("#0F172A") },
                new float[] { 0.0f, 0.55f, 1.0f },
                Shader.TileMode.CLAMP
        );
        orbPaint.setShader(sphereGrad);
        orbPaint.setAlpha(240);
        canvas.drawCircle(x, y, r, orbPaint);
        orbPaint.setShader(null);

        // Specular White Highlight
        orbHighlightPaint.setColor(Color.WHITE);
        orbHighlightPaint.setAlpha(180);
        canvas.drawCircle(x - r * 0.35f, y - r * 0.35f, r * 0.28f, orbHighlightPaint);

        // Subtle Label Tag below item
        particlePaint.setTextAlign(Paint.Align.CENTER);
        particlePaint.setTextSize(26f);
        particlePaint.setFakeBoldText(true);
        particlePaint.setColor(Color.parseColor("#E2E8F0"));
        particlePaint.setAlpha(210);
        canvas.drawText(item.label, x, y + r + 32f, particlePaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (floatAnimator != null) {
            floatAnimator.cancel();
        }
    }
}
