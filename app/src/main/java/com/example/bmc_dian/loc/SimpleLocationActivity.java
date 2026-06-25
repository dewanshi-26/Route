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


    private boolean locationAlreadyFetched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_location);
        Log.e(TAG, "SimpleLocationActivity: Started");

        statusText = findViewById(R.id.statusText);
        locSdk = new LocSdk(this);

        fetchLocationOnce();
//        new Handler().postDelayed(
//                this::fetchLocationOnce,
//                300
//        );
    }

    private void fetchLocationOnce() {
        if (locationAlreadyFetched) {
            return;
        }

        statusText.setText("Fetching location...");
        locSdk.getCurrentLocation(SimpleLocationActivity.this,
                location-> {
            if (location != null) {
                String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                String logMsg = "Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude();

                Log.e(TAG, "------------------------------------------------");
                Log.e(TAG, "MODE: SIMPLE_LOCATION_FETCH (ONCE)");
                Log.e(TAG, "TIME: " + time);
                Log.e(TAG, "DATA: " + logMsg);
                Log.e(TAG, "------------------------------------------------");

                statusText.setText(logMsg + "\nTime: " + time);
                Toast.makeText(SimpleLocationActivity.this, "Location Fetched:\n" + logMsg, Toast.LENGTH_LONG).show();
            } else {
                statusText.setText("Failed to fetch location.");
                Toast.makeText(SimpleLocationActivity.this, "Failed to fetch location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "SimpleLocationActivity: Stopped");
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (locSdk != null) {
            fetchLocationOnce();
        }
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

                fetchLocationOnce();
            }
        }
    }

}
