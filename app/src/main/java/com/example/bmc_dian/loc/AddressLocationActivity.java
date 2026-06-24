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
import com.example.locsdk.LocSdk;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddressLocationActivity extends AppCompatActivity {

    private static final String TAG = "LOCATION_TRACKING";
    private LocSdk locSdk;
    private TextView statusText;
    private Location lastKnownLocation;
    private final Handler repeatLogHandler = new Handler(Looper.getMainLooper());

    public final Runnable repeatLogRunnable = new Runnable() {
        @Override
        public void run() {
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            
            Log.e(TAG, "================================================");
            Log.e(TAG, "MODE: ADDRESS_LOCATION_FETCH");
            Log.e(TAG, "TIME: " + time);
            
            if (lastKnownLocation != null) {
                String address = locSdk.getAddressFromLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
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
        Log.e(TAG, "AddressLocationActivity: Stopped");
    }
}
