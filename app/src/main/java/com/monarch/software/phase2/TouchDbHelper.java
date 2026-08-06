package com.monarch.software.phase2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite helper for recording raw touchstroke and semantic engagement events.
 *
 * Each row represents one MotionEvent (DOWN / MOVE / UP) with:
 *  - screen coordinates (x, y and raw x, y)
 *  - finger pressure and contact size
 *  - timestamp
 *  - swipe velocity (populated only on ACTION_UP)
 */
public class TouchDbHelper extends SQLiteOpenHelper {

    public static final String DB_NAME   = "phase2_touch.db";
    public static final int    DB_VERSION = 3;
    public static final String TABLE      = "touch_events";

    // Feed engagement events table
    public static final String FEED_TABLE              = "feed_events";
    public static final String COL_FEED_EVENT_TYPE     = "event_type";     // POST_ENTER | POST_VIEW | LIKE | UNLIKE | DOUBLE_TAP | ...
    public static final String COL_FEED_POST_ID        = "post_id";
    public static final String COL_FEED_USERNAME       = "username";
    public static final String COL_FEED_SCROLL_POS     = "scroll_pos";
    public static final String COL_FEED_VIEW_DURATION  = "view_duration_ms";
    public static final String COL_FEED_SCREEN         = "screen_name";
    public static final String COL_FEED_TARGET         = "target";
    public static final String COL_FEED_DETAILS        = "details";
    public static final String COL_FEED_VISIBLE_PERCENT = "visible_percent";

    // Column names
    public static final String COL_ID                = "_id";
    public static final String COL_TASK_TYPE         = "task_type";       // "social" | "gallery"
    public static final String COL_TRIAL_NUM         = "trial_num";
    public static final String COL_GESTURE_ID        = "gesture_id";      // increments each finger-down
    public static final String COL_EVENT_TYPE        = "event_type";      // DOWN | MOVE | UP
    public static final String COL_X                 = "x";               // local coords (px)
    public static final String COL_Y                 = "y";
    public static final String COL_RAW_X             = "raw_x";           // screen coords (px)
    public static final String COL_RAW_Y             = "raw_y";
    public static final String COL_PRESSURE          = "pressure";        // 0.0 – 1.0
    public static final String COL_TOUCH_SIZE        = "touch_size";      // contact area
    public static final String COL_TIMESTAMP_MS      = "timestamp_ms";    // epoch ms
    public static final String COL_VELOCITY_X        = "velocity_x";      // px/s (UP only)
    public static final String COL_VELOCITY_Y        = "velocity_y";      // px/s (UP only)
    public static final String COL_SESSION_TIMESTAMP = "session_timestamp";

    private static final String CREATE_FEED_SQL =
        "CREATE TABLE " + FEED_TABLE + " (" +
        "_id"                       + " INTEGER PRIMARY KEY AUTOINCREMENT," +
        COL_FEED_EVENT_TYPE         + " TEXT,"    +
        COL_FEED_POST_ID            + " INTEGER," +
        COL_FEED_USERNAME           + " TEXT,"    +
        COL_FEED_SCROLL_POS         + " INTEGER," +
        COL_FEED_VIEW_DURATION      + " INTEGER," +
        COL_FEED_SCREEN             + " TEXT,"    +
        COL_FEED_TARGET             + " TEXT,"    +
        COL_FEED_DETAILS            + " TEXT,"    +
        COL_FEED_VISIBLE_PERCENT    + " REAL,"    +
        COL_TIMESTAMP_MS            + " INTEGER," +
        COL_SESSION_TIMESTAMP       + " INTEGER"  +
        ");";

    private static final String CREATE_SQL =
        "CREATE TABLE " + TABLE + " (" +
        COL_ID                + " INTEGER PRIMARY KEY AUTOINCREMENT," +
        COL_TASK_TYPE         + " TEXT,"    +
        COL_TRIAL_NUM         + " INTEGER," +
        COL_GESTURE_ID        + " INTEGER," +
        COL_EVENT_TYPE        + " TEXT,"    +
        COL_X                 + " REAL,"    +
        COL_Y                 + " REAL,"    +
        COL_RAW_X             + " REAL,"    +
        COL_RAW_Y             + " REAL,"    +
        COL_PRESSURE          + " REAL,"    +
        COL_TOUCH_SIZE        + " REAL,"    +
        COL_TIMESTAMP_MS      + " INTEGER," +
        COL_VELOCITY_X        + " REAL,"    +
        COL_VELOCITY_Y        + " REAL,"    +
        COL_SESSION_TIMESTAMP + " INTEGER"  +
        ");";

    public TouchDbHelper(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_SQL);
        db.execSQL(CREATE_FEED_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(CREATE_FEED_SQL);
        } else if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + FEED_TABLE + " ADD COLUMN " + COL_FEED_SCREEN + " TEXT");
            db.execSQL("ALTER TABLE " + FEED_TABLE + " ADD COLUMN " + COL_FEED_TARGET + " TEXT");
            db.execSQL("ALTER TABLE " + FEED_TABLE + " ADD COLUMN " + COL_FEED_DETAILS + " TEXT");
            db.execSQL("ALTER TABLE " + FEED_TABLE + " ADD COLUMN " + COL_FEED_VISIBLE_PERCENT + " REAL");
        }
    }
}
