package com.monarch.software.phase2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.monarch.software.R;

public class Phase2HomeActivity extends AppCompatActivity {

    static final String PREFS       = "phase2_prefs";
    static final String KEY_SOCIAL  = "social_done";
    static final String KEY_GALLERY = "gallery_done";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.p2_activity_home);

        findViewById(R.id.card_open_feed).setOnClickListener(v ->
                startActivity(new Intent(this, InstaFeedActivity.class)));
        findViewById(R.id.card_social_task).setOnClickListener(v ->
                startActivity(new Intent(this, SocialMediaActivity.class)));
        findViewById(R.id.card_gallery_task).setOnClickListener(v ->
                startActivity(new Intent(this, ImageGalleryActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean socialDone = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(KEY_SOCIAL, false);
        boolean galleryDone = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(KEY_GALLERY, false);
        updateStatus(R.id.tv_social_status, socialDone);
        updateStatus(R.id.tv_gallery_status, galleryDone);
    }

    private void updateStatus(int viewId, boolean complete) {
        TextView status = findViewById(viewId);
        status.setText(complete ? "Complete" : "5 trials");
        status.setTextColor(ContextCompat.getColor(
                this, complete ? R.color.success : R.color.brand_violet));
    }
}
