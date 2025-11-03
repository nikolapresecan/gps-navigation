package com.example.gpsnavigacija;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.GeoJSONSourceData;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.ScreenCoordinate;
import com.mapbox.maps.Style;
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer;
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor;
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource;
import com.mapbox.maps.plugin.LocationPuck2D;
import com.mapbox.maps.plugin.Plugin;
import com.mapbox.maps.plugin.gestures.GesturesPlugin;
import com.mapbox.maps.plugin.gestures.OnMapClickListener;
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PoljeNewActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "map_state";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LNG = "lng";
    private static final String KEY_ZOOM = "zoom";

    private EditText etNazivPolja;
    private TextView tvArea;
    private Button btnSaveField, btnLokacija;
    private ImageButton btnBack;

    private LocationComponentPlugin locationComponentPlugin;
    private MapView mapView;
    private MapboxMap mapboxMap;

    private GeoJsonSource pointsSource;
    private GeoJsonSource polygonSource;

    private FirebaseConnection firebaseConnection;
    private List<Tocka> poljeTocke = new ArrayList<>();

    private List<Feature> currentFeatures = new ArrayList<>();

    private int selectedMarkerIndex = -1;
    private boolean isMovingMarker = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_polje_new);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        etNazivPolja = findViewById(R.id.etNazivPolja);
        tvArea = findViewById(R.id.tvArea);
        btnSaveField = findViewById(R.id.btnSaveField);
        btnLokacija = findViewById(R.id.btnLocation);
        mapView = findViewById(R.id.mapView);

        firebaseConnection = new FirebaseConnection();

        mapView.getMapboxMap().loadStyleUri(Style.SATELLITE_STREETS, style -> {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
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
                btnLokacija.setOnClickListener(view -> centerOnUserLocation());
            } else {
                Toast.makeText(this, "Location component nije dostupan!", Toast.LENGTH_SHORT).show();
            }

            mapboxMap = mapView.getMapboxMap();

            pointsSource = new GeoJsonSource.Builder("points-source")
                    .data(FeatureCollection.fromFeatures(new Feature[]{}).toJson())
                    .build();
            pointsSource.bindTo(style);

            polygonSource = new GeoJsonSource.Builder("polygon-source")
                    .data(FeatureCollection.fromFeatures(new Feature[]{}).toJson())
                    .build();
            polygonSource.bindTo(style);

            Drawable drawable = AppCompatResources.getDrawable(this, R.drawable.marker_icon);
            if (drawable != null) {
                Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                        drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
                style.addImage("marker-icon", bitmap);
            } else {
                Toast.makeText(this, "Marker drawable nije pronađen", Toast.LENGTH_SHORT).show();
            }

            SymbolLayer pointsLayer = new SymbolLayer("points-layer", "points-source")
                    .iconImage("marker-icon")
                    .iconAllowOverlap(true)
                    .iconIgnorePlacement(true)
                    .iconAnchor(IconAnchor.BOTTOM);
            pointsLayer.bindTo(style);

            GesturesPlugin gesturesPlugin = mapView.getPlugin(Plugin.MAPBOX_GESTURES_PLUGIN_ID);
            if (gesturesPlugin != null) {
                gesturesPlugin.addOnMapLongClickListener(new OnMapLongClickListener() {
                    @Override
                    public boolean onMapLongClick(Point point) {

                        currentFeatures.add(Feature.fromGeometry(point));
                        FeatureCollection updatedCollection = FeatureCollection.fromFeatures(currentFeatures);
                        updatePointsSource(updatedCollection);

                        Tocka novaTocka = new Tocka(point.latitude(), point.longitude());
                        poljeTocke.add(novaTocka);
                        updatePolygonSource();
                        updateArea();
                        return true;
                    }
                });

                gesturesPlugin.addOnMapClickListener(new OnMapClickListener() {
                    @Override
                    public boolean onMapClick(Point point) {
                        if (isMovingMarker && selectedMarkerIndex != -1) {
                            Tocka t = poljeTocke.get(selectedMarkerIndex);
                            t.setLatitude(point.latitude());
                            t.setLongitude(point.longitude());

                            currentFeatures.set(selectedMarkerIndex, Feature.fromGeometry(point));
                            updatePointsSource(FeatureCollection.fromFeatures(currentFeatures));
                            updatePolygonSource();
                            updateArea();
                            Toast.makeText(PoljeNewActivity.this, "Točka pomaknuta", Toast.LENGTH_SHORT).show();
                            isMovingMarker = false;
                            selectedMarkerIndex = -1;
                            return true;
                        } else {
                            ScreenCoordinate clickPoint = mapView.getMapboxMap().pixelForCoordinate(point);
                            float threshold = 50.0f;
                            int foundIndex = -1;
                            float minDistance = Float.MAX_VALUE;
                            for (int i = 0; i < poljeTocke.size(); i++) {
                                Tocka t = poljeTocke.get(i);
                                Point markerPoint = Point.fromLngLat(t.getLongitude(), t.getLatitude());
                                ScreenCoordinate markerPixel = mapView.getMapboxMap().pixelForCoordinate(markerPoint);
                                double dx = clickPoint.getX() - markerPixel.getX();
                                double dy = clickPoint.getY() - markerPixel.getY();
                                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                                if (distance < threshold && distance < minDistance) {
                                    minDistance = distance;
                                    foundIndex = i;
                                }
                            }
                            if (foundIndex != -1) {
                                int finalFoundIndex = foundIndex;
                                new AlertDialog.Builder(PoljeNewActivity.this)
                                        .setTitle("Izaberi akciju")
                                        .setMessage("Želite pomaknuti ili obrisati točku?")
                                        .setPositiveButton("Pomakni", (dialog, which) -> {
                                            isMovingMarker = true;
                                            selectedMarkerIndex = finalFoundIndex;
                                            Toast.makeText(PoljeNewActivity.this, "Kliknite na novu lokaciju", Toast.LENGTH_SHORT).show();
                                        })
                                        .setNegativeButton("Obriši", (dialog, which) -> {
                                            poljeTocke.remove(finalFoundIndex);
                                            currentFeatures.remove(finalFoundIndex);
                                            updatePointsSource(FeatureCollection.fromFeatures(currentFeatures));
                                            updatePolygonSource();
                                            updateArea();
                                            Toast.makeText(PoljeNewActivity.this, "Točka obrisana", Toast.LENGTH_SHORT).show();
                                        })
                                        .setNeutralButton("Odustani", null)
                                        .show();
                            }
                        }
                        return false;
                    }
                });
        } else {
            Toast.makeText(this, "Gestures plugin nije pronađen!", Toast.LENGTH_SHORT).show();
        }

        });

        btnSaveField.setOnClickListener(v -> saveField());
    }

    private void centerOnUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
        }
    }

    private void updatePointsSource(FeatureCollection updatedCollection) {
        mapView.getMapboxMap().getStyle(style -> {
            if (style.styleSourceExists("points-source")) {
                style.setStyleGeoJSONSourceData("points-source", GeoJSONSourceData.valueOf(updatedCollection.toJson()));
            } else {
                GeoJsonSource newSource = new GeoJsonSource.Builder("points-source")
                        .data(updatedCollection.toJson())
                        .build();
                newSource.bindTo(style);
            }

            if (style.getStyleLayerProperties("points-layer") == null) {
                SymbolLayer pointsLayer = new SymbolLayer("points-layer", "points-source")
                        .iconImage("marker-icon")
                        .iconAllowOverlap(true)
                        .iconIgnorePlacement(true)
                        .iconAnchor(IconAnchor.BOTTOM);
                pointsLayer.bindTo(style);
            }
        });
    }

    private void updatePolygonSource() {
        if (poljeTocke.size() < 3) return;

        List<Point> polygonPoints = new ArrayList<>();
        for (Tocka t : poljeTocke) {
            polygonPoints.add(Point.fromLngLat(t.getLongitude(), t.getLatitude()));
        }
        polygonPoints.add(polygonPoints.get(0));

        Feature polygonFeature = Feature.fromGeometry(Polygon.fromLngLats(Collections.singletonList(polygonPoints)));
        FeatureCollection polygonCollection = FeatureCollection.fromFeature(polygonFeature);

        mapView.getMapboxMap().getStyle(style -> {
            if (style.styleSourceExists("polygon-source")) {
                style.setStyleGeoJSONSourceData("polygon-source", GeoJSONSourceData.valueOf(polygonCollection.toJson()));
            } else {
                GeoJsonSource newPolygonSource = new GeoJsonSource.Builder("polygon-source")
                        .data(polygonCollection.toJson())
                        .build();
                newPolygonSource.bindTo(style);
            }
        });
    }

    private double calculateSphericalArea(List<Tocka> tocke) {
        int n = tocke.size();
        if (n < 3) return 0;

        double total = 0;
        double R = 6371000;

        for (int i = 0; i < n; i++) {
            Tocka current = tocke.get(i);
            Tocka next = tocke.get((i + 1) % n);
            double lat1 = Math.toRadians(current.getLatitude());
            double lon1 = Math.toRadians(current.getLongitude());
            double lat2 = Math.toRadians(next.getLatitude());
            double lon2 = Math.toRadians(next.getLongitude());
            total += (lon2 - lon1) * (2 + Math.sin(lat1) + Math.sin(lat2));
        }
        double areaM2 = Math.abs(total) * R * R / 2.0;
        return areaM2;
    }

    private void updateArea() {
        if (poljeTocke.size() < 3) {
            tvArea.setText("Površina: -");
            return;
        }
        double areaM2 = calculateSphericalArea(poljeTocke);
        double areaHa = areaM2 / 10000.0;
        DecimalFormat df = new DecimalFormat("#.##");
        tvArea.setText("Površina: " + df.format(areaHa) + " ha");
    }

    private void saveField() {
        String naziv = etNazivPolja.getText().toString().trim();
        if (naziv.isEmpty()) {
            Toast.makeText(this, "Unesite naziv polja", Toast.LENGTH_SHORT).show();
            return;
        }
        if (poljeTocke.size() < 3) {
            Toast.makeText(this, "Morate označiti barem 3 točke za polje", Toast.LENGTH_SHORT).show();
            return;
        }

        String korisnikId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        double area = calculateSphericalArea(poljeTocke);
        double areaHa = area / 10000.0;

        Polje novoPolje = new Polje();
        novoPolje.setNaziv(naziv);
        novoPolje.setPovrsina(areaHa);
        novoPolje.setTocke(poljeTocke);
        novoPolje.setKorisnik_id(korisnikId);

        firebaseConnection.newField(novoPolje, new SaveCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(PoljeNewActivity.this, "Polje spremljeno!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(PoljeNewActivity.this, "Greška pri spremanju: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}