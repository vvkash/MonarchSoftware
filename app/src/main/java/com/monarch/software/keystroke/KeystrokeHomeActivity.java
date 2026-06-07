package com.monarch.software.keystroke;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.monarch.software.R;

public class KeystrokeHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ks_home);

        findViewById(R.id.card_pin_auth).setOnClickListener(v ->
                startActivity(new Intent(this, KeystrokeMainActivity.class)));

        findViewById(R.id.card_typing_study).setOnClickListener(v ->
                startActivity(new Intent(this, TypingStudyActivity.class)));

        findViewById(R.id.card_swipe_study).setOnClickListener(v ->
                startActivity(new Intent(this, SwipeStudyActivity.class)));
    }
}
