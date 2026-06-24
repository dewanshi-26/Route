package com.example.bmc_dian.loc;

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

import com.example.bmc_dian.R;
import com.example.locsdk.LocSdk;
import com.example.locsdk.LocationDbHelper;
import com.example.locsdk.LocationService;

import java.io.File;

public class BackgroundFetchActivity extends AppCompatActivity {

    public static final String TAG = "LOCATION_TRACKING";
    private TextView statusText;
    private LocationDbHelper dbHelper;
    private LocSdk locSdk;

    public final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
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
        locSdk = new LocSdk(this);

        Button btnStartService = findViewById(R.id.btnStartService);
        Button btnPauseService = findViewById(R.id.btnPauseService);
        Button btnResumeService = findViewById(R.id.btnResumeService);
        Button btnStopService = findViewById(R.id.btnStopService);
        
        Button btnViewLogs = findViewById(R.id.btnViewLogs);
        Button btnClearLogs = findViewById(R.id.btnClearLogs);

        statusText = findViewById(R.id.statusText);

        btnStartService.setOnClickListener(v -> locSdk.startBackgroundService());
        btnPauseService.setOnClickListener(v -> locSdk.sendActionToService(LocationService.ACTION_PAUSE));
        btnResumeService.setOnClickListener(v -> locSdk.sendActionToService(LocationService.ACTION_RESUME));
        btnStopService.setOnClickListener(v -> locSdk.stopBackgroundService());

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

    public void exportLogFile() {
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

    public void dumpDatabaseLogs() {
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
