package com.example.bmc_dian;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity2 extends AppCompatActivity {

    public static final String TAG = "LOCATION_TRACKING";
    private static final int REQUEST_CODE_PERMISSIONS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);
        
        Log.e(TAG, "!!! [APP] MainActivity2 Started !!!");

        showDeviceVersionPopup();
        checkBatterySaver();

        findViewById(R.id.notification).setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.maps).setOnClickListener(v -> startActivity(new Intent(this, MapsActivity.class)));
        findViewById(R.id.currentLoc).setOnClickListener(v -> startActivity(new Intent(this, SimpleLocationActivity.class)));
        findViewById(R.id.currentLocAddress).setOnClickListener(v -> startActivity(new Intent(this, AddressLocationActivity.class)));
        findViewById(R.id.bgFetch).setOnClickListener(v -> startActivity(new Intent(this, BackgroundFetchActivity.class)));
        findViewById(R.id.screenOnFetch).setOnClickListener(v -> startActivity(new Intent(this, ScreenOnFetchActivity.class)));

        checkAndRequestPermissions();
    }

    private void showDeviceVersionPopup() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            new AlertDialog.Builder(this)
                    .setTitle("Device Version Warning")
                    .setMessage("Your device is running Android " + Build.VERSION.RELEASE + " (below 13). You may face issues with background tracking and notifications. Android 13+ is recommended.")
                    .setPositiveButton("I Understand", null)
                    .show();
        }
    }

    private void checkBatterySaver() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null && pm.isPowerSaveMode()) {
            new AlertDialog.Builder(this)
                    .setTitle("Battery Saver is ON")
                    .setMessage("Battery saver mode may stop the background tracking service. Please turn it off for consistent 1-minute logging.")
                    .setPositiveButton("Open Settings", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void checkAndRequestPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Background location usually needs to be requested separately in some versions, 
            // but for simplicity we add it here.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Background location request popup
                showBackgroundLocationRationale();
            }
        }

        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), REQUEST_CODE_PERMISSIONS);
        }
    }

    private void showBackgroundLocationRationale() {
        new AlertDialog.Builder(this)
                .setTitle("Background Location Access")
                .setMessage("To get logs every 1 minute while the screen is off, please select 'Allow all the time' in the next location permission screen.")
                .setPositiveButton("OK", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 2001);
                    }
                })
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission GRANTED: " + permissions[i]);
                } else {
                    Log.e(TAG, "Permission DENIED: " + permissions[i]);
                }
            }
        }
    }
}
