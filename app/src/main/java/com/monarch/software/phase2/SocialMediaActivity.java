package com.monarch.software.phase2;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.monarch.software.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SocialMediaActivity extends AppCompatActivity {

    // -----------------------------------------------------------------------
    // 20 feed posts: {emoji, category, title, snippet}
    // -----------------------------------------------------------------------
    static final String[][] ALL_POSTS = {
        {"🏛️", "ARCHITECTURE", "Building of the U.S. Capitol",        "The iconic dome stands tall against a clear blue sky in Washington D.C."},
        {"🌲", "NATURE",        "Winding road in a forest",            "A scenic road curves through a dense pine forest in the Pacific Northwest."},
        {"💻", "LIFESTYLE",     "Woman working on a laptop",           "A professional works from a bright, modern coworking space downtown."},
        {"🍔", "FOOD",          "Delicious looking cheeseburger",      "A juicy double cheeseburger with fresh lettuce and tomato on a wooden board."},
        {"🐕", "ANIMALS",       "Golden retriever playing fetch",      "A happy golden retriever leaps through tall grass chasing a bright yellow ball."},
        {"🌅", "NATURE",        "Sunset over the ocean",               "Warm shades of orange and pink paint the sky above the Pacific horizon."},
        {"🌆", "URBAN",         "City skyline at night",               "Glittering lights reflect off the river as the city skyline comes alive after dark."},
        {"🥗", "FOOD",          "Fresh fruit salad bowl",              "A colourful bowl of mixed berries, mango, and mint makes for a healthy snack."},
        {"🥾", "OUTDOORS",      "Hiking trail in the mountains",       "A narrow dirt path winds up a steep ridge with panoramic views of the valley below."},
        {"⚡", "TECHNOLOGY",    "Electric vehicle charging station",   "A row of fast-charging stations sits at a busy shopping centre car park."},
        {"👧", "LIFESTYLE",     "Children playing in a park",          "Kids laugh and chase each other around a colourful playground on a sunny afternoon."},
        {"☕", "FOOD",          "Artisan coffee being poured",         "A barista carefully pours steamed milk into a cortado, creating a perfect rosette."},
        {"🏔️", "NATURE",        "Snowy mountain peak",                 "Fresh powder coats the summit as clouds drift past a towering alpine peak."},
        {"🖼️", "ARTS",          "Modern art museum interior",          "Visitors stroll past large abstract canvases in a sunlit gallery with high ceilings."},
        {"🍜", "FOOD",          "Street food market in Asia",          "Vendors serve steaming bowls of noodles and skewers at a vibrant night market."},
        {"🚴", "SPORTS",        "Cyclist on a mountain trail",         "A mountain biker navigates a rocky descent through a pine-covered hillside."},
        {"🐘", "ANIMALS",       "Baby elephant at a zoo",              "A playful baby elephant splashes water with its trunk in a sun-dappled enclosure."},
        {"🌿", "LIFESTYLE",     "Rooftop garden in a city",            "Urban farmers tend neat rows of vegetables growing on a glass-fronted rooftop terrace."},
        {"🚗", "AUTOMOTIVE",    "Classic red sports car",              "A gleaming vintage roadster is displayed under spotlights at a classic car show."},
        {"🏠", "NATURE",        "Lighthouse on a rocky coast",         "A white-and-red lighthouse overlooks crashing waves on a rugged Atlantic coastline."}
    };

    // 5 trials: index of the target post in ALL_POSTS
    static final int[] TRIAL_TARGETS = {2, 7, 14, 4, 18};

    // -----------------------------------------------------------------------
    // Touch logging
    // -----------------------------------------------------------------------
    private TouchDbHelper dbHelper;
    private SQLiteDatabase touchDb;
    private VelocityTracker velocityTracker;
    private int gestureId       = 0;
    private int moveEventCount  = 0;   // used to sample MOVE events
    private long sessionStart;

    // -----------------------------------------------------------------------
    // UI
    // -----------------------------------------------------------------------
    private int currentTrial = 0;
    private TextView tvTrialLabel, tvTargetDescription;
    private RecyclerView recycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social_media);

        dbHelper     = new TouchDbHelper(this);
        touchDb      = dbHelper.getWritableDatabase();
        sessionStart = System.currentTimeMillis();

        tvTrialLabel        = findViewById(R.id.tv_trial_label);
        tvTargetDescription = findViewById(R.id.tv_target_description);
        recycler            = findViewById(R.id.recycler_feed);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        loadTrial(0);
    }

    // -----------------------------------------------------------------------
    // Intercept every touch event in this Activity window for logging
    // -----------------------------------------------------------------------
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                gestureId++;
                moveEventCount = 0;
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain();
                } else {
                    velocityTracker.clear();
                }
                velocityTracker.addMovement(event);
                saveTouchEvent(event, "DOWN", 0f, 0f);
                break;

            case MotionEvent.ACTION_MOVE:
                if (velocityTracker != null) velocityTracker.addMovement(event);
                // Sample every 3rd MOVE to reduce DB writes while preserving trajectory shape
                moveEventCount++;
                if (moveEventCount % 3 == 0) {
                    saveTouchEvent(event, "MOVE", 0f, 0f);
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (velocityTracker != null) {
                    velocityTracker.addMovement(event);
                    velocityTracker.computeCurrentVelocity(1000); // px per second
                    float vx = velocityTracker.getXVelocity();
                    float vy = velocityTracker.getYVelocity();
                    saveTouchEvent(event, action == MotionEvent.ACTION_UP ? "UP" : "CANCEL", vx, vy);
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                break;
        }

        return super.dispatchTouchEvent(event);
    }

    private void saveTouchEvent(final MotionEvent event, final String type,
                                final float vx, final float vy) {
        final float x        = event.getX();
        final float y        = event.getY();
        final float rawX     = event.getRawX();
        final float rawY     = event.getRawY();
        final float pressure = event.getPressure();
        final float size     = event.getSize();
        final long  ts       = System.currentTimeMillis();
        final int   trial    = currentTrial;
        final int   gid      = gestureId;

        // Write off the main thread to keep UI smooth
        new Thread(() -> {
            ContentValues cv = new ContentValues();
            cv.put(TouchDbHelper.COL_TASK_TYPE,         "social");
            cv.put(TouchDbHelper.COL_TRIAL_NUM,         trial);
            cv.put(TouchDbHelper.COL_GESTURE_ID,        gid);
            cv.put(TouchDbHelper.COL_EVENT_TYPE,        type);
            cv.put(TouchDbHelper.COL_X,                 x);
            cv.put(TouchDbHelper.COL_Y,                 y);
            cv.put(TouchDbHelper.COL_RAW_X,             rawX);
            cv.put(TouchDbHelper.COL_RAW_Y,             rawY);
            cv.put(TouchDbHelper.COL_PRESSURE,          pressure);
            cv.put(TouchDbHelper.COL_TOUCH_SIZE,        size);
            cv.put(TouchDbHelper.COL_TIMESTAMP_MS,      ts);
            cv.put(TouchDbHelper.COL_VELOCITY_X,        vx);
            cv.put(TouchDbHelper.COL_VELOCITY_Y,        vy);
            cv.put(TouchDbHelper.COL_SESSION_TIMESTAMP, sessionStart);
            touchDb.insert(TouchDbHelper.TABLE, null, cv);
        }).start();
    }

    // -----------------------------------------------------------------------
    // Trial logic
    // -----------------------------------------------------------------------
    private void loadTrial(int trial) {
        currentTrial = trial;
        tvTrialLabel.setText("Trial " + (trial + 1) + " / " + TRIAL_TARGETS.length);

        int targetIdx = TRIAL_TARGETS[trial];
        tvTargetDescription.setText(ALL_POSTS[targetIdx][2]);

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < ALL_POSTS.length; i++) indices.add(i);
        Collections.shuffle(indices);

        FeedAdapter adapter = new FeedAdapter(indices, targetIdx, this::onPostTapped);
        recycler.setAdapter(adapter);
        recycler.scrollToPosition(0);
    }

    private void onPostTapped(int postIndex, boolean isTarget) {
        showResult(isTarget);
    }

    private void showResult(boolean correct) {
        new AlertDialog.Builder(this)
            .setTitle(correct ? "✅  Correct!" : "❌  Not quite")
            .setMessage(correct
                ? "You found the right post."
                : "That wasn't the target post. Moving on.")
            .setCancelable(false)
            .setPositiveButton("Next →", (d, w) -> {
                int next = currentTrial + 1;
                if (next >= TRIAL_TARGETS.length) onAllTrialsDone();
                else loadTrial(next);
            })
            .show();
    }

    private void onAllTrialsDone() {
        getSharedPreferences(Phase2HomeActivity.PREFS, MODE_PRIVATE)
            .edit().putBoolean(Phase2HomeActivity.KEY_SOCIAL, true).apply();

        new AlertDialog.Builder(this)
            .setTitle("🎉  Task Complete!")
            .setMessage("All 5 social media trials finished. Touch data saved.")
            .setCancelable(false)
            .setPositiveButton("Back to Home", (d, w) -> finish())
            .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (touchDb != null) touchDb.close();
    }

    // -----------------------------------------------------------------------
    // RecyclerView adapter
    // -----------------------------------------------------------------------
    interface OnPostTapped { void onTap(int postIndex, boolean isTarget); }

    static class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.VH> {

        private final List<Integer> order;
        private final int targetIdx;
        private final OnPostTapped listener;

        FeedAdapter(List<Integer> order, int targetIdx, OnPostTapped listener) {
            this.order     = order;
            this.targetIdx = targetIdx;
            this.listener  = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_social_post, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            int idx       = order.get(position);
            String[] post = SocialMediaActivity.ALL_POSTS[idx];

            h.tvThumb.setText(post[0]);
            h.tvThumb.setBackgroundColor(thumbColor(idx));
            h.tvCategory.setText(post[1]);
            h.tvTitle.setText(post[2]);
            h.tvSnippet.setText(post[3]);
            h.itemView.setTag(idx);
            h.itemView.setOnClickListener(v -> listener.onTap(idx, idx == targetIdx));
        }

        @Override public int getItemCount() { return order.size(); }

        private int thumbColor(int idx) {
            int[] colors = {
                0xFFE8EAF6, 0xFFE8F5E9, 0xFFFFF8E1, 0xFFFFEBEE,
                0xFFE0F7FA, 0xFFF3E5F5, 0xFFE1F5FE, 0xFFE8F5E9,
                0xFFFFF3E0, 0xFFE8EAF6, 0xFFFFEBEE, 0xFFF1F8E9,
                0xFFE0F2F1, 0xFFFCE4EC, 0xFFF9FBE7, 0xFFE8EAF6,
                0xFFE8F5E9, 0xFFE0F2F1, 0xFFFFEBEE, 0xFFE3F2FD
            };
            return colors[idx % colors.length];
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvThumb, tvCategory, tvTitle, tvSnippet;
            VH(View v) {
                super(v);
                tvThumb    = v.findViewById(R.id.tv_thumbnail);
                tvCategory = v.findViewById(R.id.tv_category);
                tvTitle    = v.findViewById(R.id.tv_title);
                tvSnippet  = v.findViewById(R.id.tv_snippet);
            }
        }
    }
}
