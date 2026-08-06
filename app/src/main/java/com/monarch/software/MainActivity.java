package com.monarch.software;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.monarch.software.biometrics.BiometricsRegisterActivity;
import com.monarch.software.keystroke.KeystrokeHomeActivity;
import com.monarch.software.phase2.Phase2HomeActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton themeToggle = findViewById(R.id.btn_theme_toggle);
        boolean darkMode = ThemeManager.isDark(this);
        themeToggle.setImageResource(
                darkMode ? R.drawable.ic_theme_sun : R.drawable.ic_theme_moon);
        themeToggle.setContentDescription(getString(
                darkMode ? R.string.theme_switch_light : R.string.theme_switch_dark));
        themeToggle.setOnClickListener(view -> {
            view.setEnabled(false);
            view.animate()
                    .rotationBy(180f)
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .setDuration(140)
                    .withEndAction(() -> ThemeManager.toggle(this))
                    .start();
        });

        findViewById(R.id.card_keystroke).setOnClickListener(v ->
                startActivity(new Intent(this, KeystrokeHomeActivity.class)));

        findViewById(R.id.card_biometrics).setOnClickListener(v ->
                startActivity(new Intent(this, BiometricsRegisterActivity.class)));

        findViewById(R.id.card_social).setOnClickListener(v ->
                startActivity(new Intent(this, Phase2HomeActivity.class)));
    }
}
