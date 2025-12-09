package com.example.air_o_walk_sprint0;

import static com.example.air_o_walk_sprint0.Utilidades.bytesToHexString;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.*;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;

import java.util.ArrayList;

public class FindMyNodeActivity extends AppCompatActivity {

    private static final String TAG = "FIND_NODE";
    private static final int GPS_PERMISSION_REQUEST = 1234;

    // BLE
    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;

    // Estimator
    private DistanceEstimator estimator;
    private String nodeName;

    // UI references
    private TextView txtDistance;
    private ImageView imgBars;
    private View circle;
    private Button btnLastLocation;

    // Node detection & timing
    private boolean nodeVisible = false;
    private long lastSeenTimestamp = 0;

    // GPS
    private LocationManager locationManager;
    private LocationListener gpsListener;
    private Location lastLocation = null;

    private Thread rangeCheckerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_node);

        // Get beacon name
        nodeName = getIntent().getStringExtra("NODE_NAME");

        estimator = new DistanceEstimator();

        // Bind UI
        txtDistance = findViewById(R.id.txtDistance);
        imgBars = findViewById(R.id.imgSignalBars);
        circle = findViewById(R.id.distanceCircle);
        btnLastLocation = findViewById(R.id.btnLastLocation);

        Button btnBack = findViewById(R.id.btnVolver);
        btnBack.setOnClickListener(v -> finish());

        btnLastLocation.setOnClickListener(v -> {
            if (lastLocation != null) {
                String uri = "geo:" + lastLocation.getLatitude() + "," + lastLocation.getLongitude();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
            }
        });

        // BLE scanner
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null)
            scanner = btAdapter.getBluetoothLeScanner();

        initGPS();
        startScanning();
    }

    // -----------------------------------------------------------
    // GPS INITIALIZATION
    // -----------------------------------------------------------
    private void initGPS() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Request permissions if needed
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    GPS_PERMISSION_REQUEST
            );
            return;
        }

        // Create listener
        gpsListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                lastLocation = location;
                Log.d(TAG, "GPS location updated: " + location.getLatitude() + ", " + location.getLongitude());
            }
        };

        // Start GPS updates
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000,
                    1,
                    gpsListener
            );
        } catch (Exception e) {
            Log.e(TAG, "GPS requestLocationUpdates failed: " + e.getMessage());
        }
    }

    // Handle GPS permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == GPS_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initGPS();
            } else {
                Log.e(TAG, "GPS permission denied");
            }
        }
    }

    // -----------------------------------------------------------
    // BLE SCANNING
    // -----------------------------------------------------------
    private void startScanning() {

        if (scanner == null) {
            Log.e(TAG, "BluetoothLeScanner is null. Is Bluetooth enabled?");
            txtDistance.setText("Bluetooth apagado");
            return;
        }

        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice dev = result.getDevice();

                byte[] scanData = result.getScanRecord().getBytes();

// The ESP32 iBeacon always contains your beacon name inside the UUID bytes,
// but NOT inside device.getName()

                String raw = bytesToHexString(scanData);
                if (!raw.contains(nodeName)) return;   // Now this detects the correct device


                int rssi = result.getRssi();
                estimator.addReading(rssi);

                nodeVisible = true;
                lastSeenTimestamp = System.currentTimeMillis();

                updateUI();
            }
        };

        ArrayList<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setDeviceName(nodeName).build());

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        scanner.startScan(null, settings, scanCallback);


        startRangeCheckerThread();
    }

    // -----------------------------------------------------------
    // THREAD: CHECK IF NODE DISAPPEARS
    // -----------------------------------------------------------
    private void startRangeCheckerThread() {
        rangeCheckerThread = new Thread(() -> {
            while (!isFinishing()) {
                long now = System.currentTimeMillis();

                if (nodeVisible && now - lastSeenTimestamp > 3000) {
                    nodeVisible = false;

                    Log.d(TAG, "Node out of range — last known location saved");

                    runOnUiThread(this::showOutOfRange);
                }

                try {
                    Thread.sleep(500);
                } catch (Exception ignore) {}
            }
        });

        rangeCheckerThread.start();
    }

    // -----------------------------------------------------------
    // UPDATE UI WHEN BEACON IS FOUND
    // -----------------------------------------------------------
    private void updateUI() {
        String category = estimator.getDistanceCategory();
        int level = estimator.getSignalLevel();

        runOnUiThread(() -> {

            txtDistance.setText("Distancia: " + category);

            switch (category) {
                case "Muy cerca":
                    circle.setBackgroundResource(R.drawable.circle_hot);
                    break;
                case "Cerca":
                    circle.setBackgroundResource(R.drawable.circle_warm);
                    break;
                default:
                    circle.setBackgroundResource(R.drawable.circle_cold);
            }

            imgBars.setImageResource(
                    getResources().getIdentifier("ic_signal_" + level, "drawable", getPackageName())
            );

            btnLastLocation.setVisibility(View.GONE);
        });
    }

    // -----------------------------------------------------------
    // NODE DISAPPEARED
    // -----------------------------------------------------------
    private void showOutOfRange() {
        txtDistance.setText("Fuera de rango");
        circle.setBackgroundResource(R.drawable.circle_cold);
        btnLastLocation.setVisibility(View.VISIBLE);
    }

    // -----------------------------------------------------------
    // CLEANUP
    // -----------------------------------------------------------
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (scanner != null && scanCallback != null) {
            scanner.stopScan(scanCallback);
        }

        if (locationManager != null && gpsListener != null) {
            locationManager.removeUpdates(gpsListener);
        }

        if (rangeCheckerThread != null) {
            rangeCheckerThread.interrupt();
        }
    }
}
