package com.example.locationsdk;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;

public class BackgroundFetchActivity extends AppCompatActivity {

    public static final String TAG = "LOCATION_TRACKING";
    private Button btnStartService, btnPauseService, btnResumeService, btnStopService;
    private TextView statusText;
    private LocationDbHelper dbHelper;

    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("LocationUpdate".equals(intent.getAction())) {
                double lat = intent.getDoubleExtra("lat", 0);
                double lng = intent.getDoubleExtra("lng", 0);
                String addr = intent.getStringExtra("addr");
                String time = intent.getStringExtra("time");

                String info = "Service Active\nLat: " + lat + "\nLng: " + lng + "\nAddr: " + addr + "\nTime: " + time;
                statusText.setText(info);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_background_fetch);
        dbHelper = new LocationDbHelper(this);

        btnStartService = findViewById(R.id.btnStartService);
        btnPauseService = findViewById(R.id.btnPauseService);
        btnResumeService = findViewById(R.id.btnResumeService);
        btnStopService = findViewById(R.id.btnStopService);
        
        Button btnViewLogs = findViewById(R.id.btnViewLogs);
        Button btnClearLogs = findViewById(R.id.btnClearLogs);

        statusText = findViewById(R.id.statusText);

        btnStartService.setOnClickListener(v -> startMyService());
        btnPauseService.setOnClickListener(v -> sendActionToService(LocationService.ACTION_PAUSE));
        btnResumeService.setOnClickListener(v -> sendActionToService(LocationService.ACTION_RESUME));
        btnStopService.setOnClickListener(v -> stopMyService());

        btnViewLogs.setOnClickListener(v -> dumpDatabaseLogs());
        
        btnClearLogs.setOnClickListener(v -> {
            dbHelper.clearAllLogs();
            File logFile = new File(getExternalFilesDir(null), "OvernightLogs.txt");
            if(logFile.exists()) logFile.delete();
            Toast.makeText(this, "Database & File Cleared", Toast.LENGTH_SHORT).show();
        });

        btnViewLogs.setOnLongClickListener(v -> {
            exportLogFile();
            return true;
        });
    }

    private void exportLogFile() {
        File logFile = new File(getExternalFilesDir(null), "OvernightLogs.txt");
        if (!logFile.exists()) {
            Toast.makeText(this, "No log file found yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", logFile);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Export Overnight Logs"));
    }

    private void dumpDatabaseLogs() {
        Log.wtf(TAG, "!!! [DB DUMP] RECOVERING OVERNIGHT HISTORY !!!");
        Cursor cursor = dbHelper.getAllLogs();
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

    private void sendActionToService(String action) {
        Intent intent = new Intent(this, LocationService.class);
        intent.setAction(action);
        startService(intent);
        Toast.makeText(this, "Action Sent: " + action, Toast.LENGTH_SHORT).show();
    }

    private void startMyService() {
        Intent intent = new Intent(this, LocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
        else startService(intent);
    }

    private void stopMyService() {
        stopService(new Intent(this, LocationService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("LocationUpdate");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(locationReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(locationReceiver, filter);
        }
    }

    @Override protected void onPause() { super.onPause(); unregisterReceiver(locationReceiver); }
}
