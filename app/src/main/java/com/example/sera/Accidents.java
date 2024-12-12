package com.example.sera;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

public class Accidents extends AppCompatActivity {

    private TextView accidentsData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accidents);

        accidentsData = findViewById(R.id.accidentsData);

        // Load accidents data
        loadAccidentsData();
    }

    private void loadAccidentsData() {
        DatabaseReference accidentsRef = FirebaseDatabase.getInstance().getReference("accidents");

        accidentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                StringBuilder data = new StringBuilder();

                for (DataSnapshot accidentSnapshot : snapshot.getChildren()) {
                    Accident accident = accidentSnapshot.getValue(Accident.class);

                    if (accident != null) {
                        Map<String, Object> location = accident.getLocation();

                        if (location != null) {
                            Object latitude = location.get("latitude");
                            Object longitude = location.get("longitude");

                            data.append("Name: ").append(accident.getName() != null ? accident.getName() : "Unknown").append("\n")
                                    .append("History: ").append(accident.getHistory() != null ? accident.getHistory() : "No history").append("\n")
                                    .append("Location: ")
                                    .append(latitude != null ? latitude.toString() : "Unknown")
                                    .append(", ")
                                    .append(longitude != null ? longitude.toString() : "Unknown").append("\n")
                                    .append("Timestamp: ").append(accident.getTimestamp()).append("\n\n");
                        }
                    }
                }

                // Update the TextView with accidents data
                if (data.length() > 0) {
                    accidentsData.setText(data.toString());
                } else {
                    accidentsData.setText("No accidents data found.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Accidents.this, "Failed to load accidents data: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
