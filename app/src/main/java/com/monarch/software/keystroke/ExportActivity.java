package com.monarch.software.keystroke;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Exports typing and swipe study data as CSV files and shares them
 * via the system share sheet (email, Drive, AirDrop, etc.).
 */
public class ExportActivity extends AppCompatActivity {

    private static final String PREFS      = "kd_prefs";
    private static final String KEY_PID    = "participant_id";
    private static final String AUTHORITY  = "com.monarch.software.fileprovider";

    private SharedPreferences prefs;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ks_export);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        tvStatus = findViewById(R.id.tv_export_status);

        EditText etPid    = findViewById(R.id.et_participant_id);
        Button   btnSave  = findViewById(R.id.btn_save_pid);

        // Pre-fill with saved ID
        etPid.setText(prefs.getString(KEY_PID, ""));

        btnSave.setOnClickListener(v -> {
            String pid = etPid.getText().toString().trim();
            prefs.edit().putString(KEY_PID, pid).apply();
            Toast.makeText(this, "Participant ID saved: " + pid, Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_export_typing).setOnClickListener(v -> {
            File csv = buildTypingCsv();
            if (csv != null) shareFiles(new File[]{csv});
        });

        findViewById(R.id.btn_export_swipe).setOnClickListener(v -> {
            File csv = buildSwipeCsv();
            if (csv != null) shareFiles(new File[]{csv});
        });

        findViewById(R.id.btn_export_both).setOnClickListener(v -> {
            File t = buildTypingCsv();
            File s = buildSwipeCsv();
            if (t != null && s != null) shareFiles(new File[]{t, s});
            else if (t != null)         shareFiles(new File[]{t});
            else if (s != null)         shareFiles(new File[]{s});
        });
    }

    // -----------------------------------------------------------------------
    // CSV builders
    // -----------------------------------------------------------------------

    private File buildTypingCsv() {
        DBHelper helper = new DBHelper(this, Database.KEYSTROKE_DYNAMICS, null, Database.VERSION);
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.rawQuery("SELECT * FROM " + TypingStudyDatabase.TABLE_NAME +
                               " ORDER BY " + TypingStudyDatabase.COL_ID + " ASC", null);

        StringBuilder sb = new StringBuilder();
        // Header
        for (int i = 0; i < c.getColumnCount(); i++) {
            if (i > 0) sb.append(',');
            sb.append(csvEscape(c.getColumnName(i)));
        }
        sb.append('\n');
        // Rows
        while (c.moveToNext()) {
            for (int i = 0; i < c.getColumnCount(); i++) {
                if (i > 0) sb.append(',');
                sb.append(csvEscape(c.getString(i)));
            }
            sb.append('\n');
        }
        c.close();

        int rows = c.getCount(); // 0 after close but we can infer from length
        return writeToCache("typing_study.csv", sb.toString());
    }

    private File buildSwipeCsv() {
        DBHelper helper = new DBHelper(this, Database.KEYSTROKE_DYNAMICS, null, Database.VERSION);
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.rawQuery("SELECT * FROM " + SwipeStudyDatabase.TABLE_NAME +
                               " ORDER BY " + SwipeStudyDatabase.COL_ID + " ASC", null);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < c.getColumnCount(); i++) {
            if (i > 0) sb.append(',');
            sb.append(csvEscape(c.getColumnName(i)));
        }
        sb.append('\n');
        while (c.moveToNext()) {
            for (int i = 0; i < c.getColumnCount(); i++) {
                if (i > 0) sb.append(',');
                sb.append(csvEscape(c.getString(i)));
            }
            sb.append('\n');
        }
        c.close();

        return writeToCache("swipe_study.csv", sb.toString());
    }

    // -----------------------------------------------------------------------
    // File helpers
    // -----------------------------------------------------------------------

    private File writeToCache(String filename, String content) {
        File dir = new File(getCacheDir(), "exports");
        if (!dir.exists() && !dir.mkdirs()) {
            tvStatus.setText("Error: could not create export directory.");
            return null;
        }
        File file = new File(dir, filename);
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(content);
            tvStatus.setText("Ready: " + filename + " (" + content.split("\n").length + " rows)");
            return file;
        } catch (IOException e) {
            tvStatus.setText("Error writing " + filename + ": " + e.getMessage());
            return null;
        }
    }

    private void shareFiles(File[] files) {
        ArrayList<Uri> uris = new ArrayList<>();
        for (File f : files) {
            uris.add(FileProvider.getUriForFile(this, AUTHORITY, f));
        }

        Intent intent;
        if (uris.size() == 1) {
            intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        } else {
            intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        }
        intent.setType("text/csv");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Share keystroke data"));
    }

    // -----------------------------------------------------------------------
    // CSV escaping
    // -----------------------------------------------------------------------

    /** Wraps value in quotes and escapes internal quotes per RFC 4180. */
    private static String csvEscape(String value) {
        if (value == null) return "";
        // If value contains comma, newline, or quote — wrap in quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
