package com.example.bmc_dian.loc;

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

import com.example.bmc_dian.R;
import com.example.locsdk.LocSdk;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SimpleLocationActivity extends AppCompatActivity {

    private static final String TAG = "LOCATION_TRACKING";
    private LocSdk locSdk;
    private TextView statusText;
    private Location lastKnownLocation;
    private final Handler repeatLogHandler = new Handler(Looper.getMainLooper());

    public final Runnable repeatLogRunnable = new Runnable() {
        @Override
        public void run() {
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            
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
        locSdk = new LocSdk(this);

        locSdk.startSimpleLocationUpdates(2000, location -> lastKnownLocation = location);
        repeatLogHandler.post(repeatLogRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        repeatLogHandler.removeCallbacks(repeatLogRunnable);
        if (locSdk != null) {
            locSdk.stopLocationUpdates();
        }
        Log.e(TAG, "SimpleLocationActivity: Stopped");
    }
}
