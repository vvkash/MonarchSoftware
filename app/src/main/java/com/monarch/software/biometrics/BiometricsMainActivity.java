package com.monarch.software.biometrics;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.monarch.software.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class BiometricsMainActivity extends AppCompatActivity
        implements SensorEventListener, View.OnClickListener {

    private Button writeButton;
    private Button stopButton;
    private Button shareButton;
    private TextView sensorPreview;
    private TextView recordingStatus;
    private TextView recordingDetail;
    private View recordingIndicator;
    private SensorManager sensorManager;
    private boolean isRecording;

    private float accelerometerX;
    private float accelerometerY;
    private float accelerometerZ;
    private float gyroscopeX;
    private float gyroscopeY;
    private float gyroscopeZ;
    private float magnetometerX;
    private float magnetometerY;
    private float magnetometerZ;

    private String applicationScenario;
    private String sensorDataName;
    private String subject;
    private final DecimalFormat previewFormat = new DecimalFormat(
            "0.00", DecimalFormatSymbols.getInstance(Locale.US));
    private final DecimalFormat csvFormat = new DecimalFormat(
            "0.000000", DecimalFormatSymbols.getInstance(Locale.US));

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bio_activity_main);

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Toast.makeText(this, "Session details are missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        applicationScenario = extras.getString("5:", "");
        subject = extras.getString("1:", "");
        sensorDataName = safeFileSegment(subject)
                + "_" + safeFileSegment(extras.getString("2:", ""))
                + "_" + safeFileSegment(extras.getString("3:", ""))
                + "_" + safeFileSegment(extras.getString("4:", ""))
                + "_" + safeFileSegment(applicationScenario)
                + ".csv";

        sensorPreview = findViewById(R.id.AT9);
        recordingStatus = findViewById(R.id.tv_recording_status);
        recordingDetail = findViewById(R.id.tv_recording_detail);
        recordingIndicator = findViewById(R.id.recording_indicator);
        writeButton = findViewById(R.id.Button_Write);
        stopButton = findViewById(R.id.Button_Stop);
        shareButton = findViewById(R.id.Button_Share);
        writeButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        shareButton.setOnClickListener(this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        ensureCsvHeader();
        updateRecordingState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerSensor(Sensor.TYPE_ACCELEROMETER, SensorManager.SENSOR_DELAY_UI);
        registerSensor(Sensor.TYPE_GYROSCOPE, SensorManager.SENSOR_DELAY_NORMAL);
        registerSensor(Sensor.TYPE_MAGNETIC_FIELD, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(this);
        if (isRecording) {
            stopRecording(false);
        }
        super.onPause();
    }

    private void registerSensor(int sensorType, int delay) {
        Sensor sensor = sensorManager.getDefaultSensor(sensorType);
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, delay);
        }
    }

    private void ensureCsvHeader() {
        File output = new File(getFilesDir(), sensorDataName);
        if (output.length() > 0) {
            return;
        }
        writeCsv("TimeStamp, Acc_x, Acc_y, Acc_z, Gyr_x, Gyr_y, Gyr_z, "
                + "Mag_x, Mag_y, Mag_z, Application Scenario, Subject\n");
    }

    private String safeFileSegment(String value) {
        String cleaned = value.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.isEmpty() ? "unknown" : cleaned;
    }

    private void writeCsv(String message) {
        try (FileOutputStream output = openFileOutput(sensorDataName, Context.MODE_APPEND);
             OutputStreamWriter writer = new OutputStreamWriter(output, "utf-8")) {
            writer.write(message);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to store sensor data.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.Button_Write) {
            startRecording();
        } else if (id == R.id.Button_Stop) {
            stopRecording(true);
        } else if (id == R.id.Button_Share) {
            if (isRecording) {
                stopRecording(false);
            }
            shareCsv();
        }
    }

    private void startRecording() {
        if (isRecording) {
            return;
        }
        isRecording = true;
        startService(new Intent(this, BiometricsSensorService.class));
        updateRecordingState();
        Toast.makeText(this, "Sensor recording started.", Toast.LENGTH_SHORT).show();
    }

    private void stopRecording(boolean announce) {
        if (!isRecording) {
            return;
        }
        isRecording = false;
        stopService(new Intent(this, BiometricsSensorService.class));
        updateRecordingState();
        if (announce) {
            Toast.makeText(this, "Recording saved to this session.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateRecordingState() {
        writeButton.setEnabled(!isRecording);
        stopButton.setEnabled(isRecording);
        recordingStatus.setText(isRecording ? "Recording in progress" : "Ready to record");
        recordingDetail.setText(isRecording
                ? "Motion samples are being appended to the session CSV."
                : "Sensor preview is active. No rows are being saved.");
        int indicatorColor = ContextCompat.getColor(
                this, isRecording ? R.color.error : R.color.success);
        recordingIndicator.setBackgroundTintList(ColorStateList.valueOf(indicatorColor));
    }

    private void shareCsv() {
        File file = new File(getFilesDir(), sensorDataName);
        if (!file.exists() || file.length() == 0) {
            Toast.makeText(this, "No session CSV is available yet.", Toast.LENGTH_LONG).show();
            return;
        }

        android.net.Uri uri = FileProvider.getUriForFile(
                this, "com.monarch.software.fileprovider", file);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/csv");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.putExtra(Intent.EXTRA_SUBJECT, "Behavioral Biometrics Data - " + sensorDataName);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, "Export session CSV"));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                accelerometerX = event.values[0];
                accelerometerY = event.values[1];
                accelerometerZ = event.values[2];
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyroscopeX = event.values[0];
                gyroscopeY = event.values[1];
                gyroscopeZ = event.values[2];
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magnetometerX = event.values[0];
                magnetometerY = event.values[1];
                magnetometerZ = event.values[2];
                break;
            default:
                return;
        }

        long timestamp = System.currentTimeMillis();
        sensorPreview.setText(
                "TIMESTAMP  " + timestamp
                        + "\n\nACCEL   " + axes(previewFormat, accelerometerX,
                        accelerometerY, accelerometerZ)
                        + "\nGYRO    " + axes(previewFormat, gyroscopeX,
                        gyroscopeY, gyroscopeZ)
                        + "\nMAG     " + axes(previewFormat, magnetometerX,
                        magnetometerY, magnetometerZ));

        if (isRecording) {
            writeCsv(timestamp + ","
                    + csvFormat.format(accelerometerX) + ","
                    + csvFormat.format(accelerometerY) + ","
                    + csvFormat.format(accelerometerZ) + ","
                    + csvFormat.format(gyroscopeX) + ","
                    + csvFormat.format(gyroscopeY) + ","
                    + csvFormat.format(gyroscopeZ) + ","
                    + csvFormat.format(magnetometerX) + ","
                    + csvFormat.format(magnetometerY) + ","
                    + csvFormat.format(magnetometerZ) + ","
                    + csvValue(applicationScenario) + ","
                    + csvValue(subject) + "\n");
        }
    }

    private String csvValue(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String axes(DecimalFormat format, float x, float y, float z) {
        return "X " + format.format(x)
                + "   Y " + format.format(y)
                + "   Z " + format.format(z);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
