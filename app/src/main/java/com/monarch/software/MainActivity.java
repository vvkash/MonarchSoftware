package com.monarch.software;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.monarch.software.biometrics.BiometricsRegisterActivity;
import com.monarch.software.keystroke.KeystrokeHomeActivity;
import com.monarch.software.phase2.Phase2HomeActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.card_keystroke).setOnClickListener(v ->
                startActivity(new Intent(this, KeystrokeHomeActivity.class)));

        findViewById(R.id.card_biometrics).setOnClickListener(v ->
                startActivity(new Intent(this, BiometricsRegisterActivity.class)));

        findViewById(R.id.card_social).setOnClickListener(v ->
                startActivity(new Intent(this, Phase2HomeActivity.class)));
    }
}
