package com.example.mad_nnm23cs124;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mad_nnm23cs124.ui.Liquid3DIntroView;
import com.example.mad_nnm23cs124.utils.ThemeManager;

public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isNavigatingAway = false;
    private Liquid3DIntroView liquidIntroView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        View splashRoot = findViewById(R.id.splashRoot);
        liquidIntroView = findViewById(R.id.liquidIntroView);
        View bottomBrandContainer = findViewById(R.id.bottomBrandContainer);
        View dotPulse = findViewById(R.id.dotPulse);

        if (bottomBrandContainer != null) {
            bottomBrandContainer.setTranslationY(40f);
            bottomBrandContainer.setAlpha(0f);
        }

        // 1. Entrance spring scale-in for 3D liquid elements
        ValueAnimator enterAnim = ValueAnimator.ofFloat(0.2f, 1.0f);
        enterAnim.setDuration(850);
        enterAnim.setInterpolator(new OvershootInterpolator(1.35f));
        enterAnim.addUpdateListener(anim -> {
            if (liquidIntroView != null) {
                liquidIntroView.setEntranceProgress((float) anim.getAnimatedValue());
            }
        });
        enterAnim.start();

        // 2. Staggered fade-in for brand tagline
        if (bottomBrandContainer != null) {
            bottomBrandContainer.animate()
                    .translationY(0f)
                    .alpha(1.0f)
                    .setStartDelay(450)
                    .setDuration(700)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // 3. Status pulsing dot
        if (dotPulse != null) {
            ObjectAnimator pulseAnim = ObjectAnimator.ofFloat(dotPulse, "alpha", 0.25f, 1.0f);
            pulseAnim.setDuration(500);
            pulseAnim.setRepeatCount(ValueAnimator.INFINITE);
            pulseAnim.setRepeatMode(ValueAnimator.REVERSE);
            pulseAnim.start();
        }

        // Tap anywhere to skip instantly
        if (splashRoot != null) {
            splashRoot.setOnClickListener(v -> navigateToMain());
        }

        // Smooth transition at 2.4s
        handler.postDelayed(this::navigateToMain, 2400);
    }

    private void navigateToMain() {
        if (isNavigatingAway) return;
        isNavigatingAway = true;
        handler.removeCallbacksAndMessages(null);

        ValueAnimator exitAnim = ValueAnimator.ofFloat(0f, 1f);
        exitAnim.setDuration(350);
        exitAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        exitAnim.addUpdateListener(anim -> {
            if (liquidIntroView != null) {
                liquidIntroView.setExitProgress((float) anim.getAnimatedValue());
            }
        });
        exitAnim.start();

        View bottomBrandContainer = findViewById(R.id.bottomBrandContainer);
        if (bottomBrandContainer != null) {
            bottomBrandContainer.animate().alpha(0f).setDuration(250).start();
        }

        handler.postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 320);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
