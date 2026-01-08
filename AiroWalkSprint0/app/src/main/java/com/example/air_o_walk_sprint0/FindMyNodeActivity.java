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

/**
 * @class FindMyNodeActivity
 * @brief Activity encargada de localizar un nodo BLE (iBeacon) en tiempo real.
 *
 * Esta actividad escanea anuncios BLE para detectar un iBeacon concreto,
 * estima la distancia mediante valores RSSI filtrados, detecta la pérdida
 * de señal y actualiza la interfaz gráfica mostrando:
 * - Categoría de distancia
 * - Barras de intensidad de señal
 * - Estado visual mediante un círculo de color
 *
 * Además, almacena la última ubicación GPS conocida y permite abrirla
 * en un visor de mapas cuando el nodo desaparece.
 *
 * NUEVO: Integración con NotifEstadoNodo para mostrar notificaciones
 * cuando el sensor está fuera de rango o desconectado.
 *
 * @author Meryame Ait Boumlik y equipo Air-o-Walk
 * @version 1.1
 */
public class FindMyNodeActivity extends AppCompatActivity {

    private static final String TAG = "FIND_NODE";
    private static final int GPS_PERMISSION_REQUEST = 1234;

    // ------------------------------------------------------------------
    // BLE
    // ------------------------------------------------------------------
    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;

    // ------------------------------------------------------------------
    // Distancia / Estimador
    // ------------------------------------------------------------------
    private DistanceEstimator estimator;
    private String nodeName;

    // ------------------------------------------------------------------
    // Referencias UI
    // ------------------------------------------------------------------
    private TextView txtDistance;
    private ImageView imgBars;
    private View circle;
    private Button btnLastLocation;

    // ------------------------------------------------------------------
    // Control de visibilidad y tiempo del beacon
    // ------------------------------------------------------------------
    private boolean nodeVisible = false;
    private long lastSeenTimestamp = 0;

    // ------------------------------------------------------------------
    // GPS
    // ------------------------------------------------------------------
    private LocationManager locationManager;
    private LocationListener gpsListener;
    private Location lastLocation = null;

    // Thread que vigila si el nodo desaparece por timeout dinámico
    private Thread rangeCheckerThread;

    // ------------------------------------------------------------------
    // NUEVO: Sistema de notificaciones
    // ------------------------------------------------------------------
    private NotifEstadoNodo monitorEstadoNodo;
    private boolean yaNotificoFueraDeRango = false;
    private static final long TIMEOUT_INICIAL_MS = 10000; // 10 segundos para primera detección

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_node);

        // Get beacon name
        nodeName = getIntent().getStringExtra("NODE_NAME");

        Log.d(TAG, "Buscando nodo: " + nodeName);

        estimator = new DistanceEstimator();

        // NUEVO: Inicializar monitor de notificaciones
        if (nodeName != null && !nodeName.isEmpty()) {
            monitorEstadoNodo = new NotifEstadoNodo(getApplicationContext(), nodeName);
        }

        // Enlazar UI
        txtDistance = findViewById(R.id.txtDistance);
        imgBars = findViewById(R.id.imgSignalBars);
        circle = findViewById(R.id.distanceCircle);
        btnLastLocation = findViewById(R.id.btnLastLocation);

        Button btnBack = findViewById(R.id.btnVolver);
        btnBack.setOnClickListener(v -> finish());

        // Botón que abre última ubicación GPS detectada
        btnLastLocation.setOnClickListener(v -> {
            if (lastLocation != null) {
                String uri = "geo:" + lastLocation.getLatitude() + "," + lastLocation.getLongitude();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
            }
        });

        // Inicializar escáner BLE
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            scanner = btAdapter.getBluetoothLeScanner();
        }

        initGPS();
        startScanning();
    }

    // --------------------------------------------------------------
    // initGPS()
    // Descripción:
    //   - Solicita permisos si falta alguno
    //   - Crea listener que actualiza lastLocation
    //   - Comienza a recibir actualizaciones GPS
    // --------------------------------------------------------------
    private void initGPS() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Request permissions if needed
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    },
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

    private void startScanning() {

        if (scanner == null) {
            Log.e(TAG, "BluetoothLeScanner is null. Is Bluetooth enabled?");
            txtDistance.setText("Bluetooth apagado");
            return;
        }

        nodeVisible = false;
        yaNotificoFueraDeRango = false;
        showOutOfRange();

        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {

                if (result.getScanRecord() == null) return;

                byte[] scanData = result.getScanRecord().getBytes();
                TramaIBeacon tib = new TramaIBeacon(scanData);

                String uuidText = Utilidades.bytesToString(tib.getUUID());

                Log.d(TAG, "UUID recibido = " + uuidText + " buscando=" + nodeName);

                // MATCH IF nodeName IS ANY SUBSTRING OF THE UUID
                if (!uuidText.contains(nodeName)) {
                    return;
                }

                int rssi = result.getRssi();
                estimator.addReading(rssi);

                // NUEVO: Si estaba fuera de rango y ahora lo detectamos, resetear flag
                if (!nodeVisible) {
                    Log.d(TAG, "¡Nodo ENCONTRADO! RSSI: " + rssi);
                    yaNotificoFueraDeRango = false;
                }

                nodeVisible = true;
                lastSeenTimestamp = System.currentTimeMillis();

                updateUI();
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e(TAG, "Error en escaneo BLE: " + errorCode);

                // NUEVO: Notificar al usuario del error
                runOnUiThread(() -> {
                    txtDistance.setText("Error al buscar sensor");
                });
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

    // --------------------------------------------------------------
    // startRangeCheckerThread()
    // Descripción:
    //   - Hilo secundario que evalúa cada 500ms si el nodo dejó de emitir
    //   - Timeout dinámico según la intensidad RSSI promedio
    //   - Si pasa el timeout → nodo considerado "Fuera de rango"
    //   - NUEVO: Notifica después de TIMEOUT_INICIAL_MS si nunca se detectó
    // --------------------------------------------------------------
    private void startRangeCheckerThread() {
        rangeCheckerThread = new Thread(() -> {
            long startTime = System.currentTimeMillis();

            while (!isFinishing()) {

                long now = System.currentTimeMillis();
                long timeout;

                float rssi = estimator.getFilteredRSSI();

                if (rssi > -60) {
                    timeout = 8000;    // close → strict timeout
                } else if (rssi > -70) {
                    timeout = 5000;    // medium → moderate timeout
                } else if (rssi > -78) {
                    timeout = 3000;    // 2–3m → weak reception
                } else {
                    timeout = 2000;    // far → packets often lost
                }

                // NUEVO: Si nunca se detectó el nodo y ya pasó el timeout inicial
                if (!nodeVisible && !yaNotificoFueraDeRango) {
                    long tiempoBuscando = now - startTime;

                    if (tiempoBuscando > TIMEOUT_INICIAL_MS) {
                        Log.w(TAG, "Nodo NO encontrado después de " + tiempoBuscando + "ms");
                        yaNotificoFueraDeRango = true;

                        // Notificar que está fuera de rango
                        if (monitorEstadoNodo != null) {
                            monitorEstadoNodo.notificarFueraDeRango();
                            Log.d(TAG, "Notificación de 'fuera de rango' enviada");
                        }
                    }
                }

                // Si el nodo estaba visible pero ahora desapareció
                if (nodeVisible && (now - lastSeenTimestamp > timeout)) {
                    nodeVisible = false;
                    yaNotificoFueraDeRango = true;

                    Log.w(TAG, "Nodo perdido — last known location saved");

                    // NUEVO: Notificar que se perdió la conexión
                    if (monitorEstadoNodo != null) {
                        monitorEstadoNodo.notificarFueraDeRango();
                        Log.d(TAG, "Notificación de 'fuera de rango' enviada por pérdida de señal");
                    }

                    runOnUiThread(this::showOutOfRange);
                }

                try {
                    Thread.sleep(500);
                } catch (Exception ignore) {}
            }
        });

        rangeCheckerThread.start();
    }

    // --------------------------------------------------------------
    // updateUI()
    // Descripción:
    //   - Actualiza texto, color del círculo e icono de barras
    //   - Se llama cada vez que se recibe un frame válido del beacon
    // --------------------------------------------------------------
    private void updateUI() {
        String category = estimator.getDistanceCategory();
        int level = estimator.getSignalLevel();

        Log.d(TAG, "UI actualizada - Distancia: " + category + " | Nivel: " + level);

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
    // showOutOfRange() - NODE DISAPPEARED
    // MODIFICADO: Añade logging para debugging
    // -----------------------------------------------------------
    private void showOutOfRange() {
        Log.w(TAG, "Mostrando 'Fuera de rango' en UI");

        runOnUiThread(() -> {
            txtDistance.setText("Fuera de rango");
            circle.setBackgroundResource(R.drawable.circle_cold);

            if (lastLocation != null) {
                btnLastLocation.setVisibility(View.VISIBLE);
                Log.d(TAG, "Botón de última ubicación visible");
            } else {
                btnLastLocation.setVisibility(View.GONE);
                Log.d(TAG, "No hay última ubicación GPS disponible");
            }

            imgBars.setImageResource(R.drawable.ic_signal_0);
        });
    }

    // --------------------------------------------------------------
    // onDestroy()
    // Descripción:
    //   - Limpia GPS, BLE scanning y el hilo vigía
    //   - NUEVO: Limpia recursos de notificaciones
    // --------------------------------------------------------------
    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Destruyendo FindMyNodeActivity");

        if (scanner != null && scanCallback != null) {
            scanner.stopScan(scanCallback);
            Log.d(TAG, "Escaneo BLE detenido");
        }

        if (locationManager != null && gpsListener != null) {
            locationManager.removeUpdates(gpsListener);
            Log.d(TAG, "GPS detenido");
        }

        if (rangeCheckerThread != null) {
            rangeCheckerThread.interrupt();
            Log.d(TAG, "Thread de verificación detenido");
        }

        // NUEVO: No detener el monitor aquí porque las notificaciones deben persistir
        // El monitor se maneja desde MainActivity
        monitorEstadoNodo = null;
    }
}