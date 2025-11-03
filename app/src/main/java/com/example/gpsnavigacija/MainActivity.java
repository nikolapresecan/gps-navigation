package com.example.gpsnavigacija;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private static final String TAG = "MainActivity";
    private TextView textView;
    private Button btnKarta, btnLogout;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        auth = FirebaseAuth.getInstance();
//
//        btnLogout = findViewById(R.id.btnLogout);
        textView = findViewById(R.id.textView);
        btnKarta = findViewById(R.id.btnKarta);

        btnKarta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });

//        btnLogout.setOnClickListener(v -> logoutUser());

        mDatabase = FirebaseDatabase.getInstance().getReference();

        ReadData();
    }

    private void ReadData() {
        DatabaseReference usersRef = mDatabase.child("korisnici");
        ValueEventListener userListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                StringBuilder userBuilder = new StringBuilder();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    Korisnik korisnik = userSnapshot.getValue(Korisnik.class);
                    if (korisnik != null) {
                        userBuilder.append(korisnik.toString());
                    }
                }
                textView.setText(userBuilder);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        };
        usersRef.addValueEventListener(userListener);
    }

//    private void logoutUser() {
//        auth.signOut();
//
//        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
//        startActivity(intent);
//        finish();
//    }
}