package com.example.bmc_dian.loc;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
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
                Log.e(TAG, "DATA: Lat: " + lastKnownLocation.getLatitude() + ", Lng: " + lastKnownLocation.getLongitude());
                statusText.setText("Screen Active:\nLat: " + lastKnownLocation.getLatitude() + "\nLng: " + lastKnownLocation.getLongitude() + "\nTime: " + time);
            } else {
                Log.e(TAG, "DATA: Searching for fix...");
                statusText.setText("Screen Active - Searching...\nTime: " + time);
            }
            Log.e(TAG, "++++++++++++++++++++++++++++++++++++++++++++++++");
            
            repeatLogHandler.postDelayed(this, 2000);
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

        if (!locSdk.hasLocationPermission()) {

            locSdk.requestLocationPermission(this);
            return;
        }

        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        boolean gpsEnabled =
                locationManager != null &&
                        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!gpsEnabled) {

            locSdk.requestEnableGps(this);
            return;
        }

        locSdk.startSimpleLocationUpdates(
                locSdk.getConfig().getInterval(),
                location -> lastKnownLocation = location
        );

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

        if (requestCode == 1001) {

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                onResume();
            }
        }
    }

}
