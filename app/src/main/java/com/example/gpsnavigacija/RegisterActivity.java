package com.example.gpsnavigacija;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private EditText etNewIme, etNewEmail, etNewPassword;
    private Button btnRegister;
    private FirebaseAuth auth;
    private DatabaseReference database;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        etNewIme = findViewById(R.id.etNewIme);
        etNewEmail = findViewById(R.id.etNewEmail);
        etNewPassword = findViewById(R.id.etNewPassword);
        btnRegister = findViewById(R.id.btnRegister);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("korisnici");

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String ime = etNewIme.getText().toString().trim();
        String email = etNewEmail.getText().toString().trim();
        String password = etNewPassword.getText().toString().trim();

        if (ime.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Molimo popunite sva polja", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().getSignInMethods().isEmpty()) {
                            Toast.makeText(this, "Račun s tim emailom već postoji", Toast.LENGTH_SHORT).show();
                        } else {
                            createUserAccount(email, password, ime);
                        }
                    } else {
                        Toast.makeText(this, "Pogreška pri provjeri emaila: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void createUserAccount(String email, String password, String ime) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = auth.getCurrentUser().getUid();
                        Korisnik korisnik = new Korisnik(ime, email);
                        database.child(userId).setValue(korisnik)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        Toast.makeText(this, "Registracija uspješna", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(this, "Pogreška u bazi: " + task1.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        Toast.makeText(this, "Registracija neuspješna: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}