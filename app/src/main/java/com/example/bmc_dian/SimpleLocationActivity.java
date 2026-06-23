package com.example.bmc_dian;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SimpleLocationActivity extends AppCompatActivity {

    private static final String TAG = "LOCATION_TRACKING";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private TextView statusText;
    private Location lastKnownLocation;
    private final Handler repeatLogHandler = new Handler(Looper.getMainLooper());

    private final Runnable repeatLogRunnable = new Runnable() {
        @Override
        public void run() {
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            
            // LOGGING EVERY 2 SECONDS NO MATTER WHAT
            Log.e(TAG, "------------------------------------------------");
            Log.e(TAG, "MODE: SIMPLE_LOCATION_FETCH");
            Log.e(TAG, "TIME: " + time);
            
            if (lastKnownLocation != null) {
                Log.e(TAG, "DATA: Lat: " + lastKnownLocation.getLatitude() + ", Lng: " + lastKnownLocation.getLongitude());
                statusText.setText("Lat: " + lastKnownLocation.getLatitude() + "\nLng: " + lastKnownLocation.getLongitude() + "\nLast Updated: " + time);
            } else {
                Log.e(TAG, "DATA: Waiting for location fix (Check GPS/Internet)");
                statusText.setText("Searching...\n(Ensure GPS is enabled)\nTime: " + time);
            }
            Log.e(TAG, "------------------------------------------------");
            
            repeatLogHandler.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_location);
        Log.e(TAG, "SimpleLocationActivity: Started");

        statusText = findViewById(R.id.statusText);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        startLocationUpdates();
        repeatLogHandler.post(repeatLogRunnable);
    }

    private void startLocationUpdates() {
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setMinUpdateIntervalMillis(1000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                lastKnownLocation = locationResult.getLastLocation();
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        repeatLogHandler.removeCallbacks(repeatLogRunnable);
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        Log.e(TAG, "SimpleLocationActivity: Stopped");
    }
}
