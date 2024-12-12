package com.example.sera;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private EditText editTextName, editTextContact, editTextMessage, editTextHistory;
    private Button buttonSave;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase Auth and Database Reference
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        // Initialize UI components
        editTextName = findViewById(R.id.editTextName);
        editTextContact = findViewById(R.id.editTextContact);
        editTextMessage = findViewById(R.id.editTextMessage);
        editTextHistory = findViewById(R.id.editTextHistory);
        buttonSave = findViewById(R.id.buttonSave);

        buttonSave.setOnClickListener(v -> checkAndSaveData());
    }

    private void checkAndSaveData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        DatabaseReference userRef = databaseReference.child(userId);

        // Retrieve existing data from Firebase
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String existingName = snapshot.child("name").getValue(String.class);
                String existingContact = snapshot.child("contact").getValue(String.class);
                String existingHistory = snapshot.child("history").getValue(String.class);

                // Get user input
                String name = editTextName.getText().toString().trim();
                String contact = editTextContact.getText().toString().trim();
                String message = editTextMessage.getText().toString().trim();
                String history = editTextHistory.getText().toString().trim();

                // Check if all fields are empty
                boolean isAllEmpty = TextUtils.isEmpty(name) && TextUtils.isEmpty(contact)
                        && TextUtils.isEmpty(history) && TextUtils.isEmpty(message);

                if (isAllEmpty && (existingName == null && existingContact == null && existingHistory == null)) {
                    Toast.makeText(ProfileActivity.this, "Please fill in at least one field", Toast.LENGTH_SHORT).show();
                    return;
                }

                // If some fields are empty, show a confirmation dialog
                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(contact) || TextUtils.isEmpty(history)) {
                    showConfirmationDialog(userRef, name, contact, history, message);
                } else {
                    saveData(userRef, name, contact, history, message);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Error accessing database: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showConfirmationDialog(DatabaseReference userRef, String name, String contact, String history, String message) {
        new AlertDialog.Builder(this)
                .setTitle("Incomplete Data")
                .setMessage("Some fields are not filled. Do you want to save anyway?")
                .setPositiveButton("Yes", (dialog, which) -> saveData(userRef, name, contact, history, message))
                .setNegativeButton("No", null)
                .show();
    }

    private void saveData(DatabaseReference userRef, String name, String contact, String history, String message) {
        // Update profile fields
        if (!TextUtils.isEmpty(name)) userRef.child("name").setValue(name);
        if (!TextUtils.isEmpty(contact)) userRef.child("contact").setValue(contact);
        if (!TextUtils.isEmpty(history)) userRef.child("history").setValue(history);

        // Save message if provided
        if (!TextUtils.isEmpty(message)) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
            userRef.child("messages").child(timestamp).child("message").setValue(message);
        }

        Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show();

        // Move to MainActivity
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Clear stack if needed
        startActivity(intent);
        finish(); // Close ProfileActivity
    }
}
