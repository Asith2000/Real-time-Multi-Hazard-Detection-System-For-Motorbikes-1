package com.example.roadhazardmap.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.roadhazardmap.R;
import com.example.roadhazardmap.models.Hazard;
import com.example.roadhazardmap.utils.HazardUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DashboardActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    // Map & location
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;
    private LatLng currentLatLng;

    // Firebase
    private DatabaseReference hazardsRef;
    private final List<Hazard> hazardList = new ArrayList<>();

    // Alert state
    private final Set<String> alertedHazardIds = new HashSet<>();
    private MaterialCardView cardHazardAlert;
    private TextView tvAlertTitle, tvAlertDetail;

    // Vibrator
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("Road Hazard Map");

        // Alert banner views
        cardHazardAlert = findViewById(R.id.cardHazardAlert);
        tvAlertTitle    = findViewById(R.id.tvAlertTitle);
        tvAlertDetail   = findViewById(R.id.tvAlertDetail);

        ImageButton btnDismiss = findViewById(R.id.btnDismissAlert);
        if (btnDismiss != null)
            btnDismiss.setOnClickListener(v -> cardHazardAlert.setVisibility(View.GONE));

        // FAB — centre map on user
        FloatingActionButton fab = findViewById(R.id.fabMyLocation);
        if (fab != null) fab.setOnClickListener(v -> centreOnUser());

        vibrator   = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialise map asynchronously
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        // Firebase RTDB reference — listens to /hazards node
        hazardsRef = FirebaseDatabase.getInstance().getReference("hazards");
    }

    // ──────────────────────────────────────────────────────────────────────
    // Google Maps callback
    // ──────────────────────────────────────────────────────────────────────
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false); // We use our own FAB

        checkAndRequestLocationPermission();
        listenForHazards();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Firebase real-time listener
    // ──────────────────────────────────────────────────────────────────────
    private void listenForHazards() {
        hazardsRef.addChildEventListener(new ChildEventListener() {

            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String prev) {
                Hazard hazard = snapshot.getValue(Hazard.class);
                if (hazard == null) return;
                hazard.setId(snapshot.getKey());
                hazardList.add(hazard);
                addMarker(hazard);
                checkProximityAlert(hazard);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String prev) {
                Hazard updated = snapshot.getValue(Hazard.class);
                if (updated == null) return;
                updated.setId(snapshot.getKey());
                for (int i = 0; i < hazardList.size(); i++) {
                    if (updated.getId() != null &&
                            updated.getId().equals(hazardList.get(i).getId())) {
                        hazardList.set(i, updated);
                        break;
                    }
                }
                refreshAllMarkers();
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String removedId = snapshot.getKey();
                hazardList.removeIf(h -> removedId != null && removedId.equals(h.getId()));
                refreshAllMarkers();
            }

            @Override public void onChildMoved(@NonNull DataSnapshot s, String p) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DashboardActivity.this,
                        "DB error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addMarker(Hazard hazard) {
        if (mMap == null) return;
        LatLng pos = new LatLng(hazard.getLatitude(), hazard.getLongitude());
        mMap.addMarker(new MarkerOptions()
                .position(pos)
                .title(hazard.getLabel())
                .snippet(HazardUtils.getSnippetForHazard(hazard))
                .icon(HazardUtils.getMarkerIconForClass(hazard.getHazardClass())));
    }

    private void refreshAllMarkers() {
        if (mMap == null) return;
        mMap.clear();
        for (Hazard h : hazardList) addMarker(h);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Location updates & proximity alerts
    // ──────────────────────────────────────────────────────────────────────
    private void startLocationUpdates() {
        LocationRequest req = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location loc = result.getLastLocation();
                if (loc == null) return;
                currentLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());
                for (Hazard h : hazardList) checkProximityAlert(h);
            }
        };

        if (hasLocationPermission()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            fusedClient.requestLocationUpdates(req, locationCallback, getMainLooper());
            try { mMap.setMyLocationEnabled(true); } catch (SecurityException ignored) {}
            fusedClient.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null) {
                    LatLng here = new LatLng(loc.getLatitude(), loc.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(here, 16f));
                }
            });
        }
    }

    private void centreOnUser() {
        if (!hasLocationPermission() || currentLatLng == null) return;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f));
    }

    private void checkProximityAlert(Hazard hazard) {
        if (currentLatLng == null || hazard.getId() == null) return;
        LatLng hazardPos = new LatLng(hazard.getLatitude(), hazard.getLongitude());
        double dist = HazardUtils.distanceMetres(currentLatLng, hazardPos);

        if (dist <= HazardUtils.PROXIMITY_THRESHOLD_METRES
                && !alertedHazardIds.contains(hazard.getId())) {
            alertedHazardIds.add(hazard.getId());
            triggerAlert(hazard, dist);
        } else if (dist > HazardUtils.PROXIMITY_THRESHOLD_METRES * 2.0) {
            // Allow re-alerting when rider approaches again
            alertedHazardIds.remove(hazard.getId());
        }
    }

    @SuppressWarnings("deprecation")
    private void triggerAlert(Hazard hazard, double distMetres) {
        // Vibrate: three short pulses
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                long[] timings  = {0, 250, 120, 250, 120, 250};
                int[]  amps     = {0, 200,   0, 200,   0, 200};
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amps, -1));
            } else {
                vibrator.vibrate(new long[]{0, 250, 120, 250, 120, 250}, -1);
            }
        }

        // Show banner
        runOnUiThread(() -> {
            String emoji = HazardUtils.getEmojiForClass(hazard.getHazardClass());
            tvAlertTitle.setText(emoji + "  " + hazard.getLabel() + " Detected!");
            tvAlertDetail.setText(String.format("%.0f m ahead — slow down and stay alert", distMetres));
            cardHazardAlert.setVisibility(View.VISIBLE);

            // Auto-dismiss after 6 seconds
            cardHazardAlert.postDelayed(
                    () -> cardHazardAlert.setVisibility(View.GONE), 6000);
        });
    }

    // ──────────────────────────────────────────────────────────────────────
    // Permissions
    // ──────────────────────────────────────────────────────────────────────
    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void checkAndRequestLocationPermission() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            Toast.makeText(this,
                    "Location permission required for proximity alerts",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Options menu
    // ──────────────────────────────────────────────────────────────────────
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_legend) {
            showLegendDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLegendDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Map Legend")
                .setMessage(
                        "🔴  Red marker     → Pothole\n\n" +
                        "🟠  Orange marker  → Speed Bump\n\n" +
                        "🔵  Blue marker    → Crosswalk\n\n" +
                        "🟡  Yellow marker  → Road Debris\n\n" +
                        "Proximity alert triggers within 100 m."
                )
                .setPositiveButton("Close", null)
                .show();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────────────────
    @Override
    protected void onStop() {
        super.onStop();
        if (locationCallback != null)
            fusedClient.removeLocationUpdates(locationCallback);
    }
}
