package com.example.bmc_dian.loc;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
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
import com.example.locsdk.LocSdk;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScreenOnFetchActivity extends AppCompatActivity {

    private static final String TAG = "LOCATION_TRACKING";
    private LocSdk locSdk;
    private TextView statusText;
    private Location lastKnownLocation;
    private final Handler repeatLogHandler = new Handler(Looper.getMainLooper());

    public final Runnable repeatLogRunnable = new Runnable() {
        @Override
        public void run() {
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            
            Log.e(TAG, "++++++++++++++++++++++++++++++++++++++++++++++++");
            Log.e(TAG, "MODE: SCREEN_ON_LOCATION_FETCH");
            Log.e(TAG, "TIME: " + time);
            
            if (lastKnownLocation != null) {
                String latLng = String.format(Locale.getDefault(), "Lat: %.6f, Lng: %.6f", lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                Log.e(TAG, "DATA: " + latLng);
                statusText.setText("Screen Active:\n" + latLng + "\nTime: " + time);
                Toast.makeText(ScreenOnFetchActivity.this, "Location Updated: " + time, Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "DATA: Searching for fix...");
                statusText.setText("Screen Active - Searching...\nTime: " + time);
            }
            Log.e(TAG, "++++++++++++++++++++++++++++++++++++++++++++++++");
            
            long delay = 2000;
            if (locSdk.getConfig() != null) delay = locSdk.getConfig().getInterval();
            repeatLogHandler.postDelayed(this, delay);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_on_fetch);
        Log.e(TAG, "ScreenOnFetchActivity: Created");

        statusText = findViewById(R.id.statusText);
        locSdk = new LocSdk(this);
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        Log.e(TAG, "ScreenOnFetchActivity: Resumed - Starting heartbeats");
//        locSdk.startSimpleLocationUpdates(locSdk.getConfig().getInterval(), location -> lastKnownLocation = location);
//        repeatLogHandler.post(repeatLogRunnable);
//    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissionsAndStart();
    }

    private void checkPermissionsAndStart() {
        if (!locSdk.hasLocationPermission()) {
            Log.w(TAG, "ScreenOnFetch: Missing Location Permission");
            locSdk.requestLocationPermission(this);
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!gpsEnabled) {
            Log.w(TAG, "ScreenOnFetch: GPS is OFF");
            locSdk.requestEnableGps(this);
            return;
        }

        // Check for Background permission even for screen-on if user wants consistency
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!locSdk.hasBackgroundLocationPermission()) {
                Log.w(TAG, "ScreenOnFetch: Missing Background Permission");
                locSdk.requestBackgroundLocationPermission(this);
                return;
            }
        }

        Log.i(TAG, "ScreenOnFetch: Starting updates");
        Toast.makeText(this, "Screen tracking started", Toast.LENGTH_SHORT).show();
        
        locSdk.startSimpleLocationUpdates(
                locSdk.getConfig().getInterval(),
                location -> lastKnownLocation = location
        );

        repeatLogHandler.removeCallbacks(repeatLogRunnable);
        repeatLogHandler.post(repeatLogRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "ScreenOnFetchActivity: Paused - Stopping heartbeats");
        locSdk.stopLocationUpdates();
        repeatLogHandler.removeCallbacks(repeatLogRunnable);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        if (granted) {
            Log.d(TAG, "Permission granted for request: " + requestCode);
            checkPermissionsAndStart();
        } else {
            Log.e(TAG, "Permission denied for request: " + requestCode);
            String key = "";
            if (requestCode == 1001) key = "location_denied_count";
            else if (requestCode == 3002) key = "bg_loc_denied_count";
            
            if (!key.isEmpty()) {
                android.content.SharedPreferences prefs = getSharedPreferences("permission_pref", Context.MODE_PRIVATE);
                int count = prefs.getInt(key, 0);
                prefs.edit().putInt(key, count + 1).apply();
            }
            Toast.makeText(this, "Permission required for tracking", Toast.LENGTH_SHORT).show();
        }
    }

}
