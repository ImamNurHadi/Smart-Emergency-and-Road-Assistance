package com.example.sera;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private EditText etName, etEmail, etPassword;
    private Button btnRegister;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Firebase Initialization
        mAuth = FirebaseAuth.getInstance();

        // UI Initialization
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnRegister = findViewById(R.id.btn_register);

        // Set button click listener to register user
        btnRegister.setOnClickListener(v -> registerUser());
    }

    // Method to register the user
    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate input fields
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Firebase Authentication: Create User
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();

                        // Call method to save user data to Realtime Database
                        if (firebaseUser != null) {
                            saveUserToDatabase(firebaseUser, name, email);
                        }
                    } else {
                        // If sign-in fails, display a message to the user
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(RegisterActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Method to save user data to Firebase Realtime Database
    private void saveUserToDatabase(FirebaseUser firebaseUser, String name, String email) {
        String userId = firebaseUser.getUid(); // Get UID of the current user

        // Firebase Realtime Database Reference
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Create a user object to save to the database
        User user = new User(name, email, "user"); // Default role as "user"

        // Save user data under the user's UID
        usersRef.child(userId).setValue(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "User registered successfully", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "User data saved to database: " + userId);

                        // Redirect to LoginActivity
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish(); // Close this activity
                    } else {
                        Toast.makeText(RegisterActivity.this, "Failed to save user data: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Database error: ", task.getException());
                    }
                });
    }
}
