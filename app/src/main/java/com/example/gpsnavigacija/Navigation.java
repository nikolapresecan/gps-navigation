package com.example.gpsnavigacija;

import android.app.Activity;
import android.content.Intent;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class Navigation {
    private final FirebaseConnection firebaseConnection;
    private ActionBarDrawerToggle toggle;

    public Navigation() {
        firebaseConnection = new FirebaseConnection();
    }

    public void setupNavigationDrawer(AppCompatActivity activity, DrawerLayout drawerLayout, NavigationView navigationView, TextView navHeaderName) {
        toggle = new ActionBarDrawerToggle(activity, drawerLayout, R.string.nav_open, R.string.nav_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_odjava) {
                LoginActivity.logoutUser(activity);
                return true;
            }
            else if(id == R.id.nav_pocetna){
                activity.startActivity(new Intent(activity, HomeActivity.class));
            }
            else if (id == R.id.nav_polja) {
                activity.startActivity(new Intent(activity, PoljeActivity.class));
                return true;
            }

            return false;
        });

        firebaseConnection.setNameInDrawer(activity, navHeaderName);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return toggle != null && toggle.onOptionsItemSelected(item);
    }
}