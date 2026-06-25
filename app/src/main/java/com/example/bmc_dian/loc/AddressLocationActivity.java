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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_location);
        Log.e(TAG, "AddressLocationActivity: Started");

        statusText = findViewById(R.id.statusText);
        locSdk = new LocSdk(this);

        fetchAddressOnce();
    }

    private void fetchAddressOnce() {
        statusText.setText("Fetching address...");
        locSdk.getCurrentLocation(location -> {
            if (location != null) {
                String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                String address = locSdk.getAddressFromLocation(location.getLatitude(), location.getLongitude());
                
                Log.e(TAG, "================================================");
                Log.e(TAG, "MODE: ADDRESS_LOCATION_FETCH (ONCE)");
                Log.e(TAG, "TIME: " + time);
                Log.e(TAG, "LAT/LNG: " + location.getLatitude() + ", " + location.getLongitude());
                Log.e(TAG, "ADDRESS: " + address);
                Log.e(TAG, "================================================");

                statusText.setText("Address:\n" + address + "\n\nTime: " + time);
                Toast.makeText(AddressLocationActivity.this, "Address Fetched:\n" + address, Toast.LENGTH_LONG).show();
            } else {
                statusText.setText("Failed to fetch address.");
                Toast.makeText(AddressLocationActivity.this, "Failed to fetch address", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "AddressLocationActivity: Stopped");
    }
}
