package com.monarch.software.keystroke;

import com.monarch.software.R;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Swipe / scroll study activity modelled on the FETA paper.
 *
 * Presents a scrollable feed of short articles.  Every finger gesture
 * (down → move* → up) on the RecyclerView is captured as one stroke row
 * in the swipe_study_sessions table.  Each row contains:
 *   - touch_events : [{t, x, y, pressure, area, action}, …]   (normalised to [0,1])
 *   - duration_ms  : ACTION_DOWN → ACTION_UP
 *   - direction    : up / down / left / right
 *   - accel_data, gyro_data, mag_data : IMU at ~100 Hz
 */
public class SwipeStudyActivity extends AppCompatActivity implements SensorEventListener {

    // -----------------------------------------------------------------------
    // Article feed content
    // -----------------------------------------------------------------------
    private static final String[] ARTICLE_TITLES = {
        "Scientists Discover New Species in Amazon",
        "Stock Markets Reach Record Highs",
        "Local Team Wins Championship After 20 Years",
        "New Study Links Sleep to Productivity",
        "City Launches Free Public Transit Program",
        "Breakthrough in Solar Panel Efficiency",
        "Restaurant Week Returns to Downtown",
        "Tech Giant Announces Layoffs",
        "NASA Plans Return to the Moon",
        "Researchers Find Cure for Common Cold",
        "Electric Vehicle Sales Surpass Expectations",
        "Community Garden Opens in Riverside Park",
        "New App Helps Track Daily Water Intake",
        "Historic Bridge Restoration Complete",
        "University Offers Free Online Courses"
    };

    private static final String[] ARTICLE_BODIES = {
        "A team of biologists has identified over 40 previously unknown species during a six-month expedition deep in the Amazon rainforest, raising hopes for biodiversity conservation efforts.",
        "Major indices closed at all-time highs on Thursday as strong earnings reports from technology companies boosted investor confidence ahead of next week's Federal Reserve meeting.",
        "The home crowd erupted in celebration as the final whistle blew, ending a two-decade drought for the city's beloved football club with a stunning 3-2 victory in overtime.",
        "Researchers at Stanford found that adults who sleep between 7 and 9 hours per night are 34% more productive and report significantly lower stress levels than those who sleep less.",
        "Starting Monday, all city buses and light-rail lines will be free of charge for residents, part of a pilot program aimed at reducing traffic congestion and carbon emissions.",
        "Engineers at MIT have developed a new solar cell coating that boosts energy conversion efficiency from 22% to nearly 35%, potentially cutting the cost of solar power in half.",
        "More than 200 local restaurants are participating this year, offering prix-fixe menus at reduced prices throughout the two-week event running from October 14 to October 28.",
        "The company confirmed it would reduce its global workforce by 8%, citing slowing consumer demand and rising operational costs. Affected employees will receive severance packages.",
        "NASA unveiled a detailed timeline for the Artemis program, targeting a crewed lunar landing by late 2026 and the establishment of a permanent lunar gateway station by 2030.",
        "A antiviral compound developed at Johns Hopkins has shown 91% efficacy against rhinovirus strains in Phase II trials, with Phase III trials expected to begin early next year.",
        "Sales of battery electric vehicles grew 47% year-over-year in the third quarter, outpacing analyst forecasts as charging infrastructure expanded across rural and suburban areas.",
        "The two-acre community garden features over 80 individual plots available to residents at low cost, along with a greenhouse, composting station, and weekly gardening workshops.",
        "The WaterWise app syncs with smart bottles and sends reminders based on your activity level, weather, and body weight. Over 500,000 users have downloaded it in its first week.",
        "After three years of careful restoration work, the 1887 iron bridge has reopened to pedestrians and cyclists, preserving a key piece of the city's industrial heritage.",
        "The initiative covers subjects ranging from machine learning and data analysis to creative writing and philosophy, with certificates available for a nominal administrative fee."
    };

    // -----------------------------------------------------------------------
    // UI
    // -----------------------------------------------------------------------
    private TextView tvStrokeCount;
    private Button   btnDone;
    private RecyclerView recyclerView;

    // -----------------------------------------------------------------------
    // Stroke state
    // -----------------------------------------------------------------------
    private int strokeIndex = 0;

    /** Touch events accumulated for the stroke currently in progress. */
    private JSONArray currentTouchEvents;

    /** Epoch-ms timestamp of ACTION_DOWN for current stroke. */
    private long strokeStartTime = 0;

    /** Starting X/Y used to classify swipe direction. */
    private float startX = 0, startY = 0;

    /** Screen dimensions for normalisation. */
    private int screenWidth = 1, screenHeight = 1;

    // -----------------------------------------------------------------------
    // IMU state
    // -----------------------------------------------------------------------
    private SensorManager mSensorMgr;
    private Sensor mAccelerometer, mGyroscope, mMagnetometer;

    /** IMU readings for the stroke currently in progress. */
    private JSONArray mAccelData, mGyroData, mMagData;

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ks_swipe_study);

        tvStrokeCount = findViewById(R.id.tv_stroke_count);
        btnDone       = findViewById(R.id.btn_swipe_done);
        recyclerView  = findViewById(R.id.rv_feed);

        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        screenWidth  = dm.widthPixels;
        screenHeight = dm.heightPixels;

        mSensorMgr    = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope     = mSensorMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mMagnetometer  = mSensorMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new ArticleFeedAdapter(ARTICLE_TITLES, ARTICLE_BODIES));

        // Intercept touch events WITHOUT consuming them so scroll still works
        recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                handleTouchEvent(e);
                return false; // do not consume
            }
        });

        btnDone.setOnClickListener(v -> {
            Toast.makeText(this,
                    strokeIndex + " swipe strokes saved.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SwipeResultsActivity.class));
            finish();
        });

        resetStrokeBuffers();
        updateStrokeCount();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int delay = 10_000; // ~100 Hz
        if (mAccelerometer != null) mSensorMgr.registerListener(this, mAccelerometer, delay);
        if (mGyroscope     != null) mSensorMgr.registerListener(this, mGyroscope,     delay);
        if (mMagnetometer  != null) mSensorMgr.registerListener(this, mMagnetometer,  delay);
        startService(new Intent(this, KeystrokeSensorService.class));
    }

    @Override
    protected void onPause() {
        super.onPause();
        // sensors stay registered so background collection continues
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSensorMgr.unregisterListener(this);
        stopService(new Intent(this, KeystrokeSensorService.class));
    }

    // -----------------------------------------------------------------------
    // SensorEventListener
    // -----------------------------------------------------------------------

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (strokeStartTime == 0) return; // only record during an active stroke
        JSONObject reading = new JSONObject();
        try {
            reading.put("t", event.timestamp / 1_000_000L);
            reading.put("x", event.values[0]);
            reading.put("y", event.values[1]);
            reading.put("z", event.values[2]);
        } catch (Exception ignored) { return; }

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:  mAccelData.put(reading); break;
            case Sensor.TYPE_GYROSCOPE:      mGyroData.put(reading);  break;
            case Sensor.TYPE_MAGNETIC_FIELD: mMagData.put(reading);   break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // -----------------------------------------------------------------------
    // Touch handling
    // -----------------------------------------------------------------------

    private void handleTouchEvent(MotionEvent e) {
        long now = System.currentTimeMillis();
        int action = e.getActionMasked();

        try {
            JSONObject point = new JSONObject();
            point.put("t",        now);
            point.put("x",        e.getX() / screenWidth);
            point.put("y",        e.getY() / screenHeight);
            point.put("pressure", e.getPressure());
            point.put("area",     e.getSize());
            point.put("action",   action);

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    resetStrokeBuffers();
                    strokeStartTime = now;
                    startX = e.getX();
                    startY = e.getY();
                    currentTouchEvents.put(point);
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (strokeStartTime > 0) currentTouchEvents.put(point);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (strokeStartTime > 0) {
                        currentTouchEvents.put(point);
                        saveStroke(now, e.getX(), e.getY());
                        strokeStartTime = 0;
                    }
                    break;
            }
        } catch (Exception ignored) {}
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void resetStrokeBuffers() {
        currentTouchEvents = new JSONArray();
        mAccelData = new JSONArray();
        mGyroData  = new JSONArray();
        mMagData   = new JSONArray();
    }

    private void saveStroke(long endTime, float endX, float endY) {
        long durationMs = endTime - strokeStartTime;
        String direction = classifyDirection(startX, startY, endX, endY);

        DBHelper dbHelper = new DBHelper(this, Database.KEYSTROKE_DYNAMICS, null, Database.VERSION);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(SwipeStudyDatabase.COL_STROKE_INDEX,      strokeIndex);
        cv.put(SwipeStudyDatabase.COL_TOUCH_EVENTS,      currentTouchEvents.toString());
        cv.put(SwipeStudyDatabase.COL_DURATION_MS,       durationMs);
        cv.put(SwipeStudyDatabase.COL_DIRECTION,         direction);
        cv.put(SwipeStudyDatabase.COL_ACCEL_DATA,        mAccelData.toString());
        cv.put(SwipeStudyDatabase.COL_GYRO_DATA,         mGyroData.toString());
        cv.put(SwipeStudyDatabase.COL_MAG_DATA,          mMagData.toString());
        cv.put(SwipeStudyDatabase.COL_SESSION_TIMESTAMP, strokeStartTime);
        cv.put(SwipeStudyDatabase.COL_PARTICIPANT_ID,
                getSharedPreferences("kd_prefs", MODE_PRIVATE)
                        .getString("participant_id", ""));

        db.insert(SwipeStudyDatabase.TABLE_NAME, null, cv);

        strokeIndex++;
        updateStrokeCount();
    }

    private String classifyDirection(float x0, float y0, float x1, float y1) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? "right" : "left";
        } else {
            return dy > 0 ? "down" : "up";
        }
    }

    private void updateStrokeCount() {
        tvStrokeCount.setText("Strokes recorded: " + strokeIndex);
    }
}
