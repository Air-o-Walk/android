package com.example.air_o_walk_sprint0;

// ------------------------------------------------------------------
// Imports necesarios para Bluetooth, permisos, logging y concurrencia
// ------------------------------------------------------------------

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import com.google.android.material.navigation.NavigationView;

/**
 * @class MainActivity
 * @brief Actividad principal de la aplicación Air-o-Walk.
 *
 * Esta actividad centraliza la mayor parte de la funcionalidad de la app:
 * - Gestión y escaneo de dispositivos BLE (beacons)
 * - Vinculación y desvinculación de nodos sensores
 * - Recepción y procesamiento de mediciones ambientales (O3, CO, NO2)
 * - Control del estado de conexión del beacon
 * - Tracking de recorridos (pasos, tiempo y ubicación GPS)
 * - Envío de mediciones completas al backend
 * - Integración con gamificación, canjeos y resumen de calidad del aire
 *
 * Además, gestiona:
 * - Permisos dinámicos (Bluetooth, localización, actividad física, notificaciones)
 * - Manejo de desconexiones abruptas del beacon
 *
 * Diseño general:
 * onCreate()
 *  → inicializarBlueTooth()
 *  → inicializarVinculador()
 *  → inicializar trackers (pasos, tiempo, GPS)
 *  → escaneo BLE y recepción de mediciones
 *  → startTracking() / stopTracking()
 *  → envío de datos al backend
 *
 * @author
 * Equipo Air-o-Walk
 * @version 1.0
 */


public class MainActivity extends BaseActivity  {

    // Etiqueta para los logs
    private static final String ETIQUETA_LOG = ">>>>";

    // Código para la petición de permisos
    private static final int CODIGO_PETICION_PERMISOS = 11223344;

    // Escáner BLE y callback para resultados de escaneo
    private BluetoothLeScanner elEscanner;
    private ScanCallback callbackDelEscaneo = null;

    // Variables para evitar duplicados y controlar el flujo de mediciones
    private int contadorAndroid = 0;
    private boolean recibioGas = false;
    private boolean recibioTemperatura = false;

    // Monitor del estado del nodo (conectado / desconectado / incoherencias)
    private NotifEstadoNodo monitorEstadoNodo;

    // Referencias a las vistas
    private TextView textMajor;
    private TextView textMinor;
    private TextView textSteps;
    private TextView tiempoTotal;
    private Button trackButton;

    //Variables vinculacion
    private VinculadorBLE vinculador;
    private ImageView iconoVincular;
    private boolean yaVinculado = false;
    private String nombreNodoVinculado = null;
    private int nodeId = 153;

    // Trackers para pasos, tiempo y GPS
    private StepCounterTracker stepTracker;
    private WalkingTimeTracker timeTracker;
    private GPSFondo gpsTracker;

    private int idUsuario;
    private String token;

    // Estado del tracking
    private boolean isTracking = false;

    // NUEVO: Estado de conexión del beacon
    private boolean beaconConectado = false;

    // NUEVO: Última ubicación conocida del nodo
    private Location ultimaUbicacionNodo = null;

    // NUEVO: Últimas mediciones de gases
    private float ultimaMedicionO3 = 0f;
    private float ultimaMedicionCO = 0f;
    private float ultimaMedicionNO2 = 0f;

    // Drawer / Navigation
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView btnMenu;

    private TextView textVinculacion;
    private View btnVincular;



    // ------------------------------------------------------------------
    // Escanea todos los dispositivos BLE cercanos y muestra su información
    // ------------------------------------------------------------------
    private void buscarTodosLosDispositivosBTLE() {
        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): empieza ");

        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): instalamos scan callback ");

        this.callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult( int callbackType, ScanResult resultado ) {
                super.onScanResult(callbackType, resultado);
                Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): onScanResult() ");

                mostrarInformacionDispositivoBTLE( resultado );
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): onBatchScanResults() ");
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): onScanFailed() ");
            }
        };

        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): empezamos a escanear ");

        this.elEscanner.startScan( this.callbackDelEscaneo);

    } // ()

    // ------------------------------------------------------------------
    // Muestra información detallada del dispositivo BLE detectado
    // Incluye datos de la trama iBeacon y valores de medición
    // ------------------------------------------------------------------
    private void mostrarInformacionDispositivoBTLE( ScanResult resultado ) {

        BluetoothDevice bluetoothDevice = resultado.getDevice();
        byte[] bytes = resultado.getScanRecord().getBytes();
        int rssi = resultado.getRssi();

        Log.d(ETIQUETA_LOG, " ****************************************************");
        Log.d(ETIQUETA_LOG, " ****** DISPOSITIVO DETECTADO BTLE ****************** ");
        Log.d(ETIQUETA_LOG, " ****************************************************");
        Log.d(ETIQUETA_LOG, " nombre = " + bluetoothDevice.getName());
        Log.d(ETIQUETA_LOG, " toString = " + bluetoothDevice.toString());

        Log.d(ETIQUETA_LOG, " dirección = " + bluetoothDevice.getAddress());
        Log.d(ETIQUETA_LOG, " rssi = " + rssi );

        Log.d(ETIQUETA_LOG, " bytes = " + new String(bytes));
        Log.d(ETIQUETA_LOG, " bytes (" + bytes.length + ") = " + Utilidades.bytesToHexString(bytes));

        // Procesa la trama iBeacon para extraer información relevante
        TramaIBeacon tib = new TramaIBeacon(bytes);

        Log.d(ETIQUETA_LOG, " ----------------------------------------------------");
        Log.d(ETIQUETA_LOG, " prefijo  = " + Utilidades.bytesToHexString(tib.getPrefijo()));
        Log.d(ETIQUETA_LOG, "          advFlags = " + Utilidades.bytesToHexString(tib.getAdvFlags()));
        Log.d(ETIQUETA_LOG, "          advHeader = " + Utilidades.bytesToHexString(tib.getAdvHeader()));
        Log.d(ETIQUETA_LOG, "          companyID = " + Utilidades.bytesToHexString(tib.getCompanyID()));
        Log.d(ETIQUETA_LOG, "          iBeacon type = " + Integer.toHexString(tib.getiBeaconType()));
        Log.d(ETIQUETA_LOG, "          iBeacon length 0x = " + Integer.toHexString(tib.getiBeaconLength()) + " ( "
                + tib.getiBeaconLength() + " ) ");
        Log.d(ETIQUETA_LOG, " uuid  = " + Utilidades.bytesToHexString(tib.getUUID()));
        Log.d(ETIQUETA_LOG, " uuid  = " + Utilidades.bytesToString(tib.getUUID()));

        byte[] major = tib.getMajor();
        Log.d(ETIQUETA_LOG, " major  = " + Utilidades.bytesToHexString(major) + "( "
                + Utilidades.bytesToInt(major) + " ) ");

        int tipoMedicion = major[0] & 0xFF ;
        Log.d(ETIQUETA_LOG, " tipo medicion  = " + tipoMedicion);

        int contador = major[1] & 0xFF;
        Log.d(ETIQUETA_LOG, " contador  = " + contador);

        Log.d(ETIQUETA_LOG, " minor  = " + Utilidades.bytesToHexString(tib.getMinor()) + "( "
                + Utilidades.bytesToInt(tib.getMinor()) + " ) ");

        Log.d(ETIQUETA_LOG, " medicion  = " + Utilidades.bytesToInt(tib.getMinor()));

        Log.d(ETIQUETA_LOG, " txPower  = " + Integer.toHexString(tib.getTxPower()) + " ( " + tib.getTxPower() + " )");
        Log.d(ETIQUETA_LOG, " ****************************************************");

    } // ()

    // ------------------------------------------------------------------
    // Escanea solo el dispositivo BLE con el nombre especificado
    // Utiliza filtros y modo de escaneo rápido
    // ------------------------------------------------------------------
    private void buscarEsteDispositivoBTLE(final String dispositivoBuscado) {
        Log.d(ETIQUETA_LOG, " buscarEsteDispositivoBTLE(): empieza ");

        Log.d(ETIQUETA_LOG, "  buscarEsteDispositivoBTLE(): instalamos scan callback ");

        this.callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult( int callbackType, ScanResult resultado ) {
                super.onScanResult(callbackType, resultado);
                Log.d(ETIQUETA_LOG, "  buscarEsteDispositivoBTLE(): onScanResult() ");

                mostrarInformacionDispositivoBTLE( resultado );
                mostrarMedicion(resultado);


            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                Log.d(ETIQUETA_LOG, "  buscarEsteDispositivoBTLE(): onBatchScanResults() ");
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.d(ETIQUETA_LOG, "  buscarEsteDispositivoBTLE(): onScanFailed() ");
            }
        };

        // Filtro por nombre de dispositivo
        ScanFilter sf = new ScanFilter.Builder().setDeviceName( dispositivoBuscado ).build();
        List<ScanFilter> filtros = new java.util.ArrayList<>();
        filtros.add(sf);

        // Configuración de escaneo (modo rápido, baja latencia)
        android.bluetooth.le.ScanSettings settings =
                new android.bluetooth.le.ScanSettings.Builder()
                        .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();

        Log.d(ETIQUETA_LOG, "  buscarEsteDispositivoBTLE(): empezamos a escanear buscando: " + dispositivoBuscado );

        this.elEscanner.startScan(filtros, settings, this.callbackDelEscaneo );
    } // ()

    // ------------------------------------------------------------------
    // Detiene el escaneo de dispositivos BLE
    // ------------------------------------------------------------------
    private void detenerBusquedaDispositivosBTLE() {

        if ( this.callbackDelEscaneo == null ) {
            return;
        }

        this.elEscanner.stopScan( this.callbackDelEscaneo );
        this.callbackDelEscaneo = null;

    } // ()

    private void mostrarMedicion( ScanResult resultado){

        // Obtiene los bytes crudos del advertising packet
        byte[] bytes = resultado.getScanRecord().getBytes();

        // Parsea los bytes como trama iBeacon
        TramaIBeacon tib = new TramaIBeacon(bytes);

        // Extrae el major (2 bytes) que contiene info codificada
        byte[] major = tib.getMajor();
        float medicionGas = Utilidades.bytesToFloat(major)/100;

        byte[] minor = tib.getMinor();
        float medicionTemperatura = Utilidades.bytesToFloat(minor)/100;

        // Segundo byte del major = CONTADOR
        // Se incrementa con cada nueva medición del Arduino
        // Permite detectar cuando llega una medición nueva vs. repetida
        int contadorArduino = tib.getTxPower();

        // VERIFICACIÓN DE DUPLICADOS:
        // Si el contador es igual al anterior, es la misma medición
        // Los beacons transmiten continuamente, así que recibiremos
        // el mismo paquete varias veces hasta que Arduino envíe uno nuevo
        if ( contadorArduino == this.contadorAndroid ) {
            Log.d(ETIQUETA_LOG, "Se repitio el contador no se envia este becon");
            return; // Salir sin guardar (es duplicado)
        }

        // Si llegamos aquí, es una medición NUEVA
        // Actualizamos nuestro contador local para futuras comparaciones
        this.contadorAndroid = contadorArduino;

        // NUEVO: Marcar beacon como conectado
        if (!beaconConectado) {
            beaconConectado = true;
            Log.d(ETIQUETA_LOG, " Beacon CONECTADO - Funcionalidades habilitadas");
        }

        // NUEVO: Guardar últimas mediciones de gases
        // Asumiendo que medicionGas es O3 (ozono)
        ultimaMedicionO3 = medicionGas;
        // TODO: Si tienes sensores para CO y NO2, actualízalos aquí
        // ultimaMedicionCO = ...;
        // ultimaMedicionNO2 = ...;
        // Por ahora usamos valores simulados para CO y NO2
        ultimaMedicionCO = medicionGas * 700;  // Reemplazar con valor real si está disponible
        ultimaMedicionNO2 = medicionGas * 700; // Reemplazar con valor real si está disponible

        // NUEVO: Actualizar última ubicación conocida del nodo
        if (gpsTracker != null) {
            Location ubicacionActual = gpsTracker.getCurrentLocation();
            if (ubicacionActual != null) {
                ultimaUbicacionNodo = ubicacionActual;
                Log.d(ETIQUETA_LOG, " Última ubicación nodo actualizada: " +
                        ubicacionActual.getLatitude() + ", " + ubicacionActual.getLongitude());
            }
        }

        // -----------------------------------------------------------
        // Llamamos al monitor del nodo (conectado / desconectado / incoherente)
        // -----------------------------------------------------------
        if (monitorEstadoNodo != null) {
            monitorEstadoNodo.onBeaconRecibido(medicionGas, medicionTemperatura);
        }

        enviarUltimaUbicacionNodo();

        // Llamamos a la notificación desde el hilo principal (UI thread)
        runOnUiThread(() -> {
            AdminNotificaciones.revisarYNotificar(this, medicionGas);
            textMajor.setText("O3(ppm): " + medicionGas);
            textMinor.setText("Temperatura(ºC): " + medicionTemperatura);
        });
    }

    // ------------------------------------------------------------------
    // MODIFICADO: Envía las 3 mediciones completas (ubicación, pasos, tiempo)
    // ------------------------------------------------------------------
    private void enviarUltimaUbicacionNodo() {
        if (ultimaUbicacionNodo == null) {
            Log.w(ETIQUETA_LOG, " enviarMedicionesCompletas(): No hay última ubicación disponible");
            return;
        }

        if (nombreNodoVinculado == null || nombreNodoVinculado.isEmpty()) {
            Log.e(ETIQUETA_LOG, " enviarMedicionesCompletas(): No hay nodo vinculado");
            return;
        }

        // Obtener pasos totales de la sesión
        int pasosTotal = 0;
        if (stepTracker != null) {
            pasosTotal = stepTracker.getSteps();
        }

        // Obtener tiempo total en segundos
        long tiempoTotalSegundos = 0;
        if (timeTracker != null) {
            tiempoTotalSegundos = timeTracker.getElapsedTimeSeconds();
        }

        Log.d(ETIQUETA_LOG, " ===============================================");
        Log.d(ETIQUETA_LOG, " Preparando envío de mediciones completas:");
        Log.d(ETIQUETA_LOG, " - Nodo: " + nombreNodoVinculado);
        Log.d(ETIQUETA_LOG, " - Pasos: " + pasosTotal);
        Log.d(ETIQUETA_LOG, " - Tiempo: " + tiempoTotalSegundos + " segundos");
        Log.d(ETIQUETA_LOG, " - Ubicación: " + ultimaUbicacionNodo.getLatitude() +
                ", " + ultimaUbicacionNodo.getLongitude());
        Log.d(ETIQUETA_LOG, " ===============================================");

        // Usar MeasurementsSender para enviar todas las mediciones
        MeasurementsSender.enviarMedicionCompleta(
                nodeId,
                ultimaMedicionO3,
                ultimaMedicionCO,
                ultimaMedicionNO2,
                ultimaUbicacionNodo,
                new MeasurementsSender.MeasurementCallback() {
                    @Override
                    public void onSuccess(String respuesta) {
                        Log.d(ETIQUETA_LOG, " Mediciones completas enviadas exitosamente");
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(ETIQUETA_LOG, " Error enviando mediciones: " + error);
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this,
                                    "No se pudieron enviar los datos.\n\nVerifica tu conexión a internet y vuelve a intentar.",
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                }
        );
    }

    // ------------------------------------------------------------------
    // Métodos que se vinculan a los botones de la interfaz
    // ------------------------------------------------------------------
    public void botonBuscarDispositivosBTLEPulsado( View v ) {
        Log.d(ETIQUETA_LOG, " boton buscar dispositivos BTLE Pulsado" );
        this.buscarTodosLosDispositivosBTLE();
    } // ()

    public void botonBuscarNuestroDispositivoBTLEPulsado( View v ) {
        Log.d(ETIQUETA_LOG, " boton nuestro dispositivo BTLE Pulsado" );
        // MODIFICADO: Solo permitir si hay beacon vinculado
        if (!yaVinculado) {
            Toast.makeText(this,
                    "Necesitas vincular un sensor primero.\n\nToca el ícono de enlace en el menú superior para comenzar.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        this.buscarEsteDispositivoBTLE(nombreNodoVinculado);
    } // ()

    public void botonDetenerBusquedaDispositivosBTLEPulsado( View v ) {
        Log.d(ETIQUETA_LOG, " boton detener busqueda dispositivos BTLE Pulsado" );
        this.detenerBusquedaDispositivosBTLE();

        // NUEVO: Al detener búsqueda, marcar beacon como desconectado
        if (beaconConectado) {
            beaconConectado = false;
            enviarUltimaUbicacionNodo();
            Log.d(ETIQUETA_LOG, " Beacon DESCONECTADO - Funcionalidades pausadas");
        }
    } // ()


    public void abrirPantallaGamificacion(View v) {
        // MODIFICADO: Solo permitir si beacon está conectado
        if (!beaconConectado) {
            Toast.makeText(this,
                    "No hay sensor conectado.\n\nEspera a que se detecte tu sensor o enciéndelo para continuar.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(MainActivity.this, GamificacionActivity.class);
        intent.putExtra("USER_ID", idUsuario);
        startActivity(intent);
    }

    public void abrirPantallaCanjeos(View v) {
        // MODIFICADO: Solo permitir si beacon está conectado
        if (!beaconConectado) {
            Toast.makeText(this,
                    "No hay sensor conectado.\n\nConecta tu sensor para acceder a los canjeos.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(MainActivity.this, CanjeoActivity.class);
        intent.putExtra("USER_ID", idUsuario);
        startActivity(intent);
    }

    // ------------------------------------------------------------------
    // MODIFICADO: Control de tracking ahora depende de la conexión del beacon
    // ------------------------------------------------------------------
    public void botonDistanceTrackerPulsado(View v) {
        // NUEVO: Verificar que el beacon esté conectado antes de iniciar tracking
        if (!beaconConectado && !isTracking) {
            new AlertDialog.Builder(this)
                    .setTitle("Sensor no conectado")
                    .setMessage(
                            "Para iniciar un recorrido necesitas tener tu sensor conectado.\n\n" +
                                    "¿Qué hacer?\n" +
                                    "• Verifica que el sensor esté encendido\n" +
                                    "• Acércate al sensor si estás lejos\n" +
                                    "• Espera unos segundos a que se detecte"
                    )
                    .setPositiveButton("Entendido", null)
                    .setNeutralButton("¿Cómo vincular?", (d, w) -> {
                        // Mostrar tutorial de vinculación
                        mostrarTutorialVinculacion();
                    })
                    .show();
            return;
        }

        if (!isTracking) {
            startTracking();
        } else {
            stopTracking();
        }
    }

    // Nuevo método auxiliar para el tutorial
    private void mostrarTutorialVinculacion() {
        new AlertDialog.Builder(this)
                .setTitle("¿Cómo vincular mi sensor?")
                .setMessage(
                        "1. Toca el ícono de enlace en el menú superior\n\n" +
                                "2. Ingresa el nombre de tu sensor (ej: GTI)\n\n" +
                                "3. Espera a que se detecte y se conecte\n\n" +
                                "4. ¡Listo! Ya puedes iniciar recorridos"
                )
                .setPositiveButton("Entendido", null)
                .show();
    }

    private void startTracking() {
        Log.d(ETIQUETA_LOG, " startTracking(): iniciando tracking de pasos, tiempo y GPS");

        if (stepTracker == null || timeTracker == null || gpsTracker == null) {
            Log.e(ETIQUETA_LOG, " startTracking(): Error - trackers no inicializados");

            // ANTES:
            // Toast.makeText(this, "Error: trackers no inicializados", Toast.LENGTH_SHORT).show();

            // AHORA:
            Toast.makeText(this,
                    "Los sensores del dispositivo no están listos.\n\nReinicia la aplicación e intenta nuevamente.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (!stepTracker.isStepCounterAvailable()) {
            Log.e(ETIQUETA_LOG, " startTracking(): Error - no hay sensor de pasos disponible");

            // ANTES:
            // Toast.makeText(this, "Este dispositivo no tiene sensor de pasos", Toast.LENGTH_LONG).show();

            // AHORA:
            Toast.makeText(this,
                    "Tu dispositivo no tiene sensor de pasos.\n\nNo podrás registrar recorridos en este teléfono.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (!beaconConectado) {
            Log.e(ETIQUETA_LOG, " startTracking(): Error - beacon no conectado");

            // ANTES:
            // Toast.makeText(this, "Beacon no conectado. Esperando señal...", Toast.LENGTH_SHORT).show();

            // AHORA:
            Toast.makeText(this,
                    "Esperando conexión con el sensor...\n\nAsegúrate de que esté encendido y cerca de ti.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        isTracking = true;
        trackButton.setText("Detener Recorrida");

        stepTracker.resetSession();
        timeTracker.reset();
        gpsTracker.resetTracking();

        stepTracker.startTracking();
        timeTracker.startTracking();
        gpsTracker.startTracking();

        Log.d(ETIQUETA_LOG, " startTracking(): tracking iniciado (steps + time + GPS)");
        Log.d(ETIQUETA_LOG, " startTracking(): " + stepTracker.getSensorInfo());

        // ANTES:
        // Toast.makeText(this, "Recorrida iniciada - Beacon conectado", Toast.LENGTH_SHORT).show();

        // AHORA:
        Toast.makeText(this,
                "¡Recorrido iniciado! Tu sensor está midiendo la calidad del aire.",
                Toast.LENGTH_SHORT).show();
    }

    private void stopTracking() {
        Log.d(ETIQUETA_LOG, " stopTracking(): deteniendo tracking");

        // Ejecutar toda la lógica de finalización
        finalizarYGuardarRecorrido();

        // Abrir resumen de calidad del aire
        Intent intent = new Intent(MainActivity.this, AirQualitySummaryActivity.class);
        intent.putExtra("USER_ID", idUsuario);
        intent.putExtra("PASOS", stepTracker != null ? stepTracker.getSteps() : 0);
        intent.putExtra("TIEMPO", timeTracker != null ? timeTracker.getElapsedTimeMinutes() : 0);
        startActivity(intent);

        Log.d(ETIQUETA_LOG, " stopTracking(): tracking detenido - valores congelados");
    }

    // ------------------------------------------------------------------
    // Actualiza la UI con los valores de pasos y tiempo
    // ------------------------------------------------------------------
    private void updateTrackingUI(int steps, long hours, long minutes, long seconds) {
        runOnUiThread(() -> {
            // Actualizar pasos
            textSteps.setText(String.format("%d pasos", steps));

            // Actualizar tiempo
            tiempoTotal.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));

            Log.d(ETIQUETA_LOG, String.format(" UI actualizada: %d pasos, %02d:%02d:%02d",
                    steps, hours, minutes, seconds));
        });
    }

    // ------------------------------------------------------------------
    // Inicializa el adaptador Bluetooth y solicita permisos si es necesario
    // ------------------------------------------------------------------
    private void inicializarBlueTooth() {
        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): obtenemos adaptador BT ");

        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();

        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): obtenemos escaner btle ");

        this.elEscanner = bta.getBluetoothLeScanner();

        if ( this.elEscanner == null ) {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): Socorro: NO hemos obtenido escaner btle  !!!!");
        }

        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): voy a pedir permisos (si no los tuviera) !!!!");

        // Construir lista de permisos necesarios
        java.util.ArrayList<String> permisosNecesarios = new java.util.ArrayList<>();

        // Permisos de Bluetooth
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            permisosNecesarios.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permisosNecesarios.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        // Permiso de localización
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permisosNecesarios.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // Permiso de ACTIVITY_RECOGNITION (solo Android Q+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                permisosNecesarios.add(Manifest.permission.ACTIVITY_RECOGNITION);
                Log.d(ETIQUETA_LOG, " Agregando ACTIVITY_RECOGNITION a la lista de permisos");
            }
        }

        // Permiso de notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permisosNecesarios.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // Si hay permisos por solicitar, pedirlos todos juntos
        if (!permisosNecesarios.isEmpty()) {
            Log.d(ETIQUETA_LOG, " Solicitando " + permisosNecesarios.size() + " permisos: " + permisosNecesarios.toString());
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    permisosNecesarios.toArray(new String[0]),
                    CODIGO_PETICION_PERMISOS
            );
        }
        else {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): parece que YA tengo los permisos necesarios !!!!");
            habilitarBluetoothSiEsNecesario(bta);
        }
    } // ()

    // ------------------------------------------------------------------
    // Habilita Bluetooth después de tener los permisos
    // ------------------------------------------------------------------
    private void habilitarBluetoothSiEsNecesario(BluetoothAdapter bta) {
        if (bta == null) {
            Log.e(ETIQUETA_LOG, " habilitarBluetoothSiEsNecesario(): BluetoothAdapter es null");
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(ETIQUETA_LOG, " habilitarBluetoothSiEsNecesario(): No tenemos permiso BLUETOOTH_CONNECT aún");
            return;
        }

        if (!bta.isEnabled()) {
            Log.d(ETIQUETA_LOG, " habilitarBluetoothSiEsNecesario(): Bluetooth desactivado, solicitando activación...");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, CODIGO_PETICION_PERMISOS);
        } else {
            Log.d(ETIQUETA_LOG, " habilitarBluetoothSiEsNecesario(): Bluetooth ya está activado");
        }

        Log.d(ETIQUETA_LOG, " habilitarBluetoothSiEsNecesario(): habilitado =  " + bta.isEnabled() );
        Log.d(ETIQUETA_LOG, " habilitarBluetoothSiEsNecesario(): estado =  " + bta.getState() );

        if (this.elEscanner == null) {
            this.elEscanner = bta.getBluetoothLeScanner();
            if (this.elEscanner != null) {
                Log.d(ETIQUETA_LOG, " habilitarBluetoothSiEsNecesario(): Scanner BLE obtenido correctamente");
                inicializarVinculador();
            } else {
                Log.e(ETIQUETA_LOG, " habilitarBluetoothSiEsNecesario(): No se pudo obtener scanner BLE");
            }
        }
    } // ()

    private void estadoBotonRecorrido(boolean estadoDispotivoVinculado){
        trackButton.setEnabled(estadoDispotivoVinculado);
    }

    private void inicializarVinculador() {
        if (this.elEscanner == null) {
            Log.e(ETIQUETA_LOG, " inicializarVinculador(): Scanner BLE no disponible aún");
            return;
        }

        verificarNodoVinculado();

        findViewById(R.id.boton_perfil).setOnClickListener(v -> {
            abrirPerfilActivity();
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1001
                );
            }
        }


        estadoBotonRecorrido(false);

        vinculador = new VinculadorBLE(this.elEscanner, new VinculadorBLE.Listener() {
            @Override
            public void onEstadoCambio(VinculadorBLE.Estado nuevoEstado) {
                Log.d(">>>>", "UI onEstadoCambio = " + nuevoEstado);
                switch (nuevoEstado) {
                    case VINCULADO:
                        String userId = Integer.toString((idUsuario));
                        String nombreNodo = vinculador.getNombreNodoActual();

                        RegistroNodo registro = new RegistroNodo(userId, nombreNodo);
                        registro.registrarNodo();

                        yaVinculado = true;
                        nombreNodoVinculado = nombreNodo;

                        vinculador.detener();
                        detenerBusquedaDispositivosBTLE();

                        runOnUiThread(() -> refrescarActividad());
                        buscarEsteDispositivoBTLE(nombreNodo);
                        estadoBotonRecorrido(true);

                        monitorEstadoNodo = new NotifEstadoNodo(MainActivity.this, nombreNodo);
                        monitorEstadoNodo.iniciarMonitor();

                        runOnUiThread(() -> {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("¡Sensor vinculado correctamente!")
                                    .setMessage(
                                            "Tu sensor está conectado y midiendo la calidad del aire.\n\n" +
                                                    "Ahora puedes:\n" +
                                                    "• Iniciar recorridos para ganar puntos\n" +
                                                    "• Ver tus mediciones en tiempo real\n" +
                                                    "• Canjear premios con tus puntos"
                                    )
                                    .setPositiveButton("¡Genial!", null)
                                    .show();
                        });

                        // NUEVO: Configurar listener para desconexión del nodo
                        monitorEstadoNodo.setDesconexionListener(() -> {
                            beaconConectado = false;

                            // Si estaba haciendo tracking, enviar mediciones antes de detener
                            if (isTracking) {
                                Log.d(ETIQUETA_LOG, "DESCONEXIÓN ABRUPTA DETECTADA");

                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this,
                                            "Sensor desconectado.\n\nGuardando tus datos de recorrido...",
                                            Toast.LENGTH_LONG).show();
                                });

                                // Obtener valores actuales
                                int pasosActuales = stepTracker != null ? stepTracker.getSteps() : 0;
                                long tiempoActual = timeTracker != null ? timeTracker.getElapsedTimeSeconds() : 0;
                                Location ubicacionActual = gpsTracker != null ? gpsTracker.getCurrentLocation() : null;

                                // Enviar mediciones con tipo "abrupta"
                                if (ubicacionActual != null && nombreNodoVinculado != null) {
                                    Log.d(ETIQUETA_LOG, " Enviando mediciones por desconexión abrupta:");
                                    Log.d(ETIQUETA_LOG, " - Pasos: " + pasosActuales);
                                    Log.d(ETIQUETA_LOG, " - Tiempo: " + tiempoActual + " seg");

                                    MeasurementsSender.enviarMedicionConDesconexion(
                                            nombreNodoVinculado,
                                            ultimaMedicionO3,
                                            ultimaMedicionCO,
                                            ultimaMedicionNO2,
                                            ubicacionActual,
                                            pasosActuales,
                                            tiempoActual,
                                            "abrupta", // Desconexión no planificada
                                            new MeasurementsSender.MeasurementCallback() {
                                                @Override
                                                public void onSuccess(String respuesta) {
                                                    Log.d(ETIQUETA_LOG, " Mediciones de desconexión abrupta enviadas");
                                                    runOnUiThread(() -> {
                                                        Toast.makeText(MainActivity.this,
                                                                "Tus datos fueron guardados correctamente antes de la desconexión.",
                                                                Toast.LENGTH_SHORT).show();
                                                    });
                                                }

                                                @Override
                                                public void onError(String error) {
                                                    Log.e(ETIQUETA_LOG, " Error enviando mediciones: " + error);
                                                }
                                            }
                                    );
                                } else {
                                    Log.w(ETIQUETA_LOG, " No se pueden enviar mediciones - datos incompletos");
                                }

                                // Opcional: detener tracking automáticamente
                                // runOnUiThread(() -> stopTracking());
                            }
                        });

                        iconoVincular.setImageResource(R.drawable.ic_vincular_verde);
                        // Mostrar botón "Encontrar mi sensor"
                        Button btnFind = findViewById(R.id.btnFindSensor);
                        btnFind.setVisibility(View.VISIBLE);

                        btnFind.setOnClickListener(v -> {
                            Intent i = new Intent(MainActivity.this, FindMyNodeActivity.class);
                            i.putExtra("NODE_NAME", nombreNodoVinculado);
                            startActivity(i);
                        });

                        break;
                }
            }

            @Override
            public void onDispositivoEncontrado(BluetoothDevice device, ScanResult result) {
                Log.d(">>>>", "Encontrado: " + device.getName() + " addr=" + device.getAddress()
                        + " rssi=" + result.getRssi());
            }

            @Override
            public void onError(int errorCode) {
                Log.d(">>>>", "Listener onError: code=" + errorCode);
            }
        });

        Log.d(ETIQUETA_LOG, " inicializarVinculador(): Vinculador BLE inicializado correctamente");
    }

    private void verificarSensores() {
        if (stepTracker != null) {
            boolean disponible = stepTracker.isStepCounterAvailable();
            String info = stepTracker.getSensorInfo();

            Log.d(ETIQUETA_LOG, " ===== VERIFICACIÓN DE SENSORES =====");
            Log.d(ETIQUETA_LOG, " Sensor disponible: " + disponible);
            Log.d(ETIQUETA_LOG, " Info: " + info);

            if (!disponible) {
                Log.e(ETIQUETA_LOG, " ⚠️ PROBLEMA: Este dispositivo NO tiene sensor de pasos");
                runOnUiThread(() -> {
                    Toast.makeText(this,
                            "Este dispositivo no tiene sensor de pasos",
                            Toast.LENGTH_LONG).show();
                });
            }
        }
    }

    private boolean verificarSesionActiva() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        int savedUserId = prefs.getInt("user_id", -1);
        String savedToken = prefs.getString("token", null);
        boolean sesionActiva = prefs.getBoolean("sesion_activa", false);

        if (savedUserId == -1 || savedToken == null || savedToken.isEmpty() || !sesionActiva) {
            Log.d(ETIQUETA_LOG, "No hay sesión activa - Redirigiendo a Login");

            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

            return false;
        }

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(ETIQUETA_LOG, " onCreate(): empieza ");

        // ⭐ AGREGAR ESTA VERIFICACIÓN AL INICIO ⭐
        if (!verificarSesionActiva()) {
            return; // Detener ejecución si no hay sesión
        }

        // =====================================================
        // 1️⃣ READ INTENT FIRST (CRITICAL)
        // =====================================================
        Intent intent = getIntent();
        if (intent != null) {
            idUsuario = intent.getIntExtra("USER_ID", -1);
            token = intent.getStringExtra("TOKEN");
        }

        // ⭐ AGREGAR: Si no vienen del intent, cargar de SharedPreferences ⭐
        if (idUsuario == -1 || token == null) {
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            idUsuario = prefs.getInt("user_id", -1);
            token = prefs.getString("token", null);
        }

        if (idUsuario == -1 || token == null) {
            Log.e(ETIQUETA_LOG, "ERROR: USER_ID o TOKEN no disponibles");
            Toast.makeText(this,
                    "Tu sesión ha expirado.\n\nVuelve a iniciar sesión para continuar.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // =====================================================
        // 2️⃣ BIND MAIN UI
        // =====================================================
        textMajor = findViewById(R.id.textMajor);
        textMinor = findViewById(R.id.textMinor);
        // IMPORTANTE: Los IDs de los TextViews parecen ser diferentes en tu código anterior y nuevo.
        // Usa los IDs que realmente están en tu activity_main.xml.
        // Asumiré que quieres usar los IDs del código nuevo (distanciaTotal y tiempoTotal)
        textSteps = findViewById(R.id.distanciaTotal);
        tiempoTotal = findViewById(R.id.tiempoTotal);
        trackButton = findViewById(R.id.track);

        // Nuevas vistas en el código
        textVinculacion = findViewById(R.id.textVinculacion);
        btnVincular = findViewById(R.id.botonBuscarNuestroDispositivoBTLE);


        // =====================================================
        // 3️⃣ DRAWER + HEADER (¡Verifica el layout del header!)
        // =====================================================
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        btnMenu = findViewById(R.id.btnMenu); // El botón de menú/hamburguesa está en el activity_main

        // OBTENER ICONO DEL HEADER: Esta es la parte crítica
        try {
            iconoVincular = findViewById(R.id.iconoVincular);

            // Configurar listener para iconoVincular (en el header)
            if (iconoVincular != null) {
                iconoVincular.setOnClickListener(v -> {
                    if (yaVinculado) {
                        new AlertDialog.Builder(this)
                                .setTitle("Dispositivo conectado")
                                .setMessage(
                                        "Tu dispositivo está conectado y midiendo la calidad del aire.\n\n" +
                                                "¿Deseas desvincularlo?"
                                )
                                .setPositiveButton("Desvincular", (d, w) -> desvincularNodo())
                                .setNegativeButton("Cancelar", null)
                                .show();
                    } else {
                        // Llama a la lógica de vinculación al hacer clic en el icono
                        botonVincularPulsado(v);
                    }
                });
            } else {
                Log.w(ETIQUETA_LOG, "AVISO: iconoVincular no se encontró en el header del Navigation View.");
            }
        } catch (Exception e) {
            Log.e(ETIQUETA_LOG, "Error al inicializar Drawer/Header: " + e.getMessage(), e);
            // Podrías lanzar un Toast aquí si es un error crítico
        }

        // Configurar listener para btnMenu (en el toolbar/layout principal)
        if (btnMenu != null && drawerLayout != null) {
            btnMenu.setOnClickListener(v ->
                    drawerLayout.openDrawer(GravityCompat.START)
            );
        } else {
            Log.w(ETIQUETA_LOG, "AVISO: btnMenu o drawerLayout no se encontraron.");
        }

        // Configurar listener para los elementos del Navigation View
        //-----CAMBIOS ADE-----

        navigationView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(GravityCompat.START);

            int itemId = item.getItemId();

            if (itemId == R.id.nav_perfil) {
                abrirPerfilActivity();

            } else if (itemId == R.id.nav_recorrido) {
                // NUEVO: Aplicar la misma lógica que stopTracking() antes de navegar
                if (!beaconConectado) {
                    Toast.makeText(this,
                            "No hay sensor conectado.\n\nConecta tu sensor para ver tus recorridos.",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }

                // Si hay tracking activo, aplicar toda la lógica de stopTracking
                if (isTracking) {
                    finalizarYGuardarRecorrido();
                }

                // Navegar a resumen con los datos más recientes
                Intent intenT = new Intent(this, AirQualitySummaryActivity.class);
                intenT.putExtra("USER_ID", idUsuario);

                // Pasar datos del último recorrido si existen
                if (stepTracker != null && timeTracker != null) {
                    intenT.putExtra("PASOS", stepTracker.getSteps());
                    intenT.putExtra("TIEMPO", timeTracker.getElapsedTimeMinutes());
                }

                startActivity(intenT);

            } else if (itemId == R.id.nav_recompensa) {
                abrirPantallaGamificacion(null);

            } else if (itemId == R.id.nav_mapa) {
                Toast.makeText(this,
                        "Ya te encuentras en la pantalla de inicio.",
                        Toast.LENGTH_SHORT).show();

            } else if (itemId == R.id.nav_notificaciones) {
                new AlertDialog.Builder(this)
                        .setTitle("Notificaciones")
                        .setMessage("Gestiona tus notificaciones de calidad del aire.\n\n" +
                                "Recibirás alertas cuando:\n" +
                                "• Los niveles de O3 sean peligrosos\n" +
                                "• Tu dispositivo se desconecte\n" +
                                "• Completes logros")
                        .setPositiveButton("Aceptar", null)
                        .show();

            } else if (itemId == R.id.nav_info) {
                new AlertDialog.Builder(this)
                        .setTitle("Información sobre Gases")
                        .setMessage("O3 (Ozono):\n" +
                                "Gas irritante que afecta las vías respiratorias.\n" +
                                "Límite seguro: < 0.06 ppm\n\n" +
                                "CO (Monóxido de Carbono):\n" +
                                "Gas tóxico sin olor ni color.\n" +
                                "Límite seguro: < 9 ppm\n\n" +
                                "NO2 (Dióxido de Nitrógeno):\n" +
                                "Irritante producido por combustión.\n" +
                                "Límite seguro: < 0.053 ppm")
                        .setPositiveButton("Entendido", null)
                        .show();

            } else {
                Toast.makeText(this,
                        "Pantalla aún no implementada",
                        Toast.LENGTH_SHORT).show();
            }

            return true;
        });

        // Configuración del botón de retroceso
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    MainActivity.super.onBackPressed();
                }
            }
        });

        // =====================================================
        // 4️⃣ INITIAL UI STATE
        // =====================================================
        // Solo si iconoVincular no es null
        if (iconoVincular != null) {
            iconoVincular.setImageResource(R.drawable.ic_link_off); // Usando el nuevo icono
        }
        estadoBotonRecorrido(false);


        // Solo si las vistas principales no son null
        if (textSteps != null) textSteps.setText("0 pasos");
        if (tiempoTotal != null) tiempoTotal.setText("00:00:00");

        // =====================================================
        // 5️⃣ BLUETOOTH + VINCULADOR (SAFE NOW)
        // =====================================================
        inicializarBlueTooth();
        inicializarVinculador();

        // =====================================================
        // 6️⃣ TRACKERS
        // =====================================================
        try {
            stepTracker = new StepCounterTracker(this);
            timeTracker = new WalkingTimeTracker();
            gpsTracker = new GPSFondo(this);

            // Listeners
            stepTracker.setStepListener(steps -> {
                if (isTracking && beaconConectado) {
                    WalkingTimeTracker.TimeComponents t = timeTracker.getTimeComponents();
                    updateTrackingUI(steps, t.hours, t.minutes, t.seconds);
                }
            });

            timeTracker.setTimeUpdateListener((h, m, s, total) -> {
                if (isTracking && beaconConectado) {
                    updateTrackingUI(stepTracker.getSteps(), h, m, s);
                }
            });

            gpsTracker.setLocationUpdateListener(new GPSFondo.LocationUpdateListener() {
                @Override
                public void onLocationUpdate(Location location) {
                    if (isTracking && beaconConectado) {
                        ultimaUbicacionNodo = location;
                    }
                }

                @Override
                public void onLocationError(String error) {
                    Log.e(ETIQUETA_LOG, " GPS Error: " + error);
                }
            });

            verificarSensores();

        } catch (Exception e) {
            Log.e(ETIQUETA_LOG, " Error inicializando trackers", e);
            // Manejo de error más visible para el usuario si es crítico
            Toast.makeText(this, "Error de sensores: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        Log.d(ETIQUETA_LOG, " onCreate(): termina ");
    }


    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults);

        if (requestCode == CODIGO_PETICION_PERMISOS) {
            boolean bluetoothGranted = true;
            boolean locationGranted = false;
            boolean activityRecognitionGranted = false;

            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;

                Log.d(ETIQUETA_LOG, " Permiso: " + permission + " = " + (granted ? "CONCEDIDO" : "DENEGADO"));

                if (permission.equals(Manifest.permission.BLUETOOTH_SCAN) ||
                        permission.equals(Manifest.permission.BLUETOOTH_CONNECT)) {
                    bluetoothGranted = bluetoothGranted && granted;
                } else if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    locationGranted = granted;
                } else if (permission.equals(Manifest.permission.ACTIVITY_RECOGNITION)) {
                    activityRecognitionGranted = granted;
                }
            }

            if (bluetoothGranted && locationGranted) {
                Log.d(ETIQUETA_LOG, " onRequestPermissionResult(): permisos BT y Location concedidos !!!!");
                BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
                habilitarBluetoothSiEsNecesario(bta);
            } else {
                Log.d(ETIQUETA_LOG, " onRequestPermissionResult(): Socorro: permisos BT/Location NO concedidos !!!!");
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (activityRecognitionGranted) {
                    Log.d(ETIQUETA_LOG, " onRequestPermissionResult(): permiso ACTIVITY_RECOGNITION concedido");
                    if (stepTracker != null) {
                        verificarSensores();
                    }
                } else {
                    Log.w(ETIQUETA_LOG, " onRequestPermissionResult(): permiso ACTIVITY_RECOGNITION DENEGADO");
                    Toast.makeText(this,
                            "Sin permiso de actividad física, no podemos contar tus pasos.\n\nActiva el permiso en Configuración > Aplicaciones > Air-o-Walk > Permisos.",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public void abrirPerfilActivity() {
        if (idUsuario == -1 || token == null) {
            Toast.makeText(this,
                    "No se encontraron tus datos de usuario.\n\nCierra sesión y vuelve a iniciar sesión.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(MainActivity.this, PerfilActivity.class);
        intent.putExtra("USER_ID", idUsuario);
        intent.putExtra("TOKEN", token);
        startActivity(intent);

        Log.d(ETIQUETA_LOG, "Abriendo PerfilActivity con USER_ID: " + idUsuario);
    }

    public void botonVincularPulsado(View v) {

        // 🔒 EXTRA SAFETY: if already linked, DO NOTHING
        if (yaVinculado) {
            return;
        }

        EditText input = new EditText(this);
        input.setHint("Ej: GTI");

        new AlertDialog.Builder(this)
                .setTitle("Conectar dispositivo")
                .setMessage("Introduce el nombre del dispositivo para empezar a medir la calidad del aire.")
                .setView(input)
                .setPositiveButton("Vincular", (dlg, which) -> {
                    String codigo = input.getText().toString().trim();
                    if (codigo.isEmpty()) return;

                    iconoVincular.setImageResource(R.drawable.ic_link_off);
                    vinculador.vincularPorNombre(codigo, 10_000);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }


    private void verificarNodoVinculado() {
        String url = "http://api.sagucre.upv.edu.es/node/ofUser/" + idUsuario;

        PeticionarioREST peticion = new PeticionarioREST();
        peticion.hacerPeticionREST("GET", url, null, new PeticionarioREST.RespuestaREST() {
            @Override
            public void callback(int codigo, String cuerpo) {
                Log.d(">>>>", "Verificar nodo, código=" + codigo + " cuerpo=" + cuerpo);

                try {
                    JSONObject json = new JSONObject(cuerpo);

                    if (json.getBoolean("success")) {
                        nombreNodoVinculado = json.getJSONObject("node").getString("name");
                        yaVinculado = true;
                    } else {
                        yaVinculado = false;
                        nombreNodoVinculado = null;
                    }

                    runOnUiThread(() -> {
                        actualizarEstadoVinculacionUI();

                        // Start scan only if linked
                        if (yaVinculado) {
                            buscarEsteDispositivoBTLE(nombreNodoVinculado);
                        }
                    });

                } catch (Exception e) {
                    Log.e(">>>>", "Error procesando verificación nodo", e);
                }
            }
        });
    }


    private void desvincularNodo() {
        String url = "http://api.sagucre.upv.edu.es/node/ofUser/" + idUsuario;

        PeticionarioREST peticion = new PeticionarioREST();

        peticion.hacerPeticionREST("DELETE", url, null, new PeticionarioREST.RespuestaREST() {
            @Override
            public void callback(int codigo, String cuerpo) {

                Log.d(">>>>", "Desvincular nodo, código=" + codigo + " cuerpo=" + cuerpo);

                runOnUiThread(() -> {
                    yaVinculado = false;
                    nombreNodoVinculado = null;
                    beaconConectado = false;

                    detenerBusquedaDispositivosBTLE();

                    actualizarEstadoVinculacionUI();

                    if (isTracking) stopTracking();

                    textSteps.setText("---");
                    tiempoTotal.setText("---");

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Sensor desvinculado")
                            .setMessage(
                                    "Tu sensor se ha desvinculado correctamente.\n\n" +
                                            "Para volver a usarlo, toca el ícono de enlace en el menú superior."
                            )
                            .setPositiveButton("Entendido", null)
                            .show();
                });


            }
        });
    }

    private void refrescarActividad() {
        Intent intent = getIntent();
        contadorAndroid = -1;
        finish();
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (monitorEstadoNodo != null) {
            monitorEstadoNodo.detenerMonitor();
        }

        if (timeTracker != null) {
            timeTracker.destroy();
        }
        if (stepTracker != null) {
            stepTracker.stopTracking();
        }
        if (gpsTracker != null) {
            gpsTracker.stopTracking();
        }

        // NUEVO: Enviar última ubicación si estaba conectado
        if (beaconConectado && ultimaUbicacionNodo != null) {
            enviarUltimaUbicacionNodo();
        }
    }
    private void actualizarUIVinculacion() {
        if (yaVinculado) {
            textVinculacion.setVisibility(View.GONE);
            btnVincular.setVisibility(View.GONE);
        } else {
            textVinculacion.setVisibility(View.VISIBLE);
            btnVincular.setVisibility(View.VISIBLE);
        }
    }
    private void actualizarEstadoVinculacionUI() {

        if (iconoVincular != null) {
            iconoVincular.setImageResource(
                    yaVinculado ? R.drawable.ic_link : R.drawable.ic_link_off
            );
            iconoVincular.setEnabled(true);
            iconoVincular.setAlpha(1f);
        }

        actualizarUIVinculacion();
        estadoBotonRecorrido(yaVinculado);

        Button btnFind = findViewById(R.id.btnFindSensor);
        if (btnFind != null) {
            btnFind.setVisibility(yaVinculado ? View.VISIBLE : View.GONE);
        }
    }

    private void finalizarYGuardarRecorrido() {
        Log.d(ETIQUETA_LOG, " finalizarYGuardarRecorrido(): procesando datos del recorrido");

        // Verificar que los trackers estén inicializados
        if (stepTracker == null || timeTracker == null || gpsTracker == null) {
            Log.e(ETIQUETA_LOG, " finalizarYGuardarRecorrido(): Error - trackers no inicializados");
            return;
        }

        // Obtener valores finales ANTES de detener
        int pasos = stepTracker.getSteps();
        long tiempoSegundos = timeTracker.getElapsedTimeSeconds();
        Location ubicacionFinal = gpsTracker.getCurrentLocation();

        // Solo detener si tracking está activo
        if (isTracking) {
            isTracking = false;
            trackButton.setText("Activar Recorrida");

            stepTracker.stopTracking();
            timeTracker.stopTracking();
            gpsTracker.stopTracking();
        }

        // Calcular puntos de gamificación
        Gamificacion game = new Gamificacion(idUsuario);
        int puntos = game.calcularPuntosMedianteDistancia(pasos);
        game.setUltimosPuntosObtenidos(puntos);

        // Guardar estadísticas diarias
        MeasurementsLogica medidas = new MeasurementsLogica(idUsuario, pasos, puntos, timeTracker.getElapsedTimeHours());
        medidas.guardarDailyStats();

        // Enviar mediciones al servidor
        if (ubicacionFinal != null && nombreNodoVinculado != null) {
            Log.d(ETIQUETA_LOG, " ENVIANDO MEDICIONES FINALES DE RECORRIDA");
            Log.d(ETIQUETA_LOG, "  Pasos: " + pasos);
            Log.d(ETIQUETA_LOG, "  Tiempo: " + tiempoSegundos + " seg (" + (tiempoSegundos/60) + " min)");
            Log.d(ETIQUETA_LOG, "  Ubicación: " + ubicacionFinal.getLatitude() + ", " + ubicacionFinal.getLongitude());

            MeasurementsSender.enviarMedicionConDesconexion(
                    nombreNodoVinculado,
                    ultimaMedicionO3,
                    ultimaMedicionCO,
                    ultimaMedicionNO2,
                    ubicacionFinal,
                    pasos,
                    tiempoSegundos,
                    "manual", // Usuario detuvo manualmente
                    new MeasurementsSender.MeasurementCallback() {
                        @Override
                        public void onSuccess(String respuesta) {
                            Log.d(ETIQUETA_LOG, " Mediciones finales enviadas correctamente");
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(ETIQUETA_LOG, " Error enviando mediciones finales: " + error);
                        }
                    }
            );
        } else {
            Log.w(ETIQUETA_LOG, " No se pueden enviar mediciones - ubicación o nodo no disponible");
        }

        Log.d(ETIQUETA_LOG, " finalizarYGuardarRecorrido(): datos procesados y guardados");
    }
}