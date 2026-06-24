package com.example.bmc_dian.loc;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
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

import com.example.bmc_dian.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddressLocationActivity extends AppCompatActivity {

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
            
            Log.e(TAG, "================================================");
            Log.e(TAG, "MODE: ADDRESS_LOCATION_FETCH");
            Log.e(TAG, "TIME: " + time);
            
            if (lastKnownLocation != null) {
                String address = getAddressFromLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                Log.e(TAG, "LAT/LNG: " + lastKnownLocation.getLatitude() + ", " + lastKnownLocation.getLongitude());
                Log.e(TAG, "ADDRESS: " + address);
                statusText.setText("Address:\n" + address + "\n\nLast updated: " + time);
            } else {
                Log.e(TAG, "DATA: Fetching location for address...");
                statusText.setText("Fetching address...\nTime: " + time);
            }
            Log.e(TAG, "================================================");
            
            repeatLogHandler.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_location);
        Log.e(TAG, "AddressLocationActivity: Started");

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

    private String getAddressFromLocation(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            return "Geocoder Error: " + e.getMessage();
        }
        return "Address not found";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        repeatLogHandler.removeCallbacks(repeatLogRunnable);
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        Log.e(TAG, "AddressLocationActivity: Stopped");
    }
}
