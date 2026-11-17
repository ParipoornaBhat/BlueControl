package com.example.mad_nnm23cs124.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    public static final String PREF_NAME = "blue_control_prefs";
    public static final String KEY_DARK_MODE = "dark_mode";
    public static final String KEY_THEME_MODE = "theme_mode";

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    /**
     * Applies the persisted theme preference across the application.
     */
    public static void applyTheme(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (prefs.contains(KEY_THEME_MODE)) {
            int mode = prefs.getInt(KEY_THEME_MODE, THEME_DARK);
            switch (mode) {
                case THEME_LIGHT:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    break;
                case THEME_SYSTEM:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    break;
                case THEME_DARK:
                default:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    break;
            }
        } else {
            // Fallback to legacy boolean key
            boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, true);
            if (isDarkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        }
    }

    /**
     * Sets explicit dark or light mode, updates preferences, and applies immediately.
     */
    public static void setDarkMode(@NonNull Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_DARK_MODE, enabled)
                .putInt(KEY_THEME_MODE, enabled ? THEME_DARK : THEME_LIGHT)
                .apply();
        applyTheme(context);
    }

    /**
     * Returns true if dark mode is active either explicitly or via system default.
     */
    public static boolean isDarkMode(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (prefs.contains(KEY_THEME_MODE)) {
            int mode = prefs.getInt(KEY_THEME_MODE, THEME_DARK);
            if (mode == THEME_LIGHT) return false;
            if (mode == THEME_DARK) return true;
            // For system theme, check current configuration
            int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return nightMode == Configuration.UI_MODE_NIGHT_YES;
        }
        return prefs.getBoolean(KEY_DARK_MODE, true);
    }

    /**
     * Sets the theme mode (THEME_SYSTEM, THEME_LIGHT, or THEME_DARK).
     */
    public static void setThemeMode(@NonNull Context context, int themeMode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt(KEY_THEME_MODE, themeMode)
                .putBoolean(KEY_DARK_MODE, themeMode != THEME_LIGHT)
                .apply();
        applyTheme(context);
    }

    /**
     * Gets the configured theme mode.
     */
    public static int getThemeMode(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, prefs.getBoolean(KEY_DARK_MODE, true) ? THEME_DARK : THEME_LIGHT);
    }

    /**
     * Returns primary glowing cyan/blue color for visual touch indicators and trails.
     */
    @ColorInt
    public static int getTrackpadGlowColor(@NonNull Context context) {
        if (isDarkMode(context)) {
            return Color.parseColor("#38BDF8"); // Light cyan glow
        } else {
            return Color.parseColor("#0284C7"); // Sky blue glow
        }
    }

    /**
     * Returns background tint color for trackpad surface.
     */
    @ColorInt
    public static int getTrackpadSurfaceColor(@NonNull Context context) {
        if (isDarkMode(context)) {
            return Color.parseColor("#0F172A"); // Slate 900
        } else {
            return Color.parseColor("#F8FAFC"); // Slate 50
        }
    }

    /**
     * Returns grid/accent line color for trackpad.
     */
    @ColorInt
    public static int getTrackpadGridColor(@NonNull Context context) {
        if (isDarkMode(context)) {
            return Color.parseColor("#1E293B"); // Slate 800
        } else {
            return Color.parseColor("#E2E8F0"); // Slate 200
        }
    }
}
