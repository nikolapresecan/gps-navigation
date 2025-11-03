package com.example.gpsnavigacija;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;


import java.util.ArrayList;
import java.util.List;

public class PoljeActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_NEW_FIELD = 1;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView navHeaderName;
    private SearchView searchView;
    private FloatingActionButton fabNewPolje;

    private Navigation navigation;

    private RecyclerView recyclerView;
    private PoljeAdapter adapter;
    private List<Polje> polja;
    private List<Polje> poljaFiltered;
    private FirebaseConnection firebaseConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_polje);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Polja");
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#0F5C10")));
        }

        drawerLayout = findViewById(R.id.my_drawer_layout);
        navHeaderName = findViewById(R.id.nav_header_name);
        navigationView = findViewById(R.id.navigation_view);
        searchView = findViewById(R.id.searchView);
        fabNewPolje = findViewById(R.id.fabNewPolje);

        navigation = new Navigation();
        navigation.setupNavigationDrawer(this, drawerLayout, navigationView, navHeaderName);

        recyclerView = findViewById(R.id.rvPolja);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        polja = new ArrayList<>();
        poljaFiltered = new ArrayList<>();

        adapter = new PoljeAdapter(poljaFiltered,
                polje -> {
                    Intent intent = new Intent(PoljeActivity.this, PoljeDetailsActivity.class);
                    intent.putExtra("polje", polje);
                    startActivity(intent);
                },
                (polje, position) -> {
                    new AlertDialog.Builder(PoljeActivity.this)
                            .setTitle("Brisanje polja")
                            .setMessage("Jeste li sigurni da želite obrisati " + polje.getNaziv() + " i sve njegove aktivnosti?")
                            .setPositiveButton("Da", (dialog, which) -> {
                                firebaseConnection.deleteFields(polje.getId(), new DeleteCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Toast.makeText(PoljeActivity.this, "Polje i njegove aktivnosti obrisani.", Toast.LENGTH_SHORT).show();
                                        polja.remove(position);
                                        adapter.notifyItemRemoved(position);
                                        filter("");
                                    }

                                    @Override
                                    public void onFailure(String errorMessage) {
                                        Toast.makeText(PoljeActivity.this, "Greška: " + errorMessage, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            })
                            .setNegativeButton("Ne", null)
                            .show();
        });
        recyclerView.setAdapter(adapter);

        firebaseConnection = new FirebaseConnection();

        String userId = firebaseConnection.getCurrentUser().getUid();

        firebaseConnection.fetchUserFields(this, polja, userId, new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                poljaFiltered.clear();
                poljaFiltered.addAll(polja);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(PoljeActivity.this, "Greška pri dohvaćanju polja.", Toast.LENGTH_SHORT).show();
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return false;
            }
        });

        fabNewPolje.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PoljeActivity.this, PoljeNewActivity.class);
                startActivityForResult(intent, REQUEST_CODE_NEW_FIELD);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_NEW_FIELD && resultCode == RESULT_OK) {
            String userId = firebaseConnection.getCurrentUser().getUid();

            firebaseConnection.fetchUserFields(this, polja, userId, new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    poljaFiltered.clear();
                    poljaFiltered.addAll(polja);
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Toast.makeText(PoljeActivity.this, "Greška pri dohvaćanju polja.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void filter(String query) {
        poljaFiltered.clear();

        if (query.isEmpty()) {
            poljaFiltered.addAll(polja);
        } else {
            for (Polje polje : polja) {
                if (polje.getNaziv().toLowerCase().contains(query.toLowerCase())) {
                    poljaFiltered.add(polje);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (navigation.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}