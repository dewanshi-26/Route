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

import androidx.annotation.NonNull;
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

//        btnStartService.setOnClickListener(v -> locSdk.startBackgroundService());
        btnStartService.setOnClickListener(v -> {

            locSdk.requestAllPermissionsAndStartService(
                    BackgroundFetchActivity.this
            );

        });

        btnPauseService.setOnClickListener(v -> locSdk.sendActionToService(LocationService.ACTION_PAUSE));
        btnResumeService.setOnClickListener(v -> locSdk.sendActionToService(LocationService.ACTION_RESUME));
        btnStopService.setOnClickListener(v -> locSdk.stopBackgroundService());

        btnViewLogs.setOnClickListener(v -> locSdk.dumpLogs());
        
        btnClearLogs.setOnClickListener(v -> locSdk.clearLogs());

        btnViewLogs.setOnLongClickListener(v -> {
            locSdk.shareLogFile(this);
            return true;
        });
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

        if (requestCode == 1001 ||
                requestCode == 3001 ||
                requestCode == 3002) {

            locSdk.requestAllPermissionsAndStartService(
                    BackgroundFetchActivity.this
            );
        }
    }

}
