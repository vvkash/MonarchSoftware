package com.monarch.software.phase2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.monarch.software.R;

public class Phase2HomeActivity extends AppCompatActivity {

    static final String PREFS       = "phase2_prefs";
    static final String KEY_SOCIAL  = "social_done";
    static final String KEY_GALLERY = "gallery_done";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.p2_activity_home);

        Button btnOpenFeed = findViewById(R.id.btn_open_feed);
        btnOpenFeed.setOnClickListener(v ->
                startActivity(new Intent(this, InstaFeedActivity.class)));
    }
}
