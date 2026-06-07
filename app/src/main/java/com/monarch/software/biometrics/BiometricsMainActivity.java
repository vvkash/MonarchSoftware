package com.monarch.software.biometrics;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View.OnClickListener;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.monarch.software.R;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class BiometricsMainActivity extends Activity implements SensorEventListener, OnClickListener {

    private Button mWriteButton, mStopButton, mShareButton;
    private boolean doWrite = false;
    private boolean updateModel = true;
    private SensorManager sm;
    private TextView AT, AT2, AT3, AT4, AT5, AT6, AT7, AT8, ACT, AT9;
    private float AccValuex, AccValuey, AccValuez;
    private float GraValuex, GraValuey, GraValuez;
    private float GyrValuex;
    private float GyrValuey;
    private float GyrValuez;
    private float OriValuex, OriValuey, OriValuez;
    private float MagValuex, MagValuey, MagValuez;
    private float LAcValuex, LAcValuey, LAcValuez;
    private float LightValue;
    private float StepSum;
    private float StepFlag;
    private Spinner mSpinner;
    private long startMilisSeconds = 0;
    private long stopMilisSeconds = 0;

    int pos;
    String Lable = new String();
    String GyrTime = new String();
    String[] DataList = new String[6];
    String CurrentTime = new String();

    String ApplicationScenario = null;

    String sensorDataName;
    String subject;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bio_activity_main);
        AT9 = (TextView) findViewById(R.id.AT9);
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        Intent woc = getIntent();
        Bundle nb = woc.getExtras();
        ApplicationScenario = nb.getString("5:");
        sensorDataName = nb.getString("1:") + "_" + nb.getString("2:") + "_" + nb.getString("3:") + "_" + nb.getString("4:") + "_" + nb.getString("5:") + ".csv";
        subject = nb.getString("1:");
        try {
            FileOutputStream fout = openFileOutput(sensorDataName, Context.MODE_APPEND);
            OutputStreamWriter osw = new OutputStreamWriter(fout, "utf-8");
            String dataFrameHeader = "TimeStamp, Acc_x, Acc_y, Acc_z, Gyr_x, Gyr_y, Gyr_z, Mag_x, Mag_y, Mag_z, Application Scenario, Subject\n";
            osw.write(dataFrameHeader);
            osw.flush();
            fout.flush();
            osw.close();
            fout.close();
        } catch (Exception e) {
            Toast.makeText(BiometricsMainActivity.this,
                    "Store data failed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        mWriteButton = (Button) findViewById(R.id.Button_Write);
        mWriteButton.setOnClickListener(this);
        mStopButton = (Button) findViewById(R.id.Button_Stop);
        mStopButton.setOnClickListener(this);
        mShareButton = (Button) findViewById(R.id.Button_Share);
        mShareButton.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        sm.registerListener(this,
                sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
        sm.registerListener(this,
                sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this,
                sm.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this,
                sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this,
                sm.getDefaultSensor(Sensor.TYPE_GRAVITY),
                SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this,
                sm.getDefaultSensor(Sensor.TYPE_LIGHT),
                SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this,
                sm.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this,
                sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this,
                sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR), SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this,
                sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onStop() {
        super.onStop();
    }

    public void onPause() {
        super.onPause();
    }

    public void writeFileSdcard(String fileName, String message) {
        try {
            FileOutputStream fout = openFileOutput(fileName, Context.MODE_APPEND);
            OutputStreamWriter osw = new OutputStreamWriter(fout, "utf-8");
            osw.write(message);
            osw.flush();
            fout.flush();
            osw.close();
            fout.close();
        } catch (Exception e) {
            Toast.makeText(BiometricsMainActivity.this,
                    "Store data failed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void onClick(View v) {
        if (v.getId() == R.id.Button_Write) {
            doWrite = true;
            startMilisSeconds = System.currentTimeMillis();
            startService(new Intent(this, BiometricsSensorService.class));
            Toast.makeText(BiometricsMainActivity.this,
                    "Starting to store data", Toast.LENGTH_SHORT).show();
        }
        if (v.getId() == R.id.Button_Stop) {
            doWrite = false;
            stopService(new Intent(this, BiometricsSensorService.class));
            Toast.makeText(BiometricsMainActivity.this,
                    "Stop", Toast.LENGTH_SHORT).show();
        }
        if (v.getId() == R.id.Button_Share) {
            shareCSV();
        }
    }

    private void shareCSV() {
        File file = new File(getFilesDir(), sensorDataName);
        if (!file.exists()) {
            Toast.makeText(this, "No data file found. Start and stop recording first.", Toast.LENGTH_LONG).show();
            return;
        }
        android.net.Uri uri = FileProvider.getUriForFile(
                this,
                "com.monarch.software.fileprovider",
                file);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/csv");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.putExtra(Intent.EXTRA_SUBJECT, "Behavioral Biometrics Data - " + sensorDataName);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, "Export CSV via..."));
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        PackageManager pm = getPackageManager();
        ResolveInfo homeInfo =
                pm.resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            ActivityInfo ai = homeInfo.activityInfo;
            Intent startIntent = new Intent(Intent.ACTION_MAIN);
            startIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            startIntent.setComponent(new ComponentName(ai.packageName, ai.name));
            startActivitySafely(startIntent);
            return true;
        } else
            return super.onKeyDown(keyCode, event);
    }

    private void startActivitySafely(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "null",
                    Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(this, "null",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void delay1() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

            }
        }, 3000);
    }


    public void onSensorChanged(SensorEvent event) {
        String ShowMsg = new String();
        String WriteMsg = new String();

        final DecimalFormat df = new DecimalFormat("#,##0.0");
        final DecimalFormat msgDf = new DecimalFormat("#,##0.000000");
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");

        int sensorType = event.sensor.getType();
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                AccValuex = event.values[0];
                AccValuey = event.values[1];
                AccValuez = event.values[2];
                break;
            case Sensor.TYPE_GYROSCOPE:
                GyrValuex = event.values[0];
                GyrValuey = event.values[1];
                GyrValuez = event.values[2];
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                MagValuex = event.values[0];
                MagValuey = event.values[1];
                MagValuez = event.values[2];
                break;
        }

        long totalMilisSeconds = System.currentTimeMillis();
        CurrentTime = String.valueOf(totalMilisSeconds);
        ShowMsg = "Timestamp (ms):  " + CurrentTime + "\n\nSensor Type:      X-axis Y-axis  Z-axis" + "\n\nAccelerometer:  " + "  " + df.format(AccValuex) + "    " + df.format(AccValuey) + "    " + df.format(AccValuez) + "\n\nGyroscope:         " + "   " + df.format(GyrValuex) + "    " + df.format(GyrValuey) + "    " + df.format(GyrValuez) + "\n\nMagnetometer:  " + "   " + df.format(MagValuex) + "    " + df.format(MagValuey) + "    " + df.format(MagValuez);
        AT9.setText(ShowMsg);

        if (doWrite) {
            WriteMsg = CurrentTime + "," + msgDf.format(AccValuex) + "," + msgDf.format(AccValuey) + "," + msgDf.format(AccValuez) + "," + msgDf.format(GyrValuex) + "," + msgDf.format(GyrValuey) + "," + msgDf.format(GyrValuez) + "," + msgDf.format(MagValuex) + "," + msgDf.format(MagValuey) + "," + msgDf.format(MagValuez) + "," + ApplicationScenario + "," + subject + "\n";
            System.out.println("Real time behavioral biometric data:" + WriteMsg);

            final String FinalWriteMessage = WriteMsg;
            writeFileSdcard(sensorDataName, FinalWriteMessage);
        }
        delay1();
    }
}
