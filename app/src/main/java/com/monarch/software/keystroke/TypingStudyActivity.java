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
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Typing study activity modelled on the Aalto Typing37k study
 * (https://userinterfaces.aalto.fi/typing37k/).
 *
 * Shows a reference sentence for the user to transcribe using the system
 * keyboard.  Records per-keystroke timing (inter-key interval and press
 * duration) plus IMU sensor readings (accelerometer, gyroscope, magnetometer)
 * and saves everything to SQLite for later analysis.
 */
public class TypingStudyActivity extends AppCompatActivity implements SensorEventListener {

    // -----------------------------------------------------------------------
    // Sentence pool – 20 everyday phrases representative of the Aalto study
    // -----------------------------------------------------------------------
    private static final String[] SENTENCES = {
        "She left him after he cheated",
        "The weather has been great lately",
        "My phone needs to be charged",
        "Can you please pass the salt",
        "He forgot his wallet at home",
        "The meeting was cancelled again",
        "I really enjoy going for walks",
        "She sent me a funny video",
        "We should go out for dinner",
        "The kids are doing really well",
        "I need to buy some groceries",
        "He got a new job last week",
        "The coffee here is really good",
        "She asked me to help her move",
        "We watched a movie last night",
        "I forgot to set my alarm",
        "The train was delayed by ten minutes",
        "She laughed at everything I said",
        "I need to call my mom today",
        "He was late to the meeting again"
    };

    // -----------------------------------------------------------------------
    // UI
    // -----------------------------------------------------------------------
    private TextView tvProgress, tvReference, tvWpm, tvErrors, tvTime;
    private LinearProgressIndicator sentenceProgress;
    private EditText etInput;
    private Button btnNext, btnDone;

    // -----------------------------------------------------------------------
    // Keystroke state
    // -----------------------------------------------------------------------
    private int currentIndex = 0;

    /** Timestamp of the first keypress in this sentence (ms). */
    private long sessionStartTime = 0;

    /** Timestamp recorded in beforeTextChanged – approximates key-down. */
    private long keyDownTime = 0;

    /** Timestamp recorded after the previous key was released. */
    private long lastKeyUpTime = 0;

    /** Whether the very next keystroke will be the first one for this sentence. */
    private boolean firstKey = true;

    // Touch snapshot taken at the moment of each key-down.
    // Updated via dispatchTouchEvent — reflects the last in-app touch since
    // soft-keyboard touches are consumed by the keyboard's own window.
    private float mLastTouchX, mLastTouchY, mLastTouchPressure, mLastTouchSize;
    private float keyDownTouchX, keyDownTouchY, keyDownTouchPressure, keyDownTouchSize;

    // Digraph: t_down[n+1] - t_down[n]
    // Trigraph: t_down[n+2] - t_down[n]
    private JSONArray digraphs, trigraphs;
    private long prevKeyDownTime, prevPrevKeyDownTime;

    /** JSON array of inter-key intervals (ms) for the current sentence. */
    private JSONArray ikis;

    /** JSON array of press durations (ms) for the current sentence. */
    private JSONArray pressDurations;

    /** JSON array of individual key events: {t_down, t_up, press_ms, iki_ms}. */
    private JSONArray keyEvents;

    // -----------------------------------------------------------------------
    // IMU state
    // -----------------------------------------------------------------------
    private SensorManager mSensorMgr;
    private Sensor mAccelerometer, mGyroscope, mMagnetometer;

    /** IMU readings accumulated for the current sentence. */
    private JSONArray mAccelData, mGyroData, mMagData;

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ks_typing_study);

        tvProgress    = findViewById(R.id.tv_progress);
        tvReference   = findViewById(R.id.tv_reference);
        tvWpm         = findViewById(R.id.tv_wpm);
        tvErrors      = findViewById(R.id.tv_errors);
        tvTime        = findViewById(R.id.tv_time);
        sentenceProgress = findViewById(R.id.progress_sentences);
        etInput       = findViewById(R.id.et_input);
        btnNext       = findViewById(R.id.btn_next);
        btnDone       = findViewById(R.id.btn_done);

        mSensorMgr    = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope     = mSensorMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mMagnetometer  = mSensorMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        loadSentence(currentIndex);

        etInput.addTextChangedListener(new TextWatcher() {

            // Called just BEFORE the text changes → approximate key-down
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                long now = System.currentTimeMillis();

                if (after > 0) {               // a character is being inserted
                    if (firstKey) {
                        sessionStartTime = now;
                        firstKey = false;
                    }
                    // IKI = time since the previous key was released
                    long iki = (lastKeyUpTime == 0) ? 0 : (now - lastKeyUpTime);
                    try { ikis.put(iki); } catch (Exception ignored) {}
                    keyDownTime          = now;
                    // Snapshot the last in-app touch at key-down time
                    keyDownTouchX        = mLastTouchX;
                    keyDownTouchY        = mLastTouchY;
                    keyDownTouchPressure = mLastTouchPressure;
                    keyDownTouchSize     = mLastTouchSize;
                    // Digraph: interval from previous key-down to this one
                    if (prevKeyDownTime > 0) {
                        try { digraphs.put(now - prevKeyDownTime); } catch (Exception ignored) {}
                    }
                    // Trigraph: interval from two key-downs ago to this one
                    if (prevPrevKeyDownTime > 0) {
                        try { trigraphs.put(now - prevPrevKeyDownTime); } catch (Exception ignored) {}
                    }
                    prevPrevKeyDownTime = prevKeyDownTime;
                    prevKeyDownTime     = now;
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            // Called just AFTER the text changes → approximate key-up
            @Override
            public void afterTextChanged(Editable s) {
                long now = System.currentTimeMillis();

                if (keyDownTime > 0) {
                    long pressDuration = now - keyDownTime;
                    try {
                        pressDurations.put(pressDuration);

                        // Full per-key event record
                        JSONObject event = new JSONObject();
                        event.put("t_down",     keyDownTime);
                        event.put("t_up",       now);
                        event.put("press_ms",   pressDuration);
                        event.put("iki_ms",     ikis.length() > 0
                                ? ikis.getLong(ikis.length() - 1) : 0);
                        event.put("x",          keyDownTouchX);
                        event.put("y",          keyDownTouchY);
                        event.put("pressure",   keyDownTouchPressure);
                        event.put("touch_size", keyDownTouchSize);
                        keyEvents.put(event);
                    } catch (Exception ignored) {}

                    lastKeyUpTime = now;
                    keyDownTime = 0;
                }

                String typed     = s.toString();
                String reference = SENTENCES[currentIndex];

                updateHighlight(typed, reference);
                updateStats(typed, reference);

                // Enable Next once the typed string is at least as long as reference
                btnNext.setEnabled(typed.length() >= reference.length());
            }
        });

        btnNext.setOnClickListener(v -> {
            saveSession();
            currentIndex++;
            if (currentIndex >= SENTENCES.length) {
                btnNext.setVisibility(View.GONE);
                btnDone.setVisibility(View.VISIBLE);
                Toast.makeText(this, "All sentences completed! Data saved.", Toast.LENGTH_LONG).show();
            } else {
                loadSentence(currentIndex);
            }
        });

        btnDone.setOnClickListener(v -> {
            Intent intent = new Intent(this, TypingResultsActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ~100 Hz (10 000 µs) matches the FETA/BehaveFormer sensor rate
        int delay = 10_000;
        if (mAccelerometer != null)
            mSensorMgr.registerListener(this, mAccelerometer, delay);
        if (mGyroscope != null)
            mSensorMgr.registerListener(this, mGyroscope, delay);
        if (mMagnetometer != null)
            mSensorMgr.registerListener(this, mMagnetometer, delay);
        startService(new Intent(this, KeystrokeSensorService.class));
    }

    @Override
    protected void onPause() {
        mSensorMgr.unregisterListener(this);
        stopService(new Intent(this, KeystrokeSensorService.class));
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSensorMgr.unregisterListener(this);
        stopService(new Intent(this, KeystrokeSensorService.class));
    }

    /**
     * Snapshot every ACTION_DOWN that lands in this activity's window so we
     * can associate spatial touch data with the nearest keystroke.
     *
     * NOTE: soft-keyboard touches are dispatched to the keyboard's own window
     * and will NOT appear here. The values captured reflect the last explicit
     * in-app touch (e.g. tapping the EditText to set focus).
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            mLastTouchX        = ev.getX();
            mLastTouchY        = ev.getY();
            mLastTouchPressure = ev.getPressure();
            mLastTouchSize     = ev.getSize();
        }
        return super.dispatchTouchEvent(ev);
    }

    // -----------------------------------------------------------------------
    // SensorEventListener
    // -----------------------------------------------------------------------

    @Override
    public void onSensorChanged(SensorEvent event) {
        JSONObject reading = new JSONObject();
        try {
            reading.put("t", event.timestamp / 1_000_000L); // ns → ms
            reading.put("x", event.values[0]);
            reading.put("y", event.values[1]);
            reading.put("z", event.values[2]);
        } catch (Exception ignored) { return; }

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                mAccelData.put(reading); break;
            case Sensor.TYPE_GYROSCOPE:
                mGyroData.put(reading);  break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mMagData.put(reading);   break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { /* no-op */ }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void loadSentence(int index) {
        tvProgress.setText((index + 1) + " / " + SENTENCES.length);
        sentenceProgress.setProgressCompat(index + 1, index > 0);
        tvReference.setText(SENTENCES[index]);
        etInput.setText("");
        etInput.setEnabled(true);
        etInput.requestFocus();
        btnNext.setEnabled(false);
        tvWpm.setText("-");
        tvErrors.setText("-");
        tvTime.setText("-");

        // Reset per-sentence keystroke state
        sessionStartTime = 0;
        keyDownTime      = 0;
        lastKeyUpTime    = 0;
        firstKey         = true;
        ikis                = new JSONArray();
        pressDurations      = new JSONArray();
        keyEvents           = new JSONArray();
        digraphs            = new JSONArray();
        trigraphs           = new JSONArray();
        prevKeyDownTime     = 0;
        prevPrevKeyDownTime = 0;

        // Reset per-sentence IMU buffers
        mAccelData = new JSONArray();
        mGyroData  = new JSONArray();
        mMagData   = new JSONArray();
    }

    /**
     * Color-code each character of the reference sentence:
     *  - green  = typed correctly
     *  - red bg = typed incorrectly
     *  - gray   = not yet typed
     */
    private void updateHighlight(String typed, String reference) {
        SpannableString span = new SpannableString(reference);
        int typedLen = typed.length();
        int refLen   = reference.length();
        int minLen   = Math.min(typedLen, refLen);

        for (int i = 0; i < refLen; i++) {
            if (i < minLen) {
                if (typed.charAt(i) == reference.charAt(i)) {
                    span.setSpan(new ForegroundColorSpan(
                                    ContextCompat.getColor(this, R.color.success)),
                            i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    span.setSpan(new BackgroundColorSpan(
                                    ContextCompat.getColor(this, R.color.error_bg)),
                            i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    span.setSpan(new ForegroundColorSpan(
                                    ContextCompat.getColor(this, R.color.error)),
                            i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else {
                span.setSpan(new ForegroundColorSpan(
                                ContextCompat.getColor(this, R.color.text_hint)),
                        i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        tvReference.setText(span);
    }

    private void updateStats(String typed, String reference) {
        if (sessionStartTime == 0 || typed.isEmpty()) return;

        long elapsedMs = System.currentTimeMillis() - sessionStartTime;

        double wpm = (typed.length() / 5.0) / (elapsedMs / 60000.0);

        int errors = 0;
        int minLen = Math.min(typed.length(), reference.length());
        for (int i = 0; i < minLen; i++) {
            if (typed.charAt(i) != reference.charAt(i)) errors++;
        }
        errors += Math.abs(typed.length() - reference.length());

        tvWpm.setText(String.format("%.0f", wpm));
        tvErrors.setText(String.valueOf(errors));
        tvTime.setText(String.format("%.1f", elapsedMs / 1000.0));
    }

    /** Persist the completed sentence data (keystrokes + IMU) to the database. */
    private void saveSession() {
        String reference = SENTENCES[currentIndex];
        String typed     = etInput.getText().toString();
        long totalTimeMs = (sessionStartTime > 0)
                ? System.currentTimeMillis() - sessionStartTime : 0;

        int errors = 0;
        int minLen = Math.min(typed.length(), reference.length());
        for (int i = 0; i < minLen; i++) {
            if (typed.charAt(i) != reference.charAt(i)) errors++;
        }
        errors += Math.abs(typed.length() - reference.length());
        double errorRate = (reference.length() > 0)
                ? (errors * 100.0 / reference.length()) : 0.0;
        double wpm = (totalTimeMs > 0)
                ? (typed.length() / 5.0) / (totalTimeMs / 60000.0) : 0.0;

        DBHelper dbHelper = new DBHelper(this, Database.KEYSTROKE_DYNAMICS, null, Database.VERSION);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("sentence_id",       currentIndex);
        cv.put("reference_text",    reference);
        cv.put("typed_text",        typed);
        cv.put("iki_data",          ikis.toString());
        cv.put("press_data",        pressDurations.toString());
        cv.put("key_events",        keyEvents.toString());
        cv.put("total_time_ms",     totalTimeMs);
        cv.put("wpm",               wpm);
        cv.put("error_rate",        errorRate);
        cv.put("session_timestamp", System.currentTimeMillis());
        cv.put("accel_data",        mAccelData.toString());
        cv.put("gyro_data",         mGyroData.toString());
        cv.put("mag_data",          mMagData.toString());
        cv.put("participant_id",    getSharedPreferences("kd_prefs", MODE_PRIVATE)
                                        .getString("participant_id", ""));
        cv.put("digraph_data",      digraphs.toString());
        cv.put("trigraph_data",     trigraphs.toString());

        db.insert(TypingStudyDatabase.TABLE_NAME, null, cv);
    }
}
