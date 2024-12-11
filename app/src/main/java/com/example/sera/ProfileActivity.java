package com.example.sera;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ProfileActivity extends AppCompatActivity {

    private EditText editTextName, editTextContact, editTextMessage, editTextHistory;
    private Button buttonSave;
    private DatabaseReference databaseReference;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Inisialisasi Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser(); // Dapatkan pengguna saat ini
        if (currentUser == null) {
            Toast.makeText(this, "Pengguna tidak ditemukan. Harap login kembali.", Toast.LENGTH_SHORT).show();
            finish(); // Tutup activity jika pengguna tidak login
            return;
        }
        databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());

        // Inisialisasi View
        editTextName = findViewById(R.id.editTextName);
        editTextContact = findViewById(R.id.editTextContact);
        editTextMessage = findViewById(R.id.editTextMessage);
        editTextHistory = findViewById(R.id.editTextHistory);
        buttonSave = findViewById(R.id.buttonSave);

        // Set OnClickListener untuk tombol Simpan
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveUserData();
            }
        });
    }

    private void saveUserData() {
        String name = editTextName.getText().toString().trim();
        String contact = editTextContact.getText().toString().trim();
        String message = editTextMessage.getText().toString().trim();
        String history = editTextHistory.getText().toString().trim();

        if (name.isEmpty() || contact.isEmpty() || message.isEmpty() || history.isEmpty()) {
            Toast.makeText(this, "Semua field harus diisi!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Simpan atau update data di Firebase menggunakan UID pengguna saat ini
        UserProfile userProfile = new UserProfile(name, contact, message, history);
        databaseReference.setValue(userProfile)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ProfileActivity.this, "Data berhasil disimpan!", Toast.LENGTH_SHORT).show();
                        // Kembali ke MainActivity
                        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish(); // Tutup ProfileActivity
                    } else {
                        Toast.makeText(ProfileActivity.this, "Gagal menyimpan data!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Model untuk menyimpan data
    public static class UserProfile {
        public String name, contact, message, history;

        public UserProfile() {
        }

        public UserProfile(String name, String contact, String message, String history) {
            this.name = name;
            this.contact = contact;
            this.message = message;
            this.history = history;
        }
    }
}
