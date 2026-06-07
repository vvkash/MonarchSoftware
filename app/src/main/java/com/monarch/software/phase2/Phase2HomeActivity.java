package com.monarch.software.phase2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.monarch.software.R;

public class Phase2HomeActivity extends AppCompatActivity {

    static final String PREFS       = "phase2_prefs";
    static final String KEY_SOCIAL  = "social_done";
    static final String KEY_GALLERY = "gallery_done";

    static final int REQ_SOCIAL  = 1;
    static final int REQ_GALLERY = 2;

    private TextView tvTasksDone, tvSocialStatus, tvGalleryStatus;
    private Button btnStartSocial, btnStartGallery, btnOpenFeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.p2_activity_home);

        tvTasksDone    = findViewById(R.id.tv_tasks_done);
        tvSocialStatus = findViewById(R.id.tv_social_status);
        tvGalleryStatus= findViewById(R.id.tv_gallery_status);
        btnStartSocial = findViewById(R.id.btn_start_social);
        btnStartGallery= findViewById(R.id.btn_start_gallery);
        btnOpenFeed    = findViewById(R.id.btn_open_feed);

        btnStartSocial.setOnClickListener(v -> {
            startActivityForResult(new Intent(this, SocialMediaActivity.class), REQ_SOCIAL);
        });

        btnStartGallery.setOnClickListener(v -> {
            startActivityForResult(new Intent(this, ImageGalleryActivity.class), REQ_GALLERY);
        });

        btnOpenFeed.setOnClickListener(v -> {
            startActivity(new Intent(this, InstaFeedActivity.class));
        });

        refreshUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }

    private void refreshUI() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean socialDone  = prefs.getBoolean(KEY_SOCIAL, false);
        boolean galleryDone = prefs.getBoolean(KEY_GALLERY, false);
        int done = (socialDone ? 1 : 0) + (galleryDone ? 1 : 0);

        tvTasksDone.setText(done + " / 2 done");

        if (socialDone) {
            tvSocialStatus.setText("✓  Done");
            tvSocialStatus.setTextColor(Color.parseColor("#10B981"));
            tvSocialStatus.setBackground(getDrawable(R.drawable.chip_bg_green));
            btnStartSocial.setText("Redo Task");
        } else {
            tvSocialStatus.setText("Pending");
            tvSocialStatus.setTextColor(Color.parseColor("#6B7280"));
            tvSocialStatus.setBackground(getDrawable(R.drawable.chip_bg_gray));
            btnStartSocial.setText("Start Task");
        }

        if (galleryDone) {
            tvGalleryStatus.setText("✓  Done");
            tvGalleryStatus.setTextColor(Color.parseColor("#10B981"));
            tvGalleryStatus.setBackground(getDrawable(R.drawable.chip_bg_green));
            btnStartGallery.setText("Redo Task");
        } else {
            tvGalleryStatus.setText("Pending");
            tvGalleryStatus.setTextColor(Color.parseColor("#6B7280"));
            tvGalleryStatus.setBackground(getDrawable(R.drawable.chip_bg_gray));
            btnStartGallery.setText("Start Task");
        }
    }
}
