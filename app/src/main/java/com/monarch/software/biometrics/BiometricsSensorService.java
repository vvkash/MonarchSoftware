package com.monarch.software.biometrics;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemClock;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Foreground service that records raw IMU streams in the HMOG on-disk layout.
 *
 * <p>Recording lives here rather than in the Activity so that a session survives the
 * screen being backgrounded, and so sensor callbacks can be serviced off the main thread.
 *
 * <p>Output mirrors the HMOG public dataset exactly, because the research pipeline
 * ({@code stage1_generalsemantic_HMOG.py}, {@code stage2_userMAE_HMOG.py},
 * {@code eval_hmae_hmog.py}) reads it with
 * {@code pd.read_csv(path, header=None, usecols=[3, 4, 5])}:
 *
 * <pre>
 *   files/hmog/&lt;subject&gt;/&lt;subject&gt;_session_&lt;n&gt;/Accelerometer.csv
 *                                                  /Gyroscope.csv
 *                                                  /Magnetometer.csv
 * </pre>
 *
 * <p>Column order is headerless and fixed:
 * {@code systime_ms, event_time_ns, activity_code, x, y, z, phone_orientation}
 * so that indices 3/4/5 are x/y/z.
 */
public class BiometricsSensorService extends Service {

    public static final String ACTION_START = "com.monarch.software.biometrics.START";
    public static final String ACTION_STOP = "com.monarch.software.biometrics.STOP";

    public static final String EXTRA_SUBJECT = "subject";
    public static final String EXTRA_ACTIVITY_CODE = "activity_code";
    public static final String EXTRA_AGE = "age";
    public static final String EXTRA_GENDER = "gender";
    public static final String EXTRA_EMAIL = "email";
    public static final String EXTRA_SCENARIO = "scenario";

    private static final String METADATA_FILE = "session_metadata.csv";
    private static final String METADATA_HEADER =
            "session,subject,age,gender,email,scenario,activity_code,started_at_ms";

    /**
     * HMOG is published at 100 Hz and both HMAE checkpoints were trained on 100 Hz
     * windows, so this requests an explicit period instead of a SENSOR_DELAY_* hint.
     */
    public static final int SAMPLING_PERIOD_US = 10_000;

    private static final int SENSOR_BUFFER_US = 0;
    private static final int WRITE_BUFFER_BYTES = 32 * 1024;
    private static final int FLUSH_EVERY_ROWS = 200;

    static final String CHANNEL_ID = "sensor_collection";
    static final int NOTIF_ID = 1;

    private final IBinder binder = new LocalBinder();

    private SensorManager sensorManager;
    private HandlerThread sensorThread;
    private Handler sensorHandler;

    private final List<Channel> channels = new ArrayList<>();
    private File sessionDir;
    private String subject = "unknown";
    private int activityCode;
    private String age = "";
    private String gender = "";
    private String email = "";
    private String scenario = "";

    private volatile boolean recording;
    private volatile int phoneOrientation;
    private volatile long recordingStartedAtMs;

    /** Wall-clock milliseconds corresponding to elapsed-realtime zero (device boot). */
    private volatile long bootWallClockMs;

    private final float[] latestAccelerometer = new float[3];
    private final float[] latestGyroscope = new float[3];
    private final float[] latestMagnetometer = new float[3];

    public class LocalBinder extends Binder {
        public BiometricsSensorService getService() {
            return BiometricsSensorService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        refreshOrientation();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();

        if (ACTION_STOP.equals(action)) {
            stopRecording();
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_START.equals(action)) {
            subject = sanitize(intent.getStringExtra(EXTRA_SUBJECT));
            activityCode = intent.getIntExtra(EXTRA_ACTIVITY_CODE, 0);
            age = nullToEmpty(intent.getStringExtra(EXTRA_AGE));
            gender = nullToEmpty(intent.getStringExtra(EXTRA_GENDER));
            email = nullToEmpty(intent.getStringExtra(EXTRA_EMAIL));
            scenario = nullToEmpty(intent.getStringExtra(EXTRA_SCENARIO));
            startForegroundNotification();
            startRecording();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        refreshOrientation();
    }

    @Override
    public void onDestroy() {
        stopRecording();
        super.onDestroy();
    }

    // ------------------------------------------------------------------
    // Recording lifecycle
    // ------------------------------------------------------------------

    private synchronized void startRecording() {
        if (recording) {
            return;
        }

        sessionDir = nextSessionDir(subject);
        if (sessionDir == null) {
            // Leave no orphaned "recording" notification behind: the bound Activity
            // keeps this service alive, so onDestroy() would not run on its own.
            stopRecording();
            stopSelf();
            return;
        }

        // Anchor once for converting per-event elapsed-realtime stamps to wall clock,
        // instead of sampling the system clock on every callback.
        bootWallClockMs = System.currentTimeMillis() - SystemClock.elapsedRealtime();

        // Written here, not from the UI, so every session directory is guaranteed a
        // matching demographics row even if the Activity is never resumed again.
        writeSessionMetadata(sessionDir.getName());

        sensorThread = new HandlerThread("imu-recorder", Process.THREAD_PRIORITY_FOREGROUND);
        sensorThread.start();
        sensorHandler = new Handler(sensorThread.getLooper());

        channels.clear();
        openChannel(Sensor.TYPE_ACCELEROMETER, "Accelerometer.csv", latestAccelerometer);
        openChannel(Sensor.TYPE_GYROSCOPE, "Gyroscope.csv", latestGyroscope);
        openChannel(Sensor.TYPE_MAGNETIC_FIELD, "Magnetometer.csv", latestMagnetometer);

        if (channels.isEmpty()) {
            stopRecording();
            stopSelf();
            return;
        }

        recordingStartedAtMs = SystemClock.elapsedRealtime();
        recording = true;
    }

    private void openChannel(int sensorType, String fileName, float[] latestSink) {
        Sensor sensor = sensorManager == null ? null : sensorManager.getDefaultSensor(sensorType);
        if (sensor == null) {
            return;
        }

        BufferedWriter writer;
        try {
            writer = new BufferedWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(new File(sessionDir, fileName), true), "UTF-8"),
                    WRITE_BUFFER_BYTES);
        } catch (IOException e) {
            return;
        }

        Channel channel = new Channel(sensor, writer, latestSink);
        channels.add(channel);
        sensorManager.registerListener(
                channel, sensor, SAMPLING_PERIOD_US, SENSOR_BUFFER_US, sensorHandler);
    }

    private synchronized void stopRecording() {
        recording = false;

        for (Channel channel : channels) {
            if (sensorManager != null) {
                sensorManager.unregisterListener(channel);
            }
            channel.close();
        }
        channels.clear();

        if (sensorThread != null) {
            sensorThread.quitSafely();
            sensorThread = null;
            sensorHandler = null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(Service.STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
    }

    // ------------------------------------------------------------------
    // Per-sensor channel
    // ------------------------------------------------------------------

    private final class Channel implements SensorEventListener {
        private final Sensor sensor;
        private final BufferedWriter writer;
        private final float[] latestSink;
        private final StringBuilder row = new StringBuilder(96);

        private volatile long samples;
        private int sinceFlush;
        private boolean closed;

        Channel(Sensor sensor, BufferedWriter writer, float[] latestSink) {
            this.sensor = sensor;
            this.writer = writer;
            this.latestSink = latestSink;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            latestSink[0] = x;
            latestSink[1] = y;
            latestSink[2] = z;

            if (!recording) {
                return;
            }

            // HMOG column order: systime, event time, activity, x, y, z, orientation.
            row.setLength(0);
            row.append(bootWallClockMs + (event.timestamp / 1_000_000L)).append(',')
                    .append(event.timestamp).append(',')
                    .append(activityCode).append(',')
                    .append(x).append(',')
                    .append(y).append(',')
                    .append(z).append(',')
                    .append(phoneOrientation).append('\n');

            synchronized (this) {
                if (closed) {
                    return;
                }
                try {
                    writer.write(row.toString());
                    samples++;
                    if (++sinceFlush >= FLUSH_EVERY_ROWS) {
                        writer.flush();
                        sinceFlush = 0;
                    }
                } catch (IOException ignored) {
                    // Drop the sample rather than tear down an in-flight session.
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        long sampleCount() {
            return samples;
        }

        Sensor sensor() {
            return sensor;
        }

        synchronized void close() {
            if (closed) {
                return;
            }
            closed = true;
            try {
                writer.flush();
                writer.close();
            } catch (IOException ignored) {
                // Nothing actionable at teardown.
            }
        }
    }

    // ------------------------------------------------------------------
    // State exposed to the UI
    // ------------------------------------------------------------------

    public boolean isRecording() {
        return recording;
    }

    /**
     * Synchronously flushes and closes the session writers.
     *
     * <p>Callers that export immediately after stopping must use this rather than the
     * {@link #ACTION_STOP} intent, which is delivered on a later main-thread message and
     * would let the export race an unflushed {@link BufferedWriter}.
     */
    public void finishSession() {
        stopRecording();
    }

    public float[] latestAccelerometer() {
        return latestAccelerometer;
    }

    public float[] latestGyroscope() {
        return latestGyroscope;
    }

    public float[] latestMagnetometer() {
        return latestMagnetometer;
    }

    public File sessionDir() {
        return sessionDir;
    }

    public synchronized long accelerometerSamples() {
        for (Channel channel : channels) {
            if (channel.sensor().getType() == Sensor.TYPE_ACCELEROMETER) {
                return channel.sampleCount();
            }
        }
        return 0L;
    }

    /** Measured accelerometer rate in Hz, so 100 Hz can be verified in the field. */
    public float measuredRateHz() {
        long elapsed = SystemClock.elapsedRealtime() - recordingStartedAtMs;
        if (!recording || elapsed <= 0L) {
            return 0f;
        }
        return accelerometerSamples() * 1000f / elapsed;
    }

    /** CSV files written for the active or most recent session. */
    public List<File> sessionFiles() {
        List<File> files = new ArrayList<>();
        if (sessionDir == null) {
            return files;
        }
        File[] found = sessionDir.listFiles();
        if (found != null) {
            for (File file : found) {
                if (file.isFile() && file.length() > 0L) {
                    files.add(file);
                }
            }
        }
        return files;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Allocates {@code hmog/<subject>/<subject>_session_<n>} with the next free index. */
    private File nextSessionDir(String user) {
        File userDir = new File(new File(getFilesDir(), "hmog"), user);
        for (int index = 1; index < 10_000; index++) {
            File candidate = new File(userDir, user + "_session_" + index);
            if (!candidate.exists()) {
                return candidate.mkdirs() ? candidate : null;
            }
        }
        return null;
    }

    /**
     * Appends one demographics row per recorded session, keyed by the session directory
     * name so it joins cleanly to the HMOG-format output. Kept out of the sensor CSVs so
     * those stay byte-compatible with the research loader.
     */
    private void writeSessionMetadata(String sessionKey) {
        File metadata = new File(getFilesDir(), METADATA_FILE);
        boolean needsHeader = true;

        if (metadata.length() > 0L) {
            if (METADATA_HEADER.equals(firstLine(metadata))) {
                needsHeader = false;
            } else {
                // An older build wrote a different schema. Preserve that file rather
                // than appending rows that would not line up with its header.
                File legacy = new File(getFilesDir(),
                        "session_metadata_legacy_" + System.currentTimeMillis() + ".csv");
                if (!metadata.renameTo(legacy)) {
                    return;
                }
            }
        }

        String row = csv(sessionKey) + ',' + csv(subject) + ',' + csv(age) + ','
                + csv(gender) + ',' + csv(email) + ',' + csv(scenario) + ','
                + activityCode + ',' + System.currentTimeMillis() + "\n";

        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(metadata, true), "UTF-8")) {
            if (needsHeader) {
                writer.write(METADATA_HEADER);
                writer.write("\n");
            }
            writer.write(row);
        } catch (IOException ignored) {
            // A missing metadata row must not abort an otherwise valid recording.
        }
    }

    private static String firstLine(File file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            return reader.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    private static String csv(String value) {
        return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void refreshOrientation() {
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null && windowManager.getDefaultDisplay() != null) {
            phoneOrientation = windowManager.getDefaultDisplay().getRotation();
        }
    }

    private static String sanitize(String value) {
        return sanitizeSubject(value);
    }

    /**
     * Subject id normalisation, shared with the UI so that metadata keys always match
     * the {@code hmog/<subject>/} directory actually written.
     */
    public static String sanitizeSubject(String value) {
        if (value == null) {
            return "unknown";
        }
        String cleaned = value.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.isEmpty() ? "unknown" : cleaned;
    }

    private void startForegroundNotification() {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Biometrics Recording")
                .setContentText("Recording IMU data at 100 Hz for the active session")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIF_ID, notification);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sensor Collection",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Shows when a behavioral biometrics session is recording");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
}
