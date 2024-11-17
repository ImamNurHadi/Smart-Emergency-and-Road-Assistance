package com.example.sera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // Views for battery temperature, gyroscope, direction, and barometer
    private TextView temperatureText;
    private TextView textX, textY, textZ;
    private TextView directionText;
    private TextView barometerText;
    // Vibrator instance
    private Vibrator vibrator;

    // Sensors and SensorManager
    private SensorManager sensorManager;
    private Sensor gyroscope, magneticSensor, accelerometerSensor, barometer;

    // Sensor data arrays for orientation calculation
    private float[] gravityValues = new float[3];
    private float[] magneticValues = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        temperatureText = findViewById(R.id.temperatureText);
        textX = findViewById(R.id.textX);
        textY = findViewById(R.id.textY);
        textZ = findViewById(R.id.textZ);
        directionText = findViewById(R.id.directionText);
        barometerText = findViewById(R.id.barometerText);

        // Initialize Vibrator
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Register battery temperature receiver
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        // Setup the SensorManager and required sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        barometer = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register gyroscope sensor listener
        if (gyroscope != null) {
            sensorManager.registerListener(gyroListener, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        }
        // Register listeners for magnetic, accelerometer, and barometer sensors
        if (magneticSensor != null && accelerometerSensor != null) {
            sensorManager.registerListener(sensorEventListener, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (barometer != null) {
            sensorManager.registerListener(barometerListener, barometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister all sensor listeners
        sensorManager.unregisterListener(gyroListener);
        sensorManager.unregisterListener(sensorEventListener);
        sensorManager.unregisterListener(barometerListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister battery receiver
        unregisterReceiver(batteryReceiver);
    }

    // Battery temperature broadcast receiver
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            float celsiusTemperature = temperature / 10.0f; // Convert to Celsius
            temperatureText.setText("Battery Temperature: " + celsiusTemperature + "°C");
        }
    };

    // Gyroscope event listener
    private final SensorEventListener gyroListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not used in this example
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            textX.setText("X : " + x + " rad/s");
            textY.setText("Y : " + y + " rad/s");
            textZ.setText("Z : " + z + " rad/s");
        }
    };


    // SensorEventListener for magnetic and accelerometer sensors
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, gravityValues, 0, event.values.length);
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, magneticValues, 0, event.values.length);
            }

            // Calculate orientation if we have both accelerometer and magnetic sensor data
            if (gravityValues != null && magneticValues != null) {
                float[] rotationMatrix = new float[9];
                float[] orientationAngles = new float[3];

                SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, magneticValues);
                SensorManager.getOrientation(rotationMatrix, orientationAngles);

                // Get the azimuth angle (rotation around the Z-axis) in degrees
                float azimuth = (float) Math.toDegrees(orientationAngles[0]);
                azimuth = (azimuth + 360) % 360; // Normalize to 0-360 degrees

                // Determine direction based on azimuth
                if (azimuth >= 315 || azimuth < 45) {
                    directionText.setText("North");
                } else if (azimuth >= 45 && azimuth < 135) {
                    directionText.setText("East");
                } else if (azimuth >= 135 && azimuth < 225) {
                    directionText.setText("South");
                } else if (azimuth >= 225 && azimuth < 315) {
                    directionText.setText("West");

                    // Trigger vibration if direction is West
                    if (vibrator != null && vibrator.hasVibrator()) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(500); // Fallback for devices below API 26
                        }
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not used in this example
        }
    };

    // Barometer event listener
    private final SensorEventListener barometerListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float pressure = event.values[0]; // Pressure in hPa
            barometerText.setText("Pressure: " + pressure + " hPa");
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not used in this example
        }
    };
}
