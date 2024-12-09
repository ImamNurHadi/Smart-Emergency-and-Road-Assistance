package com.example.sera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.media.MediaPlayer;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.view.View;


import android.telephony.SmsManager;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    // Views for battery temperature, gyroscope, direction, and barometer
    private TextView temperatureText;
    private TextView textX, textY, textZ;
    private TextView directionText;
    private TextView barometerText;
    // Vibrator instance
    private Vibrator vibrator;

    //Audio
    private MediaPlayer mediaPlayer;
    private TextView soundStatusText;
    private boolean isListening = true;
    private static final int SAMPLE_RATE = 44100; // Sample rate (Hz)
    private static final int THRESHOLD = 50000;  // Amplitudo ambang batas

    //Shaking

    // Sensors and SensorManager
    private SensorManager sensorManager;
    private Sensor gyroscope, magneticSensor, accelerometerSensor, barometer;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Location currentLocation;

    // Sensor data arrays for orientation calculation
    private float[] gravityValues = new float[3];
    private float[] magneticValues = new float[3];

    private final String PHONE_NUMBER = "089688276157";
    private final String HELP_MESSAGE = "";

    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        requestPermissions();
        // Initialize views
        temperatureText = findViewById(R.id.temperatureText);
        textX = findViewById(R.id.textX);
        textY = findViewById(R.id.textY);
        textZ = findViewById(R.id.textZ);
        directionText = findViewById(R.id.directionText);
        barometerText = findViewById(R.id.barometerText);

        // Request SMS permissions
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PackageManager.PERMISSION_GRANTED);

        // Initialize Vibrator
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Initialize MediaPlayer with the sound file
        mediaPlayer = MediaPlayer.create(this, R.raw.cina);

        // Register battery temperature receiver
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        // Setup the SensorManager and required sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        barometer = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        // Initialize FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Request location updates
        requestLocationUpdates();

        // Button to send SMS
        Button sendSmsButton = findViewById(R.id.buttonSendTest);
        sendSmsButton.setOnClickListener(view -> sendSmsIfPermissionGranted());

        Button buttonOpenMap = findViewById(R.id.buttonOpenMap);
        buttonOpenMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(intent);
            }
        });

        Button buttonSensorManager = findViewById(R.id.buttonSensorManager);
        buttonSensorManager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an Intent to start SManagerActivity
                Intent intent = new Intent(MainActivity.this, SManagerActivity.class);
                startActivity(intent);
            }
        });

        soundStatusText = findViewById(R.id.soundStatusText);

        // Inisialisasi tombol logout
        logoutButton = findViewById(R.id.buttonLogout);

        // Set onClickListener untuk tombol logout
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Logout dari Firebase Authentication
                FirebaseAuth.getInstance().signOut();

                // Arahkan kembali ke halaman login
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish(); // Tutup MainActivity
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Jika tidak ada user yang login, arahkan ke LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Menghentikan MainActivity agar tidak kembali ke halaman ini
        }
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

    private void requestPermissions() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        } else {
            startListening();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Release MediaPlayer resources
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

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

            if (celsiusTemperature > 45) {
                if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                }
            }
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

    private void startListening() {
        int bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        AudioRecord audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Toast.makeText(this, "AudioRecord Initialization Failed!", Toast.LENGTH_SHORT).show();
            return;
        }

        audioRecord.startRecording();
        isListening = true;

        new Thread(() -> {
            short[] buffer = new short[bufferSize];
            while (isListening) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                if (read > 0) {
                    int amplitude = calculateAmplitude(buffer, read);
                    runOnUiThread(() -> soundStatusText.setText("Listening... Amplitude: " + amplitude));

                    if (amplitude > THRESHOLD) {
                        runOnUiThread(this::sendHelpSms);
                    }
                }
                try {
                    Thread.sleep(500); // Perbarui setiap 500ms
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            audioRecord.stop();
            audioRecord.release();
        }).start();
    }

    private int calculateAmplitude(short[] buffer, int read) {
        int maxAmplitude = 0;
        for (int i = 0; i < read; i++) {
            maxAmplitude = Math.max(maxAmplitude, Math.abs(buffer[i]));
        }
        return maxAmplitude;
    }

    private void requestLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000) // 10 seconds
                .setFastestInterval(5000); // 5 seconds

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    currentLocation = locationResult.getLastLocation();
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void sendSmsIfPermissionGranted() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            sendHelpSms();
        } else {
            Toast.makeText(this, "SMS permission not granted!", Toast.LENGTH_SHORT).show();
        }
    }

    public void sendTestSms(    View view) {
        sendSmsIfPermissionGranted();
    }

    private void sendHelpSms() {
        if (currentLocation != null) {
            double latitude = currentLocation.getLatitude();
            double longitude = currentLocation.getLongitude();

            // Directly set the message with latitude and longitude
            String locationMessage = "Help, I'm at (" + latitude + ", " + longitude + "). I found a love!";

            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(PHONE_NUMBER, null, locationMessage, null, null);
                Toast.makeText(this, "Help message sent to " + PHONE_NUMBER, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Location not available. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}
