package com.monarch.software.phase2;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class SocialTelemetry {

    private static final String TAG = "SocialTelemetry";
    private static final AtomicInteger NEXT_GESTURE_ID =
            new AtomicInteger((int) (System.currentTimeMillis() & 0x3FFFFFFF));

    private final TouchDbHelper dbHelper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final String screenName;
    private final long sessionStart;

    private VelocityTracker velocityTracker;
    private int gestureId;
    private int moveEventCount;

    SocialTelemetry(Context context, String screenName, long sessionStart) {
        this.dbHelper = new TouchDbHelper(context.getApplicationContext());
        this.screenName = screenName;
        this.sessionStart = sessionStart;
    }

    long getSessionStart() {
        return sessionStart;
    }

    void log(String eventType, int postId, String username, int position,
             long durationMs, String target, String details) {
        log(eventType, postId, username, position, durationMs, target, details, null);
    }

    void log(String eventType, int postId, String username, int position,
             long durationMs, String target, String details, Float visiblePercent) {
        final long timestamp = System.currentTimeMillis();
        execute(() -> {
            ContentValues values = new ContentValues();
            values.put(TouchDbHelper.COL_FEED_EVENT_TYPE, eventType);
            if (postId >= 0) values.put(TouchDbHelper.COL_FEED_POST_ID, postId);
            if (username != null) values.put(TouchDbHelper.COL_FEED_USERNAME, username);
            values.put(TouchDbHelper.COL_FEED_SCROLL_POS, position);
            values.put(TouchDbHelper.COL_FEED_VIEW_DURATION, durationMs);
            values.put(TouchDbHelper.COL_FEED_SCREEN, screenName);
            if (target != null) values.put(TouchDbHelper.COL_FEED_TARGET, target);
            if (details != null) values.put(TouchDbHelper.COL_FEED_DETAILS, details);
            if (visiblePercent != null) {
                values.put(TouchDbHelper.COL_FEED_VISIBLE_PERCENT, visiblePercent);
            }
            values.put(TouchDbHelper.COL_TIMESTAMP_MS, timestamp);
            values.put(TouchDbHelper.COL_SESSION_TIMESTAMP, sessionStart);
            insert(TouchDbHelper.FEED_TABLE, values, eventType);
        });
    }

    void recordTouch(MotionEvent event, String taskType, int trialNumber) {
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                gestureId = NEXT_GESTURE_ID.incrementAndGet();
                moveEventCount = 0;
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain();
                } else {
                    velocityTracker.clear();
                }
                velocityTracker.addMovement(event);
                queueTouch(event, taskType, trialNumber, "DOWN", 0f, 0f);
                break;
            case MotionEvent.ACTION_MOVE:
                if (velocityTracker != null) velocityTracker.addMovement(event);
                moveEventCount++;
                if (moveEventCount % 3 == 0) {
                    queueTouch(event, taskType, trialNumber, "MOVE", 0f, 0f);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                float velocityX = 0f;
                float velocityY = 0f;
                if (velocityTracker != null) {
                    velocityTracker.addMovement(event);
                    velocityTracker.computeCurrentVelocity(1000);
                    velocityX = velocityTracker.getXVelocity();
                    velocityY = velocityTracker.getYVelocity();
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                queueTouch(
                        event,
                        taskType,
                        trialNumber,
                        action == MotionEvent.ACTION_UP ? "UP" : "CANCEL",
                        velocityX,
                        velocityY);
                break;
            default:
                break;
        }
    }

    private void queueTouch(MotionEvent event, String taskType, int trialNumber,
                            String eventType, float velocityX, float velocityY) {
        final float x = event.getX();
        final float y = event.getY();
        final float rawX = event.getRawX();
        final float rawY = event.getRawY();
        final float pressure = event.getPressure();
        final float size = event.getSize();
        final long timestamp = System.currentTimeMillis();
        final int currentGestureId = gestureId;

        execute(() -> {
            ContentValues values = new ContentValues();
            values.put(TouchDbHelper.COL_TASK_TYPE, taskType);
            values.put(TouchDbHelper.COL_TRIAL_NUM, trialNumber);
            values.put(TouchDbHelper.COL_GESTURE_ID, currentGestureId);
            values.put(TouchDbHelper.COL_EVENT_TYPE, eventType);
            values.put(TouchDbHelper.COL_X, x);
            values.put(TouchDbHelper.COL_Y, y);
            values.put(TouchDbHelper.COL_RAW_X, rawX);
            values.put(TouchDbHelper.COL_RAW_Y, rawY);
            values.put(TouchDbHelper.COL_PRESSURE, pressure);
            values.put(TouchDbHelper.COL_TOUCH_SIZE, size);
            values.put(TouchDbHelper.COL_TIMESTAMP_MS, timestamp);
            values.put(TouchDbHelper.COL_VELOCITY_X, velocityX);
            values.put(TouchDbHelper.COL_VELOCITY_Y, velocityY);
            values.put(TouchDbHelper.COL_SESSION_TIMESTAMP, sessionStart);
            insert(TouchDbHelper.TABLE, values, eventType);
        });
    }

    private void insert(String table, ContentValues values, String eventType) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            if (db.insert(table, null, values) == -1) {
                Log.e(TAG, "Failed to persist " + eventType + " in " + table);
            }
        } catch (SQLException error) {
            Log.e(TAG, "Database error while persisting " + eventType, error);
        }
    }

    private void execute(Runnable work) {
        if (closed.get()) {
            Log.w(TAG, "Ignoring telemetry after close on " + screenName);
            return;
        }
        try {
            executor.execute(work);
        } catch (RejectedExecutionException error) {
            Log.e(TAG, "Telemetry executor rejected work on " + screenName, error);
        }
    }

    void close() {
        if (!closed.compareAndSet(false, true)) return;
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
        try {
            executor.execute(dbHelper::close);
        } catch (RejectedExecutionException error) {
            Log.e(TAG, "Unable to queue database close", error);
            dbHelper.close();
        }
        executor.shutdown();
    }
}
