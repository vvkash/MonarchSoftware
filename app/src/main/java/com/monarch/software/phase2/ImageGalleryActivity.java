package com.monarch.software.phase2;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.monarch.software.R;

public class ImageGalleryActivity extends AppCompatActivity {

    // -----------------------------------------------------------------------
    // Object definitions: {name, emoji, hex background color}
    // -----------------------------------------------------------------------
    static final String[][] OBJECTS = {
        {"DOG",     "🐕", "#FF8F00"},
        {"CAT",     "🐈", "#00897B"},
        {"PIZZA",   "🍕", "#F4511E"},
        {"BICYCLE", "🚲", "#1E88E5"},
        {"APPLE",   "🍎", "#E53935"},
        {"CAR",     "🚗", "#546E7A"},
        {"BIRD",    "🐦", "#039BE5"},
        {"BOOK",    "📚", "#6D4C41"},
        {"FLOWER",  "🌸", "#D81B60"},
        {"HOUSE",   "🏠", "#43A047"},
    };

    // 5 trials: {targetObjectIdx, correctCount, img0..img19}
    static final int[][] TRIALS = {
        {0, 3,  0,2,5,1,3,0,7,4,6,9,0,8,3,5,2,6,1,4,7,9},
        {1, 4,  3,1,7,1,0,5,1,2,8,6,9,4,1,3,0,7,5,2,6,8},
        {2, 2,  6,0,4,7,2,9,1,5,3,8,6,0,4,2,7,1,5,3,8,9},
        {3, 5,  3,1,3,7,0,3,2,4,3,6,9,3,5,8,0,1,2,4,6,7},
        {4, 3,  2,7,4,1,6,4,0,3,5,9,4,8,1,2,7,6,0,3,5,9},
    };

    // -----------------------------------------------------------------------
    // Touch logging
    // -----------------------------------------------------------------------
    private TouchDbHelper dbHelper;
    private SQLiteDatabase touchDb;
    private VelocityTracker velocityTracker;
    private int gestureId      = 0;
    private int moveEventCount = 0;
    private long sessionStart;

    // -----------------------------------------------------------------------
    // UI
    // -----------------------------------------------------------------------
    private int currentTrial = 0;
    private int currentPage  = 0;

    private TextView tvGalleryTrial, tvTargetEmoji, tvTargetObject, tvPosition;
    private LinearLayout llDots;
    private ViewPager2 viewPager;
    private TextInputEditText etCount;
    private MaterialButton btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_gallery);

        dbHelper     = new TouchDbHelper(this);
        touchDb      = dbHelper.getWritableDatabase();
        sessionStart = System.currentTimeMillis();

        tvGalleryTrial = findViewById(R.id.tv_gallery_trial);
        tvTargetEmoji  = findViewById(R.id.tv_target_emoji);
        tvTargetObject = findViewById(R.id.tv_target_object);
        tvPosition     = findViewById(R.id.tv_position);
        llDots         = findViewById(R.id.ll_dots);
        viewPager      = findViewById(R.id.view_pager);
        etCount        = findViewById(R.id.et_count);
        btnSubmit      = findViewById(R.id.btn_submit_count);

        btnSubmit.setOnClickListener(v -> onSubmitCount());

        loadTrial(0);
    }

    // -----------------------------------------------------------------------
    // Intercept every touch in this Activity for logging
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
                moveEventCount++;
                if (moveEventCount % 3 == 0) {
                    saveTouchEvent(event, "MOVE", 0f, 0f);
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (velocityTracker != null) {
                    velocityTracker.addMovement(event);
                    velocityTracker.computeCurrentVelocity(1000);
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
        final int   page     = currentPage;
        final int   gid      = gestureId;

        new Thread(() -> {
            ContentValues cv = new ContentValues();
            cv.put(TouchDbHelper.COL_TASK_TYPE,         "gallery");
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
        currentPage  = 0;

        int[] trialData  = TRIALS[trial];
        String[] obj     = OBJECTS[trialData[0]];

        tvGalleryTrial.setText("Trial " + (trial + 1) + " / " + TRIALS.length);
        tvTargetEmoji.setText(obj[1]);
        tvTargetObject.setText("How many " + obj[0] + "S do you see?");
        etCount.setText("");

        int[] images = new int[20];
        for (int i = 0; i < 20; i++) images[i] = trialData[i + 2];

        GalleryAdapter adapter = new GalleryAdapter(this, images);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0, false);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPage = position;
                tvPosition.setText((position + 1) + " / 20");
                updateDots(position);
            }
        });

        buildDots();
        tvPosition.setText("1 / 20");
    }

    private void buildDots() {
        llDots.removeAllViews();
        int dp4 = (int)(4 * getResources().getDisplayMetrics().density);
        int dp8 = dp4 * 2;
        for (int i = 0; i < 10; i++) {  // show 10 dots representing 20 images
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp8, dp8);
            lp.setMargins(dp4, 0, dp4, 0);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(i == 0 ? R.drawable.dot_active : R.drawable.dot_inactive);
            llDots.addView(dot);
        }
    }

    private void updateDots(int page) {
        int dotIdx = page * 10 / 20;
        for (int i = 0; i < llDots.getChildCount(); i++) {
            llDots.getChildAt(i).setBackgroundResource(
                i == dotIdx ? R.drawable.dot_active : R.drawable.dot_inactive);
        }
    }

    private void onSubmitCount() {
        String input = etCount.getText() != null ? etCount.getText().toString().trim() : "";
        if (input.isEmpty()) {
            Toast.makeText(this, "Please enter your count", Toast.LENGTH_SHORT).show();
            return;
        }
        int userCount = Integer.parseInt(input);
        int correct   = TRIALS[currentTrial][1];
        String objName= OBJECTS[TRIALS[currentTrial][0]][0];
        boolean right = (userCount == correct);

        new AlertDialog.Builder(this)
            .setTitle(right ? "✅  Correct!" : "❌  Not quite")
            .setMessage(right
                ? "There were " + correct + " " + objName + "S. Well done!"
                : "The correct answer was " + correct + " " + objName + "S. You entered " + userCount + ".")
            .setCancelable(false)
            .setPositiveButton("Next →", (d, w) -> {
                int next = currentTrial + 1;
                if (next >= TRIALS.length) onAllTrialsDone();
                else loadTrial(next);
            })
            .show();
    }

    private void onAllTrialsDone() {
        getSharedPreferences(Phase2HomeActivity.PREFS, MODE_PRIVATE)
            .edit().putBoolean(Phase2HomeActivity.KEY_GALLERY, true).apply();

        new AlertDialog.Builder(this)
            .setTitle("🎉  Task Complete!")
            .setMessage("All 5 image gallery trials finished. Touch data saved.")
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
    // ViewPager2 adapter
    // -----------------------------------------------------------------------
    static class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.VH> {

        private final Context ctx;
        private final int[] images;

        GalleryAdapter(Context ctx, int[] images) {
            this.ctx    = ctx;
            this.images = images;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(ctx).inflate(R.layout.item_gallery_page, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            String[] obj = OBJECTS[images[position]];
            h.bg.setBackgroundColor(Color.parseColor(obj[2]));
            h.tvEmoji.setText(obj[1]);
            h.tvLabel.setText(obj[0]);
        }

        @Override public int getItemCount() { return images.length; }

        static class VH extends RecyclerView.ViewHolder {
            LinearLayout bg;
            TextView tvEmoji, tvLabel;
            VH(View v) {
                super(v);
                bg      = v.findViewById(R.id.card_bg);
                tvEmoji = v.findViewById(R.id.tv_object_emoji);
                tvLabel = v.findViewById(R.id.tv_object_label);
            }
        }
    }
}
