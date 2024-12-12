package com.example.sera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
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
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    // Views for battery temperature, gyroscope, direction, and barometer
    private TextView temperatureText;
    private TextView textX, textY, textZ;
    private TextView directionText;
    private TextView barometerText;
    private TextView shakeConditionText;

    // Vibrator instance
    private Vibrator vibrator;

    //Audio
    private MediaPlayer mediaPlayer;
    private TextView soundStatusText;
    private boolean isListening = true;
    private static final int SAMPLE_RATE = 44100; // Sample rate (Hz)
    private static final int THRESHOLD = 50000;  // Amplitudo ambang batas

    //Shaking
    private static final float SHAKE_THRESHOLD = 2500f; // Threshold untuk mendeteksi guncangan
    private long lastUpdate = 0; // Untuk mencatat waktu update terakhir
    private float lastX, lastY, lastZ; // Untuk menyimpan nilai sumbu terakhir


    // Sensors and SensorManager
    private SensorManager sensorManager;
    private CameraManager cameraManager;
    private Sensor gyroscope, magneticSensor, accelerometerSensor, barometer,lightSensor;

    private String cameraId;
    private boolean isFlashOn = false;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Location currentLocation;

    // Sensor data arrays for orientation calculation
    private float[] gravityValues = new float[3];
    private float[] magneticValues = new float[3];

    private final String PHONE_NUMBER = "";
    private final String HELP_MESSAGE = "";

    private Button logoutButton;
    private DatabaseReference databaseReference;


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

        ImageView imageViewMap = findViewById(R.id.imageMap);
        imageViewMap.setOnClickListener(v -> {
            // Logika buka peta
            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
            startActivity(intent);
        });

        ImageView imageViewSensorManager = findViewById(R.id.imageSensorManager);
        imageViewSensorManager.setOnClickListener(v -> {
            // Logika buka peta
            Intent intent = new Intent(MainActivity.this, SManagerActivity.class);
            startActivity(intent);
        });

        // Tombol navigasi ke Profile Activity
        ImageView profileButton = findViewById(R.id.profile);
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        shakeConditionText = findViewById(R.id.shakeConditionText);
        soundStatusText = findViewById(R.id.soundStatusText);

        // Light Sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        // Camera Manager
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        // Toggle Flash Button
        Button toggleFlashButton = findViewById(R.id.buttonToggleFlash);
        toggleFlashButton.setOnClickListener(v -> toggleFlash());

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
        if (lightSensor != null) {
            sensorManager.registerListener(lightEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (accelerometerSensor != null) {
            sensorManager.registerListener(shakeListener, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister all sensor listeners
        sensorManager.unregisterListener(gyroListener);
        sensorManager.unregisterListener(sensorEventListener);
        sensorManager.unregisterListener(barometerListener);
        sensorManager.unregisterListener(shakeListener);
        if (lightSensor != null) {
            sensorManager.unregisterListener(lightEventListener);
        }
        if (accelerometerSensor != null) {
            sensorManager.registerListener(shakeListener, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
        }
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


    private void toggleFlash() {
        try {
            isFlashOn = !isFlashOn;
            cameraManager.setTorchMode(cameraId, isFlashOn);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
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

    private final SensorEventListener shakeListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                long currentTime = System.currentTimeMillis();

                // Periksa apakah ada waktu yang cukup sejak pembaruan terakhir
                if ((currentTime - lastUpdate) > 100) { // Setiap 100ms
                    long diffTime = currentTime - lastUpdate;
                    lastUpdate = currentTime;

                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    // Hitung perubahan akselerasi
                    float deltaX = x - lastX;
                    float deltaY = y - lastY;
                    float deltaZ = z - lastZ;

                    // Hitung kekuatan guncangan
                    double shake = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) / diffTime * 10000;

                    // Simpan nilai sumbu saat ini untuk referensi berikutnya
                    lastX = x;
                    lastY = y;
                    lastZ = z;

                    // Periksa apakah kekuatan guncangan melebihi ambang batas
                    if (shake > SHAKE_THRESHOLD) {
                        onShakeDetected(); // Panggil fungsi saat guncangan terdeteksi
                    }
                    else {
                        shakeConditionText.setText("Shaking Status: Normal");
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Tidak digunakan
        }
    };

    private void onShakeDetected() {
        Toast.makeText(this, "Guncangan terdeteksi!", Toast.LENGTH_SHORT).show();
        shakeConditionText.setText("Shaking Status: Guncangan terdeteksi");
        // Tambahkan aksi setelah guncangan, misalnya mengirim SMS darurat
        sendHelpSms();
    }

    private final SensorEventListener lightEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float lightValue = event.values[0]; // Nilai cahaya dalam lux
            TextView lightText = findViewById(R.id.lightText);
            lightText.setText("Light Level: " + lightValue + " lux");

            if (lightValue < 3) { // Threshold untuk kondisi gelap
                try {
                    if (!isFlashOn) {
                        cameraManager.setTorchMode(cameraId, true);
                        isFlashOn = true;
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
            else {
                try {
                    if (isFlashOn) {
                        cameraManager.setTorchMode(cameraId, false);
                        isFlashOn = false;
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Tidak digunakan
        }
    };


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
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            // Referensi ke database
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // Ambil phoneNumber dari database
                    String phoneNumber = snapshot.child("contact").getValue(String.class);

                    if (phoneNumber != null && !phoneNumber.isEmpty()) {
                        // Pesan lokasi
                        String locationMessage = "Help, I'm at (" + latitude + ", " + longitude + "). I found a love!";

                        try {
                            // Kirim SMS
                            SmsManager smsManager = SmsManager.getDefault();
                            smsManager.sendTextMessage(phoneNumber, null, locationMessage, null, null);
                            Toast.makeText(MainActivity.this, "Help message sent to " + phoneNumber, Toast.LENGTH_SHORT).show();

                            // Simpan data ke Firebase di node "accidents"
                            saveAccidentData(userId, latitude, longitude);

                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "No contact found for this user.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(MainActivity.this, "Failed to fetch user data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Location not available. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    // Fungsi untuk menyimpan data kecelakaan ke Firebase
    private void saveAccidentData(String userId, double latitude, double longitude) {
        DatabaseReference accidentsRef = FirebaseDatabase.getInstance().getReference("accidents");
        String accidentId = accidentsRef.push().getKey(); // Generate unique key for accident

        if (accidentId != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String name = snapshot.child("name").getValue(String.class);
                    String history = snapshot.child("history").getValue(String.class);

                    // Buat data kecelakaan
                    HashMap<String, Object> accidentData = new HashMap<>();
                    accidentData.put("userId", userId);
                    accidentData.put("name", name != null ? name : "Unknown");
                    accidentData.put("history", history != null ? history : "No history");
                    accidentData.put("location", new HashMap<String, Object>() {{
                        put("latitude", latitude);
                        put("longitude", longitude);
                    }});
                    accidentData.put("timestamp", System.currentTimeMillis());

                    // Simpan ke "accidents"
                    accidentsRef.child(accidentId).setValue(accidentData)
                            .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Accident info saved.", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Failed to save accident info: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(MainActivity.this, "Failed to fetch user data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

}
