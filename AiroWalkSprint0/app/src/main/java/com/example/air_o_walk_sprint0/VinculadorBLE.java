package com.example.air_o_walk_sprint0;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

// --------------------------------------------------------------
 //VinculadorBLE.java
 // Autor : Meryame Ait Boumlik
 //Descripción: Gestiona la “vinculación” por nombre (iBeacon por advertising, sin GATT).
 //Escanea con filtro por nombre, notifica estados a la UI y aplica timeout.
 // --------------------------------------------------------------
public class VinculadorBLE {

    public static final String ETIQUETA_LOG = ">>>>VINCULAR";

    // Estados posibles del proceso de vinculación
    public enum Estado { IDLE, ESCANEANDO, VINCULADO, TIMEOUT, ERROR }

    //Interfaz de callbacks para informar a la Activity/Fragment
    public interface Listener {
        void onEstadoCambio(Estado nuevoEstado);
        void onDispositivoEncontrado(BluetoothDevice device, ScanResult result);
        void onError(int errorCode);
    }
     // ------------------------------------------------------------------
     // Dependencias y estado interno
     // ------------------------------------------------------------------
    private final BluetoothLeScanner scanner;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ScanCallback callback;
    private String objetivoNombre;            // “código” escrito por el usuario (p.ej. GTI)
    private long timeoutMs = 10000;           // por defecto 10 s
    private Estado estado = Estado.IDLE;
    private boolean activo = false;
    private String nombreNodoActual;

    // --------------------------------------------------------------
    // Constructor
    // Descripción: Inyecta el escáner BLE y el listener de la UI.
    // Diseño: scanner , listener -> constructor() ->
    // Parámetros:
    //   - scanner : BluetoothLeScanner ya inicializado
    //   - listener: callbacks de estado y hallazgo de dispositivo
    // --------------------------------------------------------------
    public VinculadorBLE(BluetoothLeScanner scanner, Listener listener) {
        this.scanner = scanner;
        this.listener = listener;
    }
     public Estado getEstado() { return estado; }

     // --------------------------------------------------------------
     // vincularPorNombre()
     // Descripción: inicia escaneo filtrado por nombre; finaliza en VINCULADO/ERROR/TIMEOUT.
     // Diseno: nombre + timeout -> vincularPorNombre() -> VINCULADO | TIMEOUT | ERROR
     // --------------------------------------------------------------
    public void vincularPorNombre(String nombre, long timeoutMs) {
        this.nombreNodoActual = nombre; // guardar nombre actual
        if (scanner == null) {
            Log.e(ETIQUETA_LOG, "No hay scanner BLE disponible");
            cambiarEstado(Estado.ERROR);
            if (listener != null) listener.onError(-1);
            return;
        }
        if (activo) {
            Log.d(ETIQUETA_LOG, "Ya hay un escaneo activo. Lo detengo y reinicio.");
            detener();
        }

        this.objetivoNombre = nombre;
        this.timeoutMs = timeoutMs <= 0 ? 10000 : timeoutMs;

        Log.d(ETIQUETA_LOG, "Iniciando escaneo filtrado por nombre = " + objetivoNombre);

        // Filtro por nombre del dispositivo
        ScanFilter sf = new ScanFilter.Builder().setDeviceName(objetivoNombre).build();
        List<ScanFilter> filtros = new ArrayList<>();
        filtros.add(sf);

        android.bluetooth.le.ScanSettings settings =
                new android.bluetooth.le.ScanSettings.Builder()
                        .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();

        this.callback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                BluetoothDevice device = result.getDevice();
                String name = device != null ? device.getName() : null;



                Log.d(ETIQUETA_LOG, "onScanResult(): visto=" + name + " rssi=" + result.getRssi());

                // Seguridad: algunos dispositivos devuelven null; comprobamos igualdad con el objetivo
                if (name != null && name.equals(objetivoNombre)) {
                    Log.d(ETIQUETA_LOG, "¡OBJETIVO ENCONTRADO! -> " + name);
                    if (listener != null) listener.onDispositivoEncontrado(device, result);

                    // En iBeacon no “conectamos”; consideramos vinculado al recibir el frame
                    cambiarEstado(Estado.VINCULADO);

                    // Detenemos el escaneo para ahorrar batería
                    detener();
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                Log.d(ETIQUETA_LOG, "onBatchScanResults(): " + results.size() + " resultados");
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e(ETIQUETA_LOG, "onScanFailed(): código=" + errorCode);
                cambiarEstado(Estado.ERROR);
                if (listener != null) listener.onError(errorCode);
                detener();
            }
        };

        // Comienza el escaneo
        try {
            scanner.startScan(filtros, settings, callback);
            activo = true;
            cambiarEstado(Estado.ESCANEANDO);
        } catch (Exception e) {
            Log.e(ETIQUETA_LOG, "Excepción al iniciar escaneo: " + e.getMessage());
            cambiarEstado(Estado.ERROR);
            if (listener != null) listener.onError(-2);
            return;
        }

        // Programamos timeout
        handler.postDelayed(() -> {
            if (activo) {
                Log.d(ETIQUETA_LOG, "Timeout sin encontrar: " + objetivoNombre);
                cambiarEstado(Estado.TIMEOUT);
                detener();
            }
        }, this.timeoutMs);
    }
    //()

     // --------------------------------------------------------------
     // detener()
     // Descripción: para el escaneo si está activo y limpia recursos.
     // Diseno : -> detener ->
     // --------------------------------------------------------------
    public void detener() {
        if (!activo) return;
        try {
            scanner.stopScan(callback);
        } catch (Exception e) {
            Log.w(ETIQUETA_LOG, "stopScan lanzó excepción: " + e.getMessage());
        }
        callback = null;
        activo = false;
        // No forzamos estado aquí si ya estamos en VINCULADO/TIMEOUT/ERROR
        if (estado == Estado.ESCANEANDO) {
            cambiarEstado(Estado.IDLE);
        }
    }
//()
// --------------------------------------------------------------
// cambiarEstado()
// Descripción: actualiza el estado interno y notifica al listener.
// Disneo : Estado nuevo -> cambiarEstado() ->
// --------------------------------------------------------------
    private void cambiarEstado(Estado nuevo) {
        this.estado = nuevo;
        Log.d(ETIQUETA_LOG, "Estado -> " + nuevo);
        if (listener != null) listener.onEstadoCambio(nuevo);
    }
// --------------------------------------------------------------
// Getter: nombre del nodo introducido por el usuario para esta vinculación
// --------------------------------------------------------------
    public String getNombreNodoActual() {
        return nombreNodoActual;
    }

}
