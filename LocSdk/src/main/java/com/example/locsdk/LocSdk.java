package com.example.locsdk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocSdk {

    private static final String TAG = "LocSdk";
    private final Context context;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static SDKConfig sdkConfig = new SDKConfig();

    public LocSdk(Context context) {
        this.context = context.getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.context);
    }

    /**
     * Get single location update.
     */
    @SuppressLint("MissingPermission")
    public void getCurrentLocation(LocationUpdateListener listener) {
        Log.d(TAG, "getCurrentLocation: Requesting single update");
        if (!hasLocationPermission()) {
            Log.e(TAG, "getCurrentLocation: Permission not granted");
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        Log.d(TAG, "getCurrentLocation: Success -> Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude());
                        if (listener != null) {
                            listener.onLocationUpdate(location);
                        }
                    } else {
                        Log.w(TAG, "getCurrentLocation: Received null location");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "getCurrentLocation: Failed -> " + e.getMessage()));
    }

    /**
     * Start simple location updates.
     */
    @SuppressLint("MissingPermission")
    public void startSimpleLocationUpdates(long interval, LocationUpdateListener listener) {
        Log.d(TAG, "startSimpleLocationUpdates: Interval = " + interval + "ms");
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
                .setMinUpdateIntervalMillis(interval / 2)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    Log.d(TAG, "onLocationResult: Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude());
                    if (listener != null) {
                        listener.onLocationUpdate(location);
                    }
                }
            }
        };

        if (hasLocationPermission()) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        } else {
            Log.e(TAG, "startSimpleLocationUpdates: Permission not granted");
        }
    }

    /**
     * Stop location updates.
     */
    public void stopLocationUpdates() {
        Log.d(TAG, "stopLocationUpdates: Stopping updates");
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    /**
     * Get address from coordinates.
     */
    public String getAddressFromLocation(double lat, double lng) {
        Log.d(TAG, "getAddressFromLocation: Fetching for Lat: " + lat + ", Lng: " + lng);
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String address = addresses.get(0).getAddressLine(0);
                Log.d(TAG, "getAddressFromLocation: Found -> " + address);
                return address;
            }
        } catch (IOException e) {
            Log.e(TAG, "getAddressFromLocation: Geocoder Error -> " + e.getMessage());
            return "Geocoder Error: " + e.getMessage();
        }
        Log.w(TAG, "getAddressFromLocation: Address not found");
        return "Address not found";
    }

    /**
     * Start background location service.
     */
    public void startBackgroundService() {
        Log.i(TAG, "startBackgroundService: Requesting service start");
        Intent intent = new Intent(context, LocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    /**
     * Stop background location service.
     */
    public void stopBackgroundService() {
        Log.i(TAG, "stopBackgroundService: Requesting service stop");
        Intent intent = new Intent(context, LocationService.class);
        intent.setAction(LocationService.ACTION_STOP);
        context.startService(intent);
    }

    /**
     * Send action to background service (PAUSE/RESUME).
     */
    public void sendActionToService(String action) {
        Log.i(TAG, "sendActionToService: Sending Action -> " + action);
        Intent intent = new Intent(context, LocationService.class);
        intent.setAction(action);
        context.startService(intent);
    }

    /**
     * Get all logs from database and print to Logcat.
     */
    public void dumpLogs() {
        LocationDbHelper dbHelper = new LocationDbHelper(context);
        Log.wtf(TAG, "!!! [DB DUMP] RECOVERING OVERNIGHT HISTORY !!!");
        android.database.Cursor cursor = dbHelper.getAllLogs();
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String date = cursor.getString(cursor.getColumnIndex(LocationDbHelper.COLUMN_DATE));
                @SuppressLint("Range") String time = cursor.getString(cursor.getColumnIndex(LocationDbHelper.COLUMN_TIME));
                @SuppressLint("Range") double lat = cursor.getDouble(cursor.getColumnIndex(LocationDbHelper.COLUMN_LAT));
                @SuppressLint("Range") double lng = cursor.getDouble(cursor.getColumnIndex(LocationDbHelper.COLUMN_LNG));
                @SuppressLint("Range") String addr = cursor.getString(cursor.getColumnIndex(LocationDbHelper.COLUMN_ADDR));
                @SuppressLint("Range") String status = cursor.getString(cursor.getColumnIndex(LocationDbHelper.COLUMN_STATUS));

                Log.wtf(TAG, String.format("RECOVERED [%d]: %s %s | %s | Lat: %.6f, Lng: %.6f | Addr: %s",
                        ++count, date, time, status, lat, lng, addr));
            } while (cursor.moveToNext());
            cursor.close();
        }
        if (count == 0) Log.wtf(TAG, "!!! [DB DUMP] DATABASE EMPTY !!!");
        else Log.wtf(TAG, "!!! [DB DUMP] SUCCESS: " + count + " LOGS PRINTED !!!");
    }

    /**
     * Share the log file.
     */
    public void shareLogFile(Activity activity) {
        java.io.File logFile = new java.io.File(context.getExternalFilesDir(null), "OvernightLogs.txt");
        if (!logFile.exists()) {
            android.widget.Toast.makeText(context, "No log file found yet.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(context, context.getPackageName() + ".locsdk.fileprovider", logFile);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(Intent.createChooser(intent, "Export Overnight Logs"));
    }

    /**
     * Clear all logs from database and file.
     */
    public void clearLogs() {
        LocationDbHelper dbHelper = new LocationDbHelper(context);
        dbHelper.clearAllLogs();
        java.io.File logFile = new java.io.File(context.getExternalFilesDir(null), "OvernightLogs.txt");
        if(logFile.exists()) logFile.delete();
        android.widget.Toast.makeText(context, "Database & File Cleared", android.widget.Toast.LENGTH_SHORT).show();
    }

    public interface LocationUpdateListener {
        void onLocationUpdate(Location location);
    }

    public boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static void initialize(
            Context context,
            SDKConfig config
    ) {
        sdkConfig = config;
    }

    public static SDKConfig getConfig() {
        return sdkConfig;
    }

}
