package com.example.gpsnavigacija;

import static java.util.Collections.singletonList;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.CameraState;
import com.mapbox.maps.EdgeInsets;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.extension.style.layers.generated.FillLayer;
import com.mapbox.maps.extension.style.layers.generated.LineLayer;
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer;
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor;
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource;
import com.mapbox.maps.plugin.LocationPuck2D;
import com.mapbox.maps.plugin.Plugin;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final String PREFS_NAME = "map_state";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LNG = "lng";
    private static final String KEY_ZOOM = "zoom";

    private MapView mapView;
    private FloatingActionButton fabMyLocation, fabNewActivity;
    private LocationComponentPlugin locationComponentPlugin;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView navHeaderName;
    private Switch switchFields;

    private Navigation navigation;

    private FirebaseConnection firebaseConnection;

    private List<Polje> allUserFields = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Početna");
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#0F5C10")));
        }

        drawerLayout = findViewById(R.id.my_drawer_layout);
        navHeaderName = findViewById(R.id.nav_header_name);
        navigationView = findViewById(R.id.navigation_view);
        switchFields = findViewById(R.id.switch_fields);

        firebaseConnection = new FirebaseConnection();

        navigation = new Navigation();
        navigation.setupNavigationDrawer(this, drawerLayout, navigationView, navHeaderName);

        mapView = findViewById(R.id.mapView);
        fabMyLocation = findViewById(R.id.fab_my_location);
        fabNewActivity = findViewById(R.id.fab_new_activity);

        double centerLat = getIntent().getDoubleExtra("center_lat", 0);
        double centerLng = getIntent().getDoubleExtra("center_lng", 0);
        boolean hasFieldLocation = centerLat != 0 && centerLng != 0;

        mapView.getMapboxMap().loadStyleUri(Style.SATELLITE_STREETS, style -> {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            if (hasFieldLocation) {
                showFieldLocation(centerLat, centerLng);
            }

            if (prefs.contains(KEY_LAT) && prefs.contains(KEY_LNG) && prefs.contains(KEY_ZOOM)) {
                double savedLat = prefs.getFloat(KEY_LAT, 0);
                double savedLng = prefs.getFloat(KEY_LNG, 0);
                double savedZoom = prefs.getFloat(KEY_ZOOM, 16.0F);
                mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                        .center(Point.fromLngLat(savedLng, savedLat))
                        .zoom(savedZoom)
                        .build());
            }

            locationComponentPlugin = mapView.getPlugin(Plugin.MAPBOX_LOCATION_COMPONENT_PLUGIN_ID);
            if (locationComponentPlugin != null) {
                locationComponentPlugin.setEnabled(true);
                locationComponentPlugin.setLocationPuck(new LocationPuck2D());

                fabMyLocation.setOnClickListener(v -> centerOnUserLocation());
            } else {
                Toast.makeText(this, "Location plugin nije dostupan!", Toast.LENGTH_SHORT).show();
            }

            checkLocationPermission();

            switchFields.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mapView.getMapboxMap().getStyle(currentStyle -> {
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    if (isChecked) {
                        firebaseConnection.fetchUserFields(this, allUserFields, userId, new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                addFieldPolygons(currentStyle, allUserFields);

                                List<Point> allPoints = new ArrayList<>();
                                for (Polje polje : allUserFields) {
                                    List<Tocka> fieldPoints = polje.getTocke();
                                    if (fieldPoints != null) {
                                        for (Tocka tocka : fieldPoints) {
                                            allPoints.add(Point.fromLngLat(tocka.getLongitude(), tocka.getLatitude()));
                                        }
                                    }
                                }
                            }
                            @Override
                            public void onCancelled(DatabaseError error) {

                            }
                        });
                    } else {
                        removeFieldPolygons(currentStyle, allUserFields);
                    }
                });
            });
        });

        fabNewActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeActivity.this, AktivnostActivity.class);
                startActivity(intent);
            }
        });
    }

    private void addFieldPolygons(Style style, List<Polje> polja) {
        if (polja == null || polja.isEmpty()) {
            Toast.makeText(this, "Nema dostupnih polja", Toast.LENGTH_SHORT).show();
            return;
        }

        for (Polje polje : polja) {
            List<Tocka> fieldPoints = polje.getTocke();
            if (fieldPoints != null && !fieldPoints.isEmpty()) {
                List<Point> polygonPoints = new ArrayList<>();
                for (Tocka tocka : fieldPoints) {
                    polygonPoints.add(Point.fromLngLat(tocka.getLongitude(), tocka.getLatitude()));
                }

                if (!polygonPoints.get(0).equals(polygonPoints.get(polygonPoints.size() - 1))) {
                    polygonPoints.add(polygonPoints.get(0));
                }
                Polygon polygon = Polygon.fromLngLats(singletonList(polygonPoints));
                Feature polygonFeature = Feature.fromGeometry(polygon);

                String sourceId = "field-source-" + polje.getId();
                String fillLayerId = "field-fill-layer-" + polje.getId();
                String lineLayerId = "field-line-layer-" + polje.getId();

                GeoJsonSource fieldSource = new GeoJsonSource.Builder(sourceId)
                        .feature(polygonFeature)
                        .build();
                fieldSource.bindTo(style);

                FillLayer fillLayer = new FillLayer(fillLayerId, sourceId)
                        .fillColor("#149c02")
                        .fillOpacity(0.5);
                fillLayer.bindTo(style);

                LineLayer lineLayer = new LineLayer(lineLayerId, sourceId)
                        .lineColor("#149c02")
                        .lineWidth(2.0);
                lineLayer.bindTo(style);
            }
        }
    }

    private void removeFieldPolygons(Style style, List<Polje> polja) {
        if (polja == null || polja.isEmpty()) {
            return;
        }

        for (Polje polje : polja) {
            String sourceId = "field-source-" + polje.getId();
            String fillLayerId = "field-fill-layer-" + polje.getId();
            String lineLayerId = "field-line-layer-" + polje.getId();

            if (style.styleLayerExists(fillLayerId)) {
                style.removeStyleLayer(fillLayerId);
            }
            if (style.styleLayerExists(lineLayerId)) {
                style.removeStyleLayer(lineLayerId);
            }
            if (style.styleSourceExists(sourceId)) {
                style.removeStyleSource(sourceId);
            }
        }
    }

    public void showFieldLocation(double lat, double lng) {
        ArrayList<Tocka> fieldPoints = (ArrayList<Tocka>) getIntent().getSerializableExtra("field_points");

        mapView.getMapboxMap().loadStyleUri(Style.SATELLITE_STREETS, style -> {
            GeoJsonSource geoJsonSource = new GeoJsonSource.Builder("field-source")
                    .feature(Feature.fromGeometry(Point.fromLngLat(lng, lat)))
                    .build();

            geoJsonSource.bindTo(style);

            Bitmap markerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mapbox_marker_icon_default);
            style.addImage("marker-icon", markerBitmap);

            SymbolLayer symbolLayer = new SymbolLayer("field-layer", "field-source")
                    .iconImage("marker-icon")
                    .iconAllowOverlap(true)
                    .iconIgnorePlacement(true)
                    .iconAnchor(IconAnchor.BOTTOM);

            symbolLayer.bindTo(style);

            mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                    .center(Point.fromLngLat(lng, lat))
                    .zoom(16.5)
                    .build());

            /*if (fieldPoints != null && !fieldPoints.isEmpty()) {
                List<Point> polygonPoints = new ArrayList<>();
                for (Tocka tocka : fieldPoints) {
                    polygonPoints.add(Point.fromLngLat(tocka.getLongitude(), tocka.getLatitude()));
                }

                if (!polygonPoints.get(0).equals(polygonPoints.get(polygonPoints.size() - 1))) {
                    polygonPoints.add(polygonPoints.get(0));
                }

                Polygon polygon = Polygon.fromLngLats(singletonList(polygonPoints));
                Feature polygonFeature = Feature.fromGeometry(polygon);

                GeoJsonSource fieldSource = new GeoJsonSource.Builder("field-source")
                        .feature(polygonFeature)
                        .build();
                fieldSource.bindTo(style);

                FillLayer fillLayer = new FillLayer("field-fill-layer", "field-source")
                        .fillColor("#149c02")
                        .fillOpacity(0.5);
                fillLayer.bindTo(style);

                LineLayer lineLayer = new LineLayer("field-line-layer", "field-source")
                        .lineColor("#149c02")
                        .lineWidth(2.0);
                lineLayer.bindTo(style);

                mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                        .center(Point.fromLngLat(lng, lat))
                        .zoom(16.5)
                        .build());
            } else {

            }*/
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (navigation.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void centerOnUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (locationComponentPlugin != null) {
                locationComponentPlugin.addOnIndicatorPositionChangedListener(new OnIndicatorPositionChangedListener() {
                    @Override
                    public void onIndicatorPositionChanged(Point point) {
                        mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                                .center(point)
                                .zoom(18.0)
                                .build());
                        locationComponentPlugin.removeOnIndicatorPositionChangedListener(this);
                    }
                });
            } else {
                Toast.makeText(this, "Location component nije dostupan!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Nije dopušteno!", Toast.LENGTH_SHORT).show();
            checkLocationPermission();
        }
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }

    private void saveMapState() {
        CameraState cameraState = mapView.getMapboxMap().getCameraState();
        Point center = cameraState.getCenter();
        double zoom = cameraState.getZoom();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (center != null) {
            editor.putFloat(KEY_LAT, (float) center.latitude());
            editor.putFloat(KEY_LNG, (float) center.longitude());
        }
        editor.putFloat(KEY_ZOOM, (float) zoom);
        editor.apply();
    }


    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        saveMapState();
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
}