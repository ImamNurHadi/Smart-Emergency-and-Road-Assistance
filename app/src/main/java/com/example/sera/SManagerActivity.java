package com.example.sera;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class SManagerActivity extends AppCompatActivity {

    private ListView sensorListView;
    private SensorManager sensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smanager);

        // Initialize the SensorManager and ListView
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorListView = findViewById(R.id.sensorListView);

        // Get the list of all available sensors
        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
        List<String> sensorNames = new ArrayList<>();

        for (Sensor sensor : sensorList) {
            sensorNames.add(sensor.getName() + " (" + sensor.getType() + ")");
        }

        // Set up the ListView with sensor names
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sensorNames);
        sensorListView.setAdapter(adapter);
    }
}
