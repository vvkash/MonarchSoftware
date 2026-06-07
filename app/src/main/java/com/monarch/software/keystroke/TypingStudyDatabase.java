package com.monarch.software.keystroke;

/** Constants for the typing-study SQLite table. */
public class TypingStudyDatabase {

    public static final String TABLE_NAME = "typing_study_sessions";

    // Columns
    public static final String COL_ID                = "_id";
    public static final String COL_SENTENCE_ID       = "sentence_id";
    public static final String COL_REFERENCE_TEXT    = "reference_text";
    public static final String COL_TYPED_TEXT        = "typed_text";
    /** JSON array of inter-key intervals in milliseconds. */
    public static final String COL_IKI_DATA          = "iki_data";
    /** JSON array of press durations in milliseconds. */
    public static final String COL_PRESS_DATA        = "press_data";
    /** JSON array of full per-key event objects {t_down, t_up, press_ms, iki_ms}. */
    public static final String COL_KEY_EVENTS        = "key_events";
    public static final String COL_TOTAL_TIME_MS     = "total_time_ms";
    public static final String COL_WPM               = "wpm";
    public static final String COL_ERROR_RATE        = "error_rate";
    public static final String COL_SESSION_TIMESTAMP = "session_timestamp";
    /** JSON array of accelerometer readings: [{t, x, y, z}, …] */
    public static final String COL_ACCEL_DATA        = "accel_data";
    /** JSON array of gyroscope readings: [{t, x, y, z}, …] */
    public static final String COL_GYRO_DATA         = "gyro_data";
    /** JSON array of magnetometer readings: [{t, x, y, z}, …] */
    public static final String COL_MAG_DATA          = "mag_data";
    /** Participant identifier set by the researcher (e.g. "P01"). */
    public static final String COL_PARTICIPANT_ID    = "participant_id";
    /** JSON array of digraph intervals (ms): t_down[n+1] - t_down[n] for each consecutive key pair. */
    public static final String COL_DIGRAPH_DATA      = "digraph_data";
    /** JSON array of trigraph intervals (ms): t_down[n+2] - t_down[n] for each consecutive key triple. */
    public static final String COL_TRIGRAPH_DATA     = "trigraph_data";

    public static final String CREATE_SQL =
        "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
        COL_ID                + " INTEGER PRIMARY KEY AUTOINCREMENT," +
        COL_SENTENCE_ID       + " INTEGER," +
        COL_REFERENCE_TEXT    + " TEXT," +
        COL_TYPED_TEXT        + " TEXT," +
        COL_IKI_DATA          + " TEXT," +
        COL_PRESS_DATA        + " TEXT," +
        COL_KEY_EVENTS        + " TEXT," +
        COL_TOTAL_TIME_MS     + " INTEGER," +
        COL_WPM               + " REAL," +
        COL_ERROR_RATE        + " REAL," +
        COL_SESSION_TIMESTAMP + " INTEGER," +
        COL_ACCEL_DATA        + " TEXT," +
        COL_GYRO_DATA         + " TEXT," +
        COL_MAG_DATA          + " TEXT," +
        COL_PARTICIPANT_ID    + " TEXT," +
        COL_DIGRAPH_DATA      + " TEXT," +
        COL_TRIGRAPH_DATA     + " TEXT" +
        ");";
}
