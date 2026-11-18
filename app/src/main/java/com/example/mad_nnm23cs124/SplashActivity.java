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

import com.example.mad_nnm23cs124.utils.ThemeManager;

public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isNavigatingAway = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        View splashRoot = findViewById(R.id.splashRoot);
        View cardLogo = findViewById(R.id.cardLogo);
        View viewGlowRing = findViewById(R.id.viewGlowRing);
        View chipBle = findViewById(R.id.chipBle);
        View chipTrackpad = findViewById(R.id.chipTrackpad);
        View chipAirMouse = findViewById(R.id.chipAirMouse);
        View bottomBrandContainer = findViewById(R.id.bottomBrandContainer);
        View dotPulse = findViewById(R.id.dotPulse);

        // Initial setup for entrance animations
        if (cardLogo != null) {
            cardLogo.setScaleX(0.4f);
            cardLogo.setScaleY(0.4f);
            cardLogo.setAlpha(0f);
        }
        if (viewGlowRing != null) {
            viewGlowRing.setScaleX(0.2f);
            viewGlowRing.setScaleY(0.2f);
            viewGlowRing.setAlpha(0f);
        }
        if (chipBle != null) {
            chipBle.setTranslationY(-40f);
            chipBle.setAlpha(0f);
        }
        if (chipTrackpad != null) {
            chipTrackpad.setTranslationX(-50f);
            chipTrackpad.setAlpha(0f);
        }
        if (chipAirMouse != null) {
            chipAirMouse.setTranslationX(50f);
            chipAirMouse.setAlpha(0f);
        }
        if (bottomBrandContainer != null) {
            bottomBrandContainer.setTranslationY(30f);
            bottomBrandContainer.setAlpha(0f);
        }

        // 1. Entrance animation (Hero Logo spring scale-in)
        if (cardLogo != null) {
            cardLogo.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(750)
                    .setInterpolator(new OvershootInterpolator(1.4f))
                    .start();
        }

        // 2. Glow ring expand
        if (viewGlowRing != null) {
            viewGlowRing.animate()
                    .scaleX(1.35f)
                    .scaleY(1.35f)
                    .alpha(0.35f)
                    .setDuration(900)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // 3. Staggered entrance for floating 3D chips (Corner style)
        if (chipBle != null) {
            chipBle.animate()
                    .translationY(0f)
                    .alpha(1.0f)
                    .setStartDelay(250)
                    .setDuration(600)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
            startFloatingBob(chipBle, -12f, 1800);
        }

        if (chipTrackpad != null) {
            chipTrackpad.animate()
                    .translationX(0f)
                    .alpha(1.0f)
                    .setStartDelay(400)
                    .setDuration(600)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
            startFloatingBob(chipTrackpad, 10f, 2100);
        }

        if (chipAirMouse != null) {
            chipAirMouse.animate()
                    .translationX(0f)
                    .alpha(1.0f)
                    .setStartDelay(550)
                    .setDuration(600)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
            startFloatingBob(chipAirMouse, -8f, 1900);
        }

        // 4. Typography & status fade-in
        if (bottomBrandContainer != null) {
            bottomBrandContainer.animate()
                    .translationY(0f)
                    .alpha(1.0f)
                    .setStartDelay(350)
                    .setDuration(600)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // 5. Breathing dot animation
        if (dotPulse != null) {
            ObjectAnimator pulseAnim = ObjectAnimator.ofFloat(dotPulse, "alpha", 0.3f, 1.0f);
            pulseAnim.setDuration(600);
            pulseAnim.setRepeatCount(ValueAnimator.INFINITE);
            pulseAnim.setRepeatMode(ValueAnimator.REVERSE);
            pulseAnim.start();
        }

        // Tap to skip intro immediately
        if (splashRoot != null) {
            splashRoot.setOnClickListener(v -> navigateToMain());
        }

        // Seamless 2.4-second transition into MainActivity
        handler.postDelayed(this::navigateToMain, 2400);
    }

    private void startFloatingBob(View view, float offset, long duration) {
        ObjectAnimator bob = ObjectAnimator.ofFloat(view, "translationY", -offset / 2f, offset / 2f);
        bob.setDuration(duration);
        bob.setRepeatCount(ValueAnimator.INFINITE);
        bob.setRepeatMode(ValueAnimator.REVERSE);
        bob.setInterpolator(new AccelerateDecelerateInterpolator());
        bob.setStartDelay(650);
        bob.start();
    }

    private void navigateToMain() {
        if (isNavigatingAway) return;
        isNavigatingAway = true;
        handler.removeCallbacksAndMessages(null);

        View splashRoot = findViewById(R.id.splashRoot);
        if (splashRoot != null) {
            splashRoot.animate()
                    .alpha(0f)
                    .scaleX(1.04f)
                    .scaleY(1.04f)
                    .setDuration(320)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    })
                    .start();
        } else {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
