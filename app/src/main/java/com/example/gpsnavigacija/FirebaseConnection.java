package com.example.gpsnavigacija;

import android.content.Context;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseConnection {
    private final FirebaseAuth auth;
    private final DatabaseReference database;

    public FirebaseConnection() {
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public DatabaseReference getDatabaseReference() {
        return database;
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public void setNameInDrawer(Context context, TextView navHeaderName) {
        FirebaseUser currentUser = getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(context, "Korisnik nije prijavljen.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = currentUser.getUid();
        database.child("korisnici").child(currentUserId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Korisnik korisnik = task.getResult().getValue(Korisnik.class);
                        if (korisnik != null) {
                            navHeaderName.setText("Prijavljen/a: " + korisnik.getIme());
                        } else {
                            Toast.makeText(context, "Korisnik nije pronađen", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, "Pogreška pri dohvaćanju podataka", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void fetchUserFields(Context context, List<Polje> polja, String userId, final ValueEventListener listener) {
        DatabaseReference poljaRef = database.child("polja");
        Query query = poljaRef.orderByChild("korisnik_id").equalTo(userId);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                polja.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Polje polje = ds.getValue(Polje.class);
                    if (polje != null) {
                        polje.setId(ds.getKey());
                        polja.add(polje);
                    }
                }

                if (polja.isEmpty()) {
                    Toast.makeText(context, "Korisnik nema polja", Toast.LENGTH_SHORT).show();
                }

                listener.onDataChange(snapshot);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(context, "Greška pri dohvaćanju polja.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void deleteFields(String poljeId, final DeleteCallback callback) {
        Query query = database.child("aktivnosti").orderByChild("id_polja").equalTo(poljeId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();

                updates.put("polja/" + poljeId, null);

                for (DataSnapshot ds : snapshot.getChildren()) {
                    updates.put("aktivnosti/" + ds.getKey(), null);
                }

                database.updateChildren(updates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onFailure(error.getMessage());
            }
        });
    }

    public void fetchWorkType(ValueEventListener listener) {
        DatabaseReference ref = database.child("tipovi_obrade");
        ref.addListenerForSingleValueEvent(listener);
    }

    public void fetchBranchWidth(ValueEventListener listener) {
        DatabaseReference ref = database.child("sirine_grana");
        ref.addListenerForSingleValueEvent(listener);
    }

    public void fetchActivityForField(String poljeId, ValueEventListener listener) {
        Query query = database.child("aktivnosti").orderByChild("id_polja").equalTo(poljeId);
        query.addListenerForSingleValueEvent(listener);
    }

    public void fetchFieldPoints(String poljeId, ValueEventListener listener) {
        DatabaseReference ref = database.child("polja").child(poljeId).child("tocke");
        ref.addListenerForSingleValueEvent(listener);
    }

    public void newField(Polje polje, SaveCallback callback) {
        DatabaseReference poljaRef = database.child("polja");
        poljaRef.push().setValue(polje, (databaseError, databaseReference) -> {
            if (databaseError == null) {
                callback.onSuccess();
            } else {
                callback.onFailure(databaseError.getMessage());
            }
        });
    }

    public void updateField(String poljeId, String noviNaziv, double novaPovrsina, final UpdateCallback callback) {
        DatabaseReference poljeRef = database.child("polja").child(poljeId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("naziv", noviNaziv);
        updates.put("povrsina", novaPovrsina);

        poljeRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Greška");
            }
        });
    }

    public void deleteActivity(String activityId, final DeleteCallback callback) {
        DatabaseReference activityRef = database.child("aktivnosti").child(activityId);

        activityRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Greška pri brisanju");
            }
        });
    }

    public void addBranchWidth(SirinaGrana sirinaGrana, SaveCallback callback) {
        DatabaseReference ref = database.child("sirine_grana");
        ref.push().setValue(sirinaGrana, (error, refResult) -> {
            if (error == null) {
                callback.onSuccess();
            } else {
                callback.onFailure(error.getMessage());
            }
        });
    }

    public void newActivity(Aktivnost aktivnost, SaveCallback callback) {
        DatabaseReference aktivnostiRef = database.child("aktivnosti");
        aktivnostiRef.push().setValue(aktivnost, (databaseError, databaseReference) -> {
            if (databaseError == null) {
                callback.onSuccess();
            } else {
                callback.onFailure(databaseError.getMessage());
            }
        });
    }
}