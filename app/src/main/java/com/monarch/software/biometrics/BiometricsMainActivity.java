package com.monarch.software.biometrics;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.monarch.software.R;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Session screen for the behavioural biometrics module.
 *
 * <p>All recording is delegated to {@link BiometricsSensorService} so a session keeps
 * running when the app is backgrounded. This Activity only renders a low-rate preview
 * and drives start/stop/share.
 */
public class BiometricsMainActivity extends AppCompatActivity
        implements SensorEventListener, View.OnClickListener {

    /** Preview refresh only. The recorded stream is sampled at 100 Hz in the service. */
    private static final int PREVIEW_PERIOD_US = 100_000;
    private static final long PREVIEW_REFRESH_MS = 200L;

    private Button writeButton;
    private Button stopButton;
    private Button shareButton;
    private TextView sensorPreview;
    private TextView recordingStatus;
    private TextView recordingDetail;
    private View recordingIndicator;

    private SensorManager sensorManager;
    private BiometricsSensorService recorder;
    private boolean bound;
    /**
     * Tracks that {@link #bindService} was accepted, independently of whether
     * {@link ServiceConnection#onServiceConnected} has fired yet. The binding must be
     * released even if the connection never connected, or was dropped.
     */
    private boolean bindRequested;

    private final float[] accelerometer = new float[3];
    private final float[] gyroscope = new float[3];
    private final float[] magnetometer = new float[3];

    private String applicationScenario;
    private String subject;
    private String age;
    private String gender;
    private String email;
    private int activityCode;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final DecimalFormat previewFormat = new DecimalFormat(
            "0.00", DecimalFormatSymbols.getInstance(Locale.US));

    private final Runnable previewTick = new Runnable() {
        @Override
        public void run() {
            renderPreview();
            uiHandler.postDelayed(this, PREVIEW_REFRESH_MS);
        }
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            recorder = ((BiometricsSensorService.LocalBinder) service).getService();
            bound = true;
            updateRecordingState();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // The connection stays registered; only unbindService() releases it.
            recorder = null;
            bound = false;
            updateRecordingState();
        }
    };

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

        subject = extras.getString("1:", "");
        age = extras.getString("2:", "");
        gender = extras.getString("3:", "");
        email = extras.getString("4:", "");
        applicationScenario = extras.getString("5:", "");
        activityCode = scenarioToCode(applicationScenario);

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

        updateRecordingState();    }

    @Override
    protected void onStart() {
        super.onStart();
        bindRequested = true;
        bindService(new Intent(this, BiometricsSensorService.class),
                connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerPreview(Sensor.TYPE_ACCELEROMETER);
        registerPreview(Sensor.TYPE_GYROSCOPE);
        registerPreview(Sensor.TYPE_MAGNETIC_FIELD);
        uiHandler.post(previewTick);
    }

    @Override
    protected void onPause() {
        uiHandler.removeCallbacks(previewTick);
        sensorManager.unregisterListener(this);
        // Recording deliberately continues in the foreground service.
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (bindRequested) {
            unbindService(connection);
            bindRequested = false;
            bound = false;
            recorder = null;
        }
        super.onStop();
    }

    private void registerPreview(int sensorType) {
        Sensor sensor = sensorManager.getDefaultSensor(sensorType);
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, PREVIEW_PERIOD_US);
        }
    }

    // ------------------------------------------------------------------
    // Controls
    // ------------------------------------------------------------------

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.Button_Write) {
            startRecording();
        } else if (id == R.id.Button_Stop) {
            stopRecording(true);
        } else if (id == R.id.Button_Share) {
            if (isRecording()) {
                stopRecording(false);
            }
            shareSession();
        }
    }

    private void startRecording() {
        if (isRecording()) {
            return;
        }
        Intent intent = new Intent(this, BiometricsSensorService.class)
                .setAction(BiometricsSensorService.ACTION_START)
                .putExtra(BiometricsSensorService.EXTRA_SUBJECT, subject)
                .putExtra(BiometricsSensorService.EXTRA_ACTIVITY_CODE, activityCode)
                .putExtra(BiometricsSensorService.EXTRA_AGE, age)
                .putExtra(BiometricsSensorService.EXTRA_GENDER, gender)
                .putExtra(BiometricsSensorService.EXTRA_EMAIL, email)
                .putExtra(BiometricsSensorService.EXTRA_SCENARIO, applicationScenario);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        // onStart() already holds a BIND_AUTO_CREATE binding; binding again here would
        // leak the ServiceConnection, since onStop() only unbinds once.
        updateRecordingState();
        Toast.makeText(this, "Recording started at 100 Hz.", Toast.LENGTH_SHORT).show();
    }

    private void stopRecording(boolean announce) {
        if (!isRecording()) {
            return;
        }
        // Flush and close synchronously through the binder first, so an immediately
        // following export sees complete files; the intent only tears the service down.
        recorder.finishSession();
        startService(new Intent(this, BiometricsSensorService.class)
                .setAction(BiometricsSensorService.ACTION_STOP));
        updateRecordingState();
        if (announce) {
            Toast.makeText(this, "Recording saved to this session.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isRecording() {
        return bound && recorder != null && recorder.isRecording();
    }

    // ------------------------------------------------------------------
    // Presentation
    // ------------------------------------------------------------------

    private void updateRecordingState() {
        boolean recording = isRecording();
        writeButton.setEnabled(!recording);
        stopButton.setEnabled(recording);
        recordingStatus.setText(recording ? "Recording in progress" : "Ready to record");
        recordingDetail.setText(recording
                ? "Writing HMOG-format CSVs at 100 Hz. Safe to leave this screen."
                : "Sensor preview is active. No rows are being saved.");
        int indicatorColor = ContextCompat.getColor(
                this, recording ? R.color.error : R.color.success);
        recordingIndicator.setBackgroundTintList(ColorStateList.valueOf(indicatorColor));
    }

    private void renderPreview() {
        StringBuilder text = new StringBuilder()
                .append("ACCEL   ").append(axes(accelerometer))
                .append("\nGYRO    ").append(axes(gyroscope))
                .append("\nMAG     ").append(axes(magnetometer));

        if (isRecording()) {
            text.append("\n\nSAMPLES ").append(recorder.accelerometerSamples())
                    .append("\nRATE    ")
                    .append(previewFormat.format(recorder.measuredRateHz()))
                    .append(" Hz (target 100)");
        }

        sensorPreview.setText(text.toString());
        updateRecordingState();
    }

    private String axes(float[] values) {
        return "X " + previewFormat.format(values[0])
                + "   Y " + previewFormat.format(values[1])
                + "   Z " + previewFormat.format(values[2]);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, accelerometer, 0, 3);
                break;
            case Sensor.TYPE_GYROSCOPE:
                System.arraycopy(event.values, 0, gyroscope, 0, 3);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, magnetometer, 0, 3);
                break;
            default:
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // ------------------------------------------------------------------
    // Export
    // ------------------------------------------------------------------

    private void shareSession() {
        List<File> files = recorder == null ? new ArrayList<File>() : recorder.sessionFiles();
        if (files.isEmpty()) {
            Toast.makeText(this, "No session data is available yet.", Toast.LENGTH_LONG).show();
            return;
        }

        ArrayList<Uri> uris = new ArrayList<>();
        for (File file : files) {
            uris.add(FileProvider.getUriForFile(
                    this, "com.monarch.software.fileprovider", file));
        }

        Intent share = new Intent(Intent.ACTION_SEND_MULTIPLE)
                .setType("text/csv")
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                .putExtra(Intent.EXTRA_SUBJECT, "Behavioral Biometrics Data - " + subject)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, "Export session CSVs"));
    }

    /** Stable numeric activity id written into the HMOG activity column. */
    private int scenarioToCode(String scenario) {
        List<String> scenarios = Arrays.asList(getResources().getStringArray(R.array.scenarios));
        int index = scenarios.indexOf(scenario);
        return index < 0 ? 0 : index + 1;
    }
}
