package com.monarch.software.keystroke;

/** Constants for the swipe-study SQLite table. One row = one complete stroke gesture. */
public class SwipeStudyDatabase {

    public static final String TABLE_NAME = "swipe_study_sessions";

    public static final String COL_ID                = "_id";
    /** Sequential index of this stroke within the session (0-based). */
    public static final String COL_STROKE_INDEX      = "stroke_index";
    /**
     * JSON array of touch-point objects recorded during this stroke:
     * [{t, x, y, pressure, area, action}, …]
     * x and y are normalised to [0,1] by screen resolution.
     * action: 0=DOWN, 2=MOVE, 1=UP
     */
    public static final String COL_TOUCH_EVENTS      = "touch_events";
    /** Total duration of the stroke from ACTION_DOWN to ACTION_UP (ms). */
    public static final String COL_DURATION_MS       = "duration_ms";
    /** Primary direction: "up", "down", "left", or "right". */
    public static final String COL_DIRECTION         = "direction";
    /** JSON array of accelerometer readings during this stroke: [{t, x, y, z}, …] */
    public static final String COL_ACCEL_DATA        = "accel_data";
    /** JSON array of gyroscope readings during this stroke: [{t, x, y, z}, …] */
    public static final String COL_GYRO_DATA         = "gyro_data";
    /** JSON array of magnetometer readings during this stroke: [{t, x, y, z}, …] */
    public static final String COL_MAG_DATA          = "mag_data";
    /** Unix timestamp (ms) when the stroke started. */
    public static final String COL_SESSION_TIMESTAMP = "session_timestamp";
    /** Participant identifier set by the researcher (e.g. "P01"). */
    public static final String COL_PARTICIPANT_ID    = "participant_id";

    public static final String CREATE_SQL =
        "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
        COL_ID                + " INTEGER PRIMARY KEY AUTOINCREMENT," +
        COL_STROKE_INDEX      + " INTEGER," +
        COL_TOUCH_EVENTS      + " TEXT," +
        COL_DURATION_MS       + " INTEGER," +
        COL_DIRECTION         + " TEXT," +
        COL_ACCEL_DATA        + " TEXT," +
        COL_GYRO_DATA         + " TEXT," +
        COL_MAG_DATA          + " TEXT," +
        COL_SESSION_TIMESTAMP + " INTEGER," +
        COL_PARTICIPANT_ID    + " TEXT" +
        ");";
}
