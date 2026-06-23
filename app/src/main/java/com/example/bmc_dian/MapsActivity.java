package com.example.bmc_dian;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * MapsActivity implements real-time vehicle tracking.
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String TAG = "TRACKING_LOG";

    private Button btnStart, btnStop;
    private TextView tvSpeed, tvKM, tvTime;
    private Marker movingMarker, startMarker, endMarker;
    private Polyline trailPolyline;
    private final List<LatLng> pathPoints = new ArrayList<>();

    private boolean isTracking = false;
    private double totalDistanceKm = 0.0;
    private Location lastLocation = null;
    private long startTimeMillis = 0;

    // Animation control
    private final Handler animationHandler = new Handler(Looper.getMainLooper());
    private Runnable animationRunnable;
    private LatLng lastTargetLatLng = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupUI();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupUI() {
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        tvSpeed = findViewById(R.id.tvSpeed);
        tvKM = findViewById(R.id.tvKM);
        tvTime = findViewById(R.id.tvTime);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        btnStart.setOnClickListener(v -> showStartDialog());
        btnStop.setOnClickListener(v -> showStopDialog());

        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
    }

    private void showStartDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmation")
                .setMessage("Are you sure you want to start tracking?")
                .setPositiveButton("Yes", (dialog, which) -> startTracking())
                .setNegativeButton("No", null)
                .show();
    }

    private void showStopDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmation")
                .setMessage("Are you sure you want to stop tracking?")
                .setPositiveButton("Yes", (dialog, which) -> stopTracking())
                .setNegativeButton("No", null)
                .show();
    }

    private void startTracking() {
        Log.d(TAG, ">>> START TRACKING REQUESTED <<<");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        isTracking = true;
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        
        totalDistanceKm = 0;
        lastLocation = null;
        startTimeMillis = System.currentTimeMillis();
        pathPoints.clear();
        lastTargetLatLng = null;
        
        if (movingMarker != null) movingMarker.remove();
        if (startMarker != null) startMarker.remove();
        if (endMarker != null) endMarker.remove();
        if (trailPolyline != null) trailPolyline.remove();
        movingMarker = null;
        startMarker = null;
        endMarker = null;
        trailPolyline = null;

        tvSpeed.setText("Speed: 0.0 km/h");
        tvKM.setText("KM: 0.00");
        tvTime.setText("Time: 0 min");

        requestLocationUpdates();
        
        // Immediate check for last known location
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null && isTracking) {
                Log.d(TAG, "Initial location found via fused provider");
                processLocation(location);
            }
        });

        Toast.makeText(this, "Tracking Started", Toast.LENGTH_SHORT).show();
    }

    private void stopTracking() {
        Log.d(TAG, ">>> STOP TRACKING REQUESTED <<<");
        isTracking = false;
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        // Cancel any running animation
        if (animationRunnable != null) {
            animationHandler.removeCallbacks(animationRunnable);
        }

        // Determine final position for the End Marker
        LatLng finalPos = null;
        if (movingMarker != null) {
            finalPos = movingMarker.getPosition();
        } else if (lastLocation != null) {
            finalPos = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        }

        // Add End Marker at the exact final position
        if (finalPos != null && mMap != null) {
            if (endMarker != null) endMarker.remove();
            
            // Add to pathPoints to complete the polyline
            if (!pathPoints.contains(finalPos)) {
                pathPoints.add(finalPos);
            }
            
            endMarker = mMap.addMarker(new MarkerOptions()
                    .position(finalPos)
                    .title("End Point")
                    .zIndex(200)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            
            // Final polyline update to the exact end point
            updatePolyline(finalPos);
            mMap.animateCamera(CameraUpdateFactory.newLatLng(finalPos));
            
            Log.d(TAG, "=============================================");
            Log.d(TAG, " TRACKING STOPPED ");
            Log.d(TAG, " FINAL DISTANCE: " + String.format(Locale.getDefault(), "%.2f km", totalDistanceKm));
            Log.d(TAG, " FINAL POSITION: " + finalPos.latitude + ", " + finalPos.longitude);
            Log.d(TAG, "=============================================");
        }

        // Remove the moving vehicle marker
        if (movingMarker != null) {
            movingMarker.remove();
            movingMarker = null;
        }

        Toast.makeText(this, "Tracking Stopped", Toast.LENGTH_SHORT).show();
    }

    private void requestLocationUpdates() {
        Log.d(TAG, "Requesting location updates (3s interval)");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(3000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!isTracking) return;
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        processLocation(location);
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void processLocation(Location location) {
        double speedKmH = location.getSpeed() * 3.6;
        
        // LOG DATA IMMEDIATELY FOR EVERY UPDATE
        fetchAddressAndLog(location, speedKmH, totalDistanceKm);

        // Accuracy filter for Map Updates
        if (location.getAccuracy() > 50) {
            Log.d(TAG, "Map Update Skipped: Accuracy " + location.getAccuracy() + "m > 50m");
            return;
        }

        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (lastLocation != null) {
            float distance = lastLocation.distanceTo(location);
            // Responsiveness threshold
            if (distance > 2.0) {
                totalDistanceKm += (distance / 1000.0);
                lastLocation = location;
                
                runOnUiThread(() -> {
                    tvSpeed.setText(String.format(Locale.getDefault(), "Speed: %.1f km/h", speedKmH));
                    tvKM.setText(String.format(Locale.getDefault(), "KM: %.2f", totalDistanceKm));
                    long elapsedMin = (System.currentTimeMillis() - startTimeMillis) / 60000;
                    tvTime.setText(String.format(Locale.getDefault(), "Time: %d min", elapsedMin));
                    
                    animateMarkerAndLine(currentLatLng, location.getBearing(), speedKmH);
                });
            }
        } else {
            // First point - Add Start Marker
            lastLocation = location;
            pathPoints.add(currentLatLng);
            runOnUiThread(() -> {
                if (mMap != null) {
                    startMarker = mMap.addMarker(new MarkerOptions()
                            .position(currentLatLng)
                            .title("Start Point")
                            .zIndex(200)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f));
                    updatePolyline(currentLatLng);
                }
            });
        }
    }

    private void animateMarkerAndLine(final LatLng toPosition, final float bearing, final double speed) {
        if (mMap == null || !isTracking) return;

        // Cancel previous animation and complete its path
        if (animationRunnable != null) {
            animationHandler.removeCallbacks(animationRunnable);
            if (lastTargetLatLng != null && !pathPoints.contains(lastTargetLatLng)) {
                pathPoints.add(lastTargetLatLng);
            }
        }
        
        lastTargetLatLng = toPosition;

        // Create moving marker if missing
        if (movingMarker == null && !pathPoints.isEmpty()) {
            LatLng lastPoint = pathPoints.get(pathPoints.size() - 1);
            movingMarker = mMap.addMarker(new MarkerOptions()
                    .position(lastPoint)
                    .anchor(0.5f, 0.5f)
                    .flat(true)
                    .rotation(bearing)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }

        if (movingMarker == null) return;

        final LatLng startPosition = movingMarker.getPosition();
        final long start = System.currentTimeMillis();
        final long duration = 2500; 
        final LinearInterpolator interpolator = new LinearInterpolator();

        animationRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isTracking || movingMarker == null) return;

                long elapsed = System.currentTimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                
                double lng = t * toPosition.longitude + (1 - t) * startPosition.longitude;
                double lat = t * toPosition.latitude + (1 - t) * startPosition.latitude;
                LatLng intermediatePos = new LatLng(lat, lng);
                
                movingMarker.setPosition(intermediatePos);
                if (speed > 1.5) movingMarker.setRotation(bearing);
                
                updatePolyline(intermediatePos);

                if (t < 1.0) {
                    animationHandler.postDelayed(this, 16);
                } else {
                    if (!pathPoints.contains(toPosition)) {
                        pathPoints.add(toPosition);
                    }
                    updatePolyline(toPosition);
                }
            }
        };
        
        animationHandler.post(animationRunnable);
        mMap.animateCamera(CameraUpdateFactory.newLatLng(toPosition));
    }

    private void updatePolyline(LatLng currentPos) {
        if (mMap == null) return;
        List<LatLng> pointsToDraw = new ArrayList<>(pathPoints);
        if (!pointsToDraw.contains(currentPos)) {
            pointsToDraw.add(currentPos);
        }

        if (trailPolyline == null) {
            trailPolyline = mMap.addPolyline(new PolylineOptions()
                    .addAll(pointsToDraw)
                    .width(12f) // Slightly thicker for better visibility
                    .color(Color.BLUE)
                    .startCap(new RoundCap())
                    .endCap(new RoundCap())
                    .jointType(JointType.ROUND)
                    .zIndex(100));
        } else {
            trailPolyline.setPoints(pointsToDraw);
        }
    }

    private void fetchAddressAndLog(final Location loc, double speed, double distance) {
        new Thread(() -> {
            String addr = "Fetching address...";
            try {
                Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    addr = addresses.get(0).getAddressLine(0);
                }
            } catch (Exception e) {
                addr = "Service Unavailable";
            }

            String dateStr = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date(loc.getTime()));
            String timeStr = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(loc.getTime()));

            // DIRECT LOGCAT OUTPUT
            Log.d(TAG, "=============================================");
            Log.d(TAG, " VEHICLE TRACKING DATA ");
            Log.d(TAG, " TIME      : " + dateStr + " " + timeStr);
            Log.d(TAG, " LATITUDE  : " + loc.getLatitude());
            Log.d(TAG, " LONGITUDE : " + loc.getLongitude());
            Log.d(TAG, " SPEED     : " + String.format(Locale.getDefault(), "%.1f km/h", speed));
            Log.d(TAG, " DISTANCE  : " + String.format(Locale.getDefault(), "%.2f km", distance));
            Log.d(TAG, " ADDRESS   : " + addr);
            Log.d(TAG, " ACCURACY  : " + loc.getAccuracy() + "m");
            Log.d(TAG, "=============================================");
        }).start();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "Map Ready");
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startTracking();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
