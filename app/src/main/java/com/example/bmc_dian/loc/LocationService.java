package com.example.bmc_dian.loc;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.bmc_dian.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LocationService extends Service {

    public static final String TAG = "LOCATION_TRACKING"; 
    private static final String CHANNEL_ID = "LocationServiceChannel";
    public static final String ACTION_STOP = "com.example.bmc_dian.loc.STOP";
    public static final String ACTION_PAUSE = "com.example.bmc_dian.loc.PAUSE";
    public static final String ACTION_RESUME = "com.example.bmc_dian.loc.RESUME";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location lastLocation = null;
    private Location anchorLocation = null; 
    private PowerManager.WakeLock wakeLock;
    private volatile boolean isPaused = false;
    private final Handler repeatLogHandler = new Handler(Looper.getMainLooper());
    private LocationDbHelper dbHelper;

    private final Runnable repeatLogRunnable = new Runnable() {
        @Override
        public void run() {
            logCurrentData("OVERNIGHT_LOG_HEARTBEAT");
            repeatLogHandler.postDelayed(this, 60000); 
        }
    };

    private final Runnable pcConnectionCheck = new Runnable() {
        @Override
        public void run() {
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            Log.wtf(TAG, ">>> [PC CONNECTION CHECK] If you see this, your PC is receiving logs. Time: " + time);
            repeatLogHandler.postDelayed(this, 10000); 
        }
    };

    private void logCurrentData(String triggerSource) {
        Date now = new Date();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now);
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now);
        
        Log.i(TAG, "!!! [LOG TRIGGER] Source: " + triggerSource + " | Time: " + time);

        if (isPaused) {
            Log.wtf(TAG, "!!! [DATA LOG] STATUS: PAUSED at " + time);
            return;
        }

        if (lastLocation != null) {
            if (anchorLocation == null) anchorLocation = lastLocation;

            float distance = lastLocation.distanceTo(anchorLocation);
            String statusStr = (distance <= 50.0) ? "INSIDE 50m (" + String.format("%.1fm", distance) + ")" : "OUTSIDE 50m";
            
            double lat = lastLocation.getLatitude();
            double lng = lastLocation.getLongitude();

            new Thread(() -> {
                String addr = (distance <= 50.0) ? getAddressFromLocation(lat, lng) : "Hidden";
                
                String logMsg = String.format("\nDATE: %s\nTIME: %s\nSTATUS: %s\nLAT: %.7f\nLNG: %.7f\nADDR: %s\n", 
                        date, time, statusStr, lat, lng, addr);
                Log.wtf(TAG, "------------------------------------------------");
                Log.wtf(TAG, "!!! [OVERNIGHT DATA BLOCK] !!!" + logMsg);
                Log.wtf(TAG, "------------------------------------------------");

                dbHelper.insertLog(date, time, lat, lng, addr, statusStr);
                saveLogToFile(logMsg);

                Intent intent = new Intent("LocationUpdate");
                intent.putExtra("lat", lat);
                intent.putExtra("lng", lng);
                intent.putExtra("addr", addr);
                intent.putExtra("time", time);
                sendBroadcast(intent);

                showProperToast(toastText(time, statusStr, lat, lng, addr));

                updateNotification(lat, lng, time, distance <= 50.0);
            }).start();

        } else {
            Log.wtf(TAG, "!!! [DATA LOG] " + time + " : Still waiting for GPS Signal...");
        }
    }

    private String toastText(String time, String status, double lat, double lng, String addr) {
        return String.format("Time: %s\nStatus: %s\nLat: %.5f\nLng: %.5f\nAddr: %s", time, status, lat, lng, addr);
    }

    private void saveLogToFile(String data) {
        try {
            File logFile = new File(getExternalFilesDir(null), "OvernightLogs.txt");
            FileOutputStream fos = new FileOutputStream(logFile, true);
            fos.write((data + "\n").getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to save log to file", e);
        }
    }

    private void showProperToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new LocationDbHelper(this);
        
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocationApp:WakeLock");
        if (!wakeLock.isHeld()) wakeLock.acquire();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) lastLocation = location;
            });
        } catch (SecurityException ignored) {}

        createNotificationChannel();
        startMyForeground();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result != null && result.getLastLocation() != null) {
                    lastLocation = result.getLastLocation();
                }
            }
        };
        startTracking();
        repeatLogHandler.post(repeatLogRunnable);
        repeatLogHandler.post(pcConnectionCheck);
        Log.wtf(TAG, "!!! [SERVICE] SYSTEM STARTED - Logcat visibility check initiated !!!");
    }

    private String getAddressFromLocation(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) return addresses.get(0).getAddressLine(0);
        } catch (IOException e) { return "Geocoder Error: " + e.getMessage(); }
        return "N/A";
    }

    private void updateNotification(double lat, double lng, String time, boolean inside) {
        String title = "Logging Active (" + time + ")";
        String content = inside ? String.format("Lat: %.5f, Lng: %.5f | 1min | 50m", lat, lng) : "Outside 50m - Hidden";

        android.widget.RemoteViews remoteViews = new android.widget.RemoteViews(getPackageName(), R.layout.custom_notification);
        remoteViews.setTextViewText(R.id.txtTitle, title);
        remoteViews.setTextViewText(R.id.txtMessage, content);
        remoteViews.setTextViewText(R.id.txtTime, time);

        android.widget.RemoteViews remoteViewsExpanded = new android.widget.RemoteViews(getPackageName(), R.layout.custom_notification_expanded);
        remoteViewsExpanded.setTextViewText(R.id.txtTitleExpanded, title);
        remoteViewsExpanded.setTextViewText(R.id.txtMessageExpanded, content + "\nStatus: " + (isPaused ? "PAUSED" : "RUNNING"));
        remoteViewsExpanded.setTextViewText(R.id.txtTimeExpanded, time);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ringing)
                .setCustomContentView(remoteViews)
                .setCustomBigContentView(remoteViewsExpanded)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, notification);
    }

    @SuppressLint("MissingPermission")
    private void startTracking() {
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30000)
                .setMinUpdateIntervalMillis(10000)
                .setMaxUpdateDelayMillis(0)
                .build();
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Location Service", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void startMyForeground() {
        android.widget.RemoteViews remoteViews = new android.widget.RemoteViews(getPackageName(), R.layout.custom_notification);
        remoteViews.setTextViewText(R.id.txtTitle, "Location Service");
        remoteViews.setTextViewText(R.id.txtMessage, "Initializing...");

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ringing)
                .setCustomContentView(remoteViews)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(1, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startMyForeground();
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Log.d(TAG, "Service received action: " + action);
            if (ACTION_STOP.equals(action)) {
                stopSelf();
                return START_NOT_STICKY;
            } else if (ACTION_PAUSE.equals(action)) {
                isPaused = true;
                showProperToast("Logging Paused");
            } else if (ACTION_RESUME.equals(action)) {
                isPaused = false;
                showProperToast("Logging Resumed");
            }
        }
        logCurrentData("MANUAL_TRIGGER");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wakeLock.isHeld()) wakeLock.release();
        repeatLogHandler.removeCallbacks(repeatLogRunnable);
        repeatLogHandler.removeCallbacks(pcConnectionCheck);
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
