package com.monarch.software.keystroke;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class SwipeResultsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ks_swipe_results);

        TextView tvTotalStrokes = findViewById(R.id.tv_total_strokes);
        TextView tvAvgDuration  = findViewById(R.id.tv_avg_duration);
        TextView tvAvgPoints    = findViewById(R.id.tv_avg_points);
        ListView listView       = findViewById(R.id.lv_swipe_results);

        List<StrokeRow> rows = loadResults();

        long totalDuration = 0;
        long totalPoints   = 0;
        for (StrokeRow r : rows) {
            totalDuration += r.durationMs;
            totalPoints   += r.pointCount;
        }
        int count = rows.size();
        tvTotalStrokes.setText(String.valueOf(count));
        if (count > 0) {
            tvAvgDuration.setText(String.valueOf(totalDuration / count));
            tvAvgPoints.setText(String.valueOf(totalPoints / count));
        } else {
            tvAvgDuration.setText("0");
            tvAvgPoints.setText("0");
        }

        listView.setAdapter(new StrokeAdapter(this, rows));
    }

    // -----------------------------------------------------------------------
    // Data loading
    // -----------------------------------------------------------------------

    private List<StrokeRow> loadResults() {
        List<StrokeRow> rows = new ArrayList<>();
        DBHelper helper = new DBHelper(this, Database.KEYSTROKE_DYNAMICS, null, Database.VERSION);
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.rawQuery(
            "SELECT " +
            SwipeStudyDatabase.COL_STROKE_INDEX + ", " +
            SwipeStudyDatabase.COL_DIRECTION + ", " +
            SwipeStudyDatabase.COL_DURATION_MS + ", " +
            SwipeStudyDatabase.COL_TOUCH_EVENTS + ", " +
            SwipeStudyDatabase.COL_ACCEL_DATA + ", " +
            SwipeStudyDatabase.COL_SESSION_TIMESTAMP +
            " FROM " + SwipeStudyDatabase.TABLE_NAME +
            " ORDER BY " + SwipeStudyDatabase.COL_ID + " ASC",
            null);

        while (c.moveToNext()) {
            StrokeRow row = new StrokeRow();
            row.strokeIndex      = c.getInt(0);
            row.direction        = c.getString(1);
            row.durationMs       = c.getLong(2);
            row.sessionTimestamp = c.getLong(5);

            // Count touch points from JSON
            String touchJson = c.getString(3);
            String accelJson = c.getString(4);
            row.pointCount = countJsonArray(touchJson);
            row.hasImu     = countJsonArray(accelJson) > 0;

            rows.add(row);
        }
        c.close();
        return rows;
    }

    private int countJsonArray(String json) {
        if (json == null || json.equals("[]")) return 0;
        try { return new JSONArray(json).length(); } catch (Exception e) { return 0; }
    }

    // -----------------------------------------------------------------------
    // Data model
    // -----------------------------------------------------------------------

    static class StrokeRow {
        int     strokeIndex;
        String  direction;
        long    durationMs;
        int     pointCount;
        boolean hasImu;
        long    sessionTimestamp;
    }

    // -----------------------------------------------------------------------
    // List adapter
    // -----------------------------------------------------------------------

    static class StrokeAdapter extends ArrayAdapter<StrokeRow> {

        StrokeAdapter(Context ctx, List<StrokeRow> rows) {
            super(ctx, 0, rows);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.ks_item_swipe_result, parent, false);
            }

            StrokeRow row = getItem(position);
            if (row == null) return convertView;

            TextView tvLabel     = convertView.findViewById(R.id.tv_stroke_label);
            TextView tvDirection = convertView.findViewById(R.id.tv_direction_badge);
            TextView tvDuration  = convertView.findViewById(R.id.tv_duration);
            TextView tvPoints    = convertView.findViewById(R.id.tv_point_count);
            TextView tvHasImu   = convertView.findViewById(R.id.tv_has_imu);

            tvLabel.setText("Stroke #" + (row.strokeIndex + 1));
            tvDirection.setText(row.direction != null ? row.direction.toUpperCase() : "?");
            tvDuration.setText(row.durationMs + " ms");
            tvPoints.setText(row.pointCount + " pts");
            tvHasImu.setText(row.hasImu ? "IMU: yes" : "IMU: no");

            return convertView;
        }
    }
}
