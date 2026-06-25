package com.example.locsdk;

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
    public static final String ACTION_STOP = "com.example.locsdk.STOP";
    public static final String ACTION_PAUSE = "com.example.locsdk.PAUSE";
    public static final String ACTION_RESUME = "com.example.locsdk.RESUME";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location lastLocation = null;
    private Location anchorLocation = null; 
    private PowerManager.WakeLock wakeLock;
    private volatile boolean isPaused = false;
    private final Handler repeatLogHandler = new Handler(Looper.getMainLooper());
    private LocationDbHelper dbHelper;

    public final Runnable repeatLogRunnable = new Runnable() {
        @Override
        public void run() {
            logCurrentData("OVERNIGHT_LOG_HEARTBEAT");
            long interval = 60000;
            if (LocSdk.getConfig() != null) {
                interval = LocSdk.getConfig().getInterval();
            }
            repeatLogHandler.postDelayed(this, interval); 
        }
    };

    public final Runnable pcConnectionCheck = new Runnable() {
        @Override
        public void run() {
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            Log.wtf(TAG, ">>> [PC CONNECTION CHECK] If you see this, your PC is receiving logs. Time: " + time);
            repeatLogHandler.postDelayed(this, 10000); 
        }
    };

    public void logCurrentData(String triggerSource) {
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

            float radius = 50.0f;
            if (LocSdk.getConfig() != null) {
                radius = LocSdk.getConfig().getRadius();
            }

            float distance = lastLocation.distanceTo(anchorLocation);
            String statusStr = (distance <= radius) ? "INSIDE " + radius + "m (" + String.format("%.1fm", distance) + ")" : "OUTSIDE " + radius + "m";
            
            double lat = lastLocation.getLatitude();
            double lng = lastLocation.getLongitude();

            final float finalRadius = radius;
            new Thread(() -> {
                String addr = (distance <= finalRadius) ? getAddressFromLocation(lat, lng) : "Hidden";
                
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

                updateNotification(lat, lng, time, distance <= finalRadius);
            }).start();

        } else {
            Log.wtf(TAG, "!!! [DATA LOG] " + time + " : Still waiting for GPS Signal...");
        }
    }

    public String toastText(String time, String status, double lat, double lng, String addr) {
        return String.format("Time: %s\nStatus: %s\nLat: %.5f\nLng: %.5f\nAddr: %s", time, status, lat, lng, addr);
    }

    public void saveLogToFile(String data) {
        try {
            File logFile = new File(getExternalFilesDir(null), "OvernightLogs.txt");
            FileOutputStream fos = new FileOutputStream(logFile, true);
            fos.write((data + "\n").getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to save log to file", e);
        }
    }

    public void showProperToast(String message) {
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

    public String getAddressFromLocation(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) return addresses.get(0).getAddressLine(0);
        } catch (IOException e) { return "Geocoder Error: " + e.getMessage(); }
        return "N/A";
    }

    public void updateNotification(double lat, double lng, String time, boolean inside) {
        String title = "Logging Active (" + time + ")";
        if (LocSdk.getConfig() != null) {
            title = LocSdk.getConfig().getNotificationTitle() + " (" + time + ")";
        }
        
        String content = "Outside 50m - Hidden";
        if (inside) {
            float radius = 50f;
            long interval = 30000;
            if (LocSdk.getConfig() != null) {
                radius = LocSdk.getConfig().getRadius();
                interval = LocSdk.getConfig().getInterval();
            }
            content = String.format(Locale.getDefault(), "Lat: %.5f, Lng: %.5f | %dm | %.0fm", lat, lng, interval / 60000, radius);
        }

        android.widget.RemoteViews remoteViews = new android.widget.RemoteViews(getPackageName(), R.layout.custom_notification);
        remoteViews.setTextViewText(R.id.txtTitle, title);
        remoteViews.setTextViewText(R.id.txtMessage, content);
        remoteViews.setTextViewText(R.id.txtTime, time);

        android.widget.RemoteViews remoteViewsExpanded = new android.widget.RemoteViews(getPackageName(), R.layout.custom_notification_expanded);
        remoteViewsExpanded.setTextViewText(R.id.txtTitleExpanded, title);
        remoteViewsExpanded.setTextViewText(R.id.txtMessageExpanded, content + "\nStatus: " + (isPaused ? "PAUSED" : "RUNNING"));
        remoteViewsExpanded.setTextViewText(R.id.txtTimeExpanded, time);

        if (LocSdk.getConfig() != null) {
            if (LocSdk.getConfig().getNotificationColor() != -1) {
                remoteViews.setInt(R.id.notification_container, "setBackgroundColor", LocSdk.getConfig().getNotificationColor());
                remoteViewsExpanded.setInt(R.id.notification_container_expanded, "setBackgroundColor", LocSdk.getConfig().getNotificationColor());
            }
            if (LocSdk.getConfig().getNotificationImage() != -1) {
                remoteViews.setImageViewResource(R.id.imgLogo, LocSdk.getConfig().getNotificationImage());
                remoteViewsExpanded.setImageViewResource(R.id.imgLogoExpanded, LocSdk.getConfig().getNotificationImage());
            }
        }

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
    public void startTracking() {
        long interval = 30000;
        long fastestInterval = 10000;
        if (LocSdk.getConfig() != null) {
            interval = LocSdk.getConfig().getInterval();
            fastestInterval = LocSdk.getConfig().getFastestInterval();
        }

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
                .setMinUpdateIntervalMillis(fastestInterval)
                .setMaxUpdateDelayMillis(0)
                .build();
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Location Service", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    public void startMyForeground() {
        String title = "Location Service";
        String content = "Initializing...";
        if (LocSdk.getConfig() != null) {
            title = LocSdk.getConfig().getNotificationTitle();
            content = LocSdk.getConfig().getNotificationContent();
        }

        android.widget.RemoteViews remoteViews = new android.widget.RemoteViews(getPackageName(), R.layout.custom_notification);
        remoteViews.setTextViewText(R.id.txtTitle, title);
        remoteViews.setTextViewText(R.id.txtMessage, content);

        if (LocSdk.getConfig() != null) {
            if (LocSdk.getConfig().getNotificationColor() != -1) {
                remoteViews.setInt(R.id.notification_container, "setBackgroundColor", LocSdk.getConfig().getNotificationColor());
            }
            if (LocSdk.getConfig().getNotificationImage() != -1) {
                remoteViews.setImageViewResource(R.id.imgLogo, LocSdk.getConfig().getNotificationImage());
            }
        }

        int icon = R.drawable.ringing;
        if (LocSdk.getConfig() != null && LocSdk.getConfig().getNotificationIcon() != -1) {
            icon = LocSdk.getConfig().getNotificationIcon();
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(icon)
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
