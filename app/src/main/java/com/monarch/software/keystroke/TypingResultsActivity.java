package com.monarch.software.keystroke;

import com.monarch.software.R;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class TypingResultsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ks_typing_results);

        TextView tvAvgWpm      = findViewById(R.id.tv_avg_wpm);
        TextView tvAvgErrors   = findViewById(R.id.tv_avg_errors);
        TextView tvSentences   = findViewById(R.id.tv_sentences_done);
        ListView listView      = findViewById(R.id.lv_results);

        List<ResultRow> rows = loadResults();

        // Compute summary stats
        double totalWpm = 0, totalError = 0;
        for (ResultRow r : rows) {
            totalWpm   += r.wpm;
            totalError += r.errorRate;
        }
        int count = rows.size();
        if (count > 0) {
            tvAvgWpm.setText(String.format("%.0f", totalWpm / count));
            tvAvgErrors.setText(String.format("%.1f", totalError / count));
        } else {
            tvAvgWpm.setText("0");
            tvAvgErrors.setText("0");
        }
        tvSentences.setText(String.valueOf(count));

        listView.setAdapter(new ResultAdapter(this, rows));

        Button btnSwipeStudy = findViewById(R.id.btn_start_swipe_study);
        btnSwipeStudy.setOnClickListener(v ->
                startActivity(new Intent(this, SwipeStudyActivity.class)));

        Button btnExport = findViewById(R.id.btn_export);
        btnExport.setOnClickListener(v ->
                startActivity(new Intent(this, ExportActivity.class)));
    }

    // -----------------------------------------------------------------------
    // Data loading
    // -----------------------------------------------------------------------

    private List<ResultRow> loadResults() {
        List<ResultRow> rows = new ArrayList<>();
        DBHelper helper = new DBHelper(this, Database.KEYSTROKE_DYNAMICS, null, Database.VERSION);
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.rawQuery(
            "SELECT " +
            TypingStudyDatabase.COL_SENTENCE_ID + ", " +
            TypingStudyDatabase.COL_REFERENCE_TEXT + ", " +
            TypingStudyDatabase.COL_TYPED_TEXT + ", " +
            TypingStudyDatabase.COL_WPM + ", " +
            TypingStudyDatabase.COL_ERROR_RATE + ", " +
            TypingStudyDatabase.COL_TOTAL_TIME_MS + ", " +
            TypingStudyDatabase.COL_IKI_DATA +
            " FROM " + TypingStudyDatabase.TABLE_NAME +
            " ORDER BY " + TypingStudyDatabase.COL_ID + " ASC",
            null);

        while (c.moveToNext()) {
            ResultRow row = new ResultRow();
            row.sentenceId    = c.getInt(0);
            row.referenceText = c.getString(1);
            row.typedText     = c.getString(2);
            row.wpm           = c.getDouble(3);
            row.errorRate     = c.getDouble(4);
            row.totalTimeMs   = c.getLong(5);
            row.ikiData       = c.getString(6);
            rows.add(row);
        }
        c.close();
        return rows;
    }

    // -----------------------------------------------------------------------
    // Data model
    // -----------------------------------------------------------------------

    static class ResultRow {
        int    sentenceId;
        String referenceText;
        String typedText;
        double wpm;
        double errorRate;
        long   totalTimeMs;
        String ikiData;
    }

    // -----------------------------------------------------------------------
    // List adapter
    // -----------------------------------------------------------------------

    static class ResultAdapter extends ArrayAdapter<ResultRow> {

        ResultAdapter(Context ctx, List<ResultRow> rows) {
            super(ctx, 0, rows);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.ks_item_typing_result, parent, false);
            }

            ResultRow row = getItem(position);
            if (row == null) return convertView;

            TextView tvLabel      = convertView.findViewById(R.id.tv_sentence_label);
            TextView tvReference  = convertView.findViewById(R.id.tv_reference);
            TextView tvWpm        = convertView.findViewById(R.id.tv_wpm);
            TextView tvErrors     = convertView.findViewById(R.id.tv_errors);
            TextView tvTime       = convertView.findViewById(R.id.tv_time);
            TextView tvKeystrokes = convertView.findViewById(R.id.tv_keystrokes);

            tvLabel.setText("Sentence " + (row.sentenceId + 1));
            tvReference.setText("\"" + row.referenceText + "\"");
            tvWpm.setText(String.format("%.0f", row.wpm));
            tvErrors.setText(String.format("%.1f", row.errorRate));
            tvTime.setText(String.format("%.1f", row.totalTimeMs / 1000.0));

            // Count keystrokes from IKI array length (+1 for first key)
            int keyCount = 0;
            if (row.ikiData != null && !row.ikiData.equals("[]")) {
                try {
                    org.json.JSONArray arr = new org.json.JSONArray(row.ikiData);
                    keyCount = arr.length() + 1;
                } catch (Exception ignored) {}
            }
            tvKeystrokes.setText(String.valueOf(keyCount));

            return convertView;
        }
    }
}
