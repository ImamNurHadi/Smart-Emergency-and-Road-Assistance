package com.example.sera;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText etEmail, etPassword;
    private Button btnLogin, tvLogin, tvRegister;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Firebase Authentication instance
        mAuth = FirebaseAuth.getInstance();

        // UI Initialization
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btnlogin);
        tvRegister = findViewById(R.id.tvregister);
        tvLogin = findViewById(R.id.tvlogin);

        // Set button click listener for login
        btnLogin.setOnClickListener(v -> loginUser());

        // Set click listener for registering a new account
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent); // Go to Register Activity
        });

        // Set click listener for registering a new account
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, LoginActivity.class);
            startActivity(intent); // Go to Register Activity
        });
    }

    // Method to login user
    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate input fields
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Firebase Authentication: Sign in user with email and password
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, get the current user
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    } else {
                        // If sign-in fails, display a message to the user
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                });
    }

    // Update UI based on whether login is successful or not
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            // If user is logged in, navigate to another activity (e.g., HomeActivity)
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Close the login activity
        } else {
            // If login failed, keep the user on the login screen
            Toast.makeText(LoginActivity.this, "Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}
