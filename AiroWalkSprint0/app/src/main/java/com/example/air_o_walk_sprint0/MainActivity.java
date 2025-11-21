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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;

// ------------------------------------------------------------------
// Clase principal de la actividad Android
// ------------------------------------------------------------------

public class MainActivity extends AppCompatActivity {

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

    // Trackers para pasos, tiempo y GPS
    private StepCounterTracker stepTracker;
    private WalkingTimeTracker timeTracker;
    private GPSFondo gpsTracker;

    private int idUsuario;
    private String token;

    // Estado del tracking
    private boolean isTracking = false;

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
                //guardarMedicion( resultado ); // Envía la medición al backend
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

    // ------------------------------------------------------------------
    // Procesa la trama recibida y envía la medición al backend si es nueva
    // Evita duplicados usando el contador y banderas
    // ------------------------------------------------------------------
    private void guardarMedicion( ScanResult resultado ){

        CompletableFuture.runAsync(() -> {
            byte[] bytes = resultado.getScanRecord().getBytes();
            TramaIBeacon tib = new TramaIBeacon(bytes);

            byte[] major = tib.getMajor();
            int tipoMedicion = major[0] & 0xFF;
            int contadorArduino = major[1] & 0xFF;
            int valorMedicion = Utilidades.bytesToInt(tib.getMinor());

            // Si es un nuevo contador, reinicia banderas
            if (contadorArduino != this.contadorAndroid) {
                Log.d("ETIQUETA_LOG", "Nuevo contador, se reinician banderas");
                this.contadorAndroid = contadorArduino;
                this.recibioGas = false;
                this.recibioTemperatura = false;
            }

            // Verificamos qué tipo de medición llegó y si ya se envió
            if (tipoMedicion == 11 && !recibioGas) {
                this.recibioGas = true;
                Log.d("ETIQUETA_LOG", "Enviando medición tipo: " + tipoMedicion + " (contador " + contadorArduino + ")");
                Logica logica = new Logica(tipoMedicion, valorMedicion);
                logica.guardarMedcion();
            }
            else if (tipoMedicion == 12 && !recibioTemperatura) {
                this.recibioTemperatura = true;
                Log.d("ETIQUETA_LOG", "Enviando medición tipo: " + tipoMedicion + " (contador " + contadorArduino + ")");
                Logica logica = new Logica(tipoMedicion, valorMedicion);
                logica.guardarMedcion();
            }
            else {
                Log.d("ETIQUETA_LOG", "Medición duplicada ignorada (tipo=" + tipoMedicion + ", contador=" + contadorArduino + ")");
            }

        });
    }


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

        // -----------------------------------------------------------
        // Llamamos al monitor del nodo (conectado / desconectado / incoherente)
        // -----------------------------------------------------------
        if (monitorEstadoNodo != null) {
            monitorEstadoNodo.onBeaconRecibido(medicionGas, medicionTemperatura);
        }


        // Llamamos a la notificación desde el hilo principal (UI thread)
        runOnUiThread(() -> {
            AdminNotificaciones.revisarYNotificar(this, medicionGas);
            textMajor.setText("03(ppm): " + medicionGas);
            textMinor.setText("Temperatura(ºC): " + medicionTemperatura);
        });
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
        this.buscarEsteDispositivoBTLE( "GTI");
    } // ()

    public void botonDetenerBusquedaDispositivosBTLEPulsado( View v ) {
        Log.d(ETIQUETA_LOG, " boton detener busqueda dispositivos BTLE Pulsado" );
        this.detenerBusquedaDispositivosBTLE();
    } // ()


    public void abrirPantallaGamificacion(View v) {
        // Crear el Intent para abrir GamificacionActivity
        Intent intent = new Intent(MainActivity.this, GamificacionActivity.class);

        // Pasar el user_id (reemplaza 'user_id' con el nombre de tu variable)
        intent.putExtra("USER_ID", idUsuario);

        // Iniciar la nueva Activity
        startActivity(intent);
    }

    public void abrirPantallaCanjeos(View v) {
        // Crear el Intent para abrir GamificacionActivity
        Intent intent = new Intent(MainActivity.this, CanjeoActivity.class);

        // Pasar el user_id (reemplaza 'user_id' con el nombre de tu variable)
        intent.putExtra("USER_ID", idUsuario);

        // Iniciar la nueva Activity
        startActivity(intent);
    }

    // ------------------------------------------------------------------
    // NUEVO: Método para controlar el tracking de pasos, tiempo y GPS
    // ------------------------------------------------------------------
    public void botonDistanceTrackerPulsado(View v) {
        if (!isTracking) {
            // Iniciar tracking
            startTracking();
        } else {
            // Detener tracking
            stopTracking();
        }
    }

    private void startTracking() {
        Log.d(ETIQUETA_LOG, " startTracking(): iniciando tracking de pasos, tiempo y GPS");

        // Verificar que los trackers estén inicializados
        if (stepTracker == null || timeTracker == null || gpsTracker == null) {
            Log.e(ETIQUETA_LOG, " startTracking(): Error - trackers no inicializados");
            Toast.makeText(this, "Error: trackers no inicializados", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar que el sensor de pasos esté disponible
        if (!stepTracker.isStepCounterAvailable()) {
            Log.e(ETIQUETA_LOG, " startTracking(): Error - no hay sensor de pasos disponible");
            Toast.makeText(this, "Este dispositivo no tiene sensor de pasos", Toast.LENGTH_LONG).show();
            return;
        }

        isTracking = true;
        trackButton.setText("Detener Recorrida");

        // Reiniciar trackers
        stepTracker.resetSession();
        timeTracker.reset();
        gpsTracker.resetTracking();

        // Iniciar step counter
        stepTracker.startTracking();

        // Iniciar time tracker
        timeTracker.startTracking();

        // Iniciar GPS tracker
        gpsTracker.startTracking();



        Log.d(ETIQUETA_LOG, " startTracking(): tracking iniciado (steps + time + GPS)");
        Log.d(ETIQUETA_LOG, " startTracking(): " + stepTracker.getSensorInfo());
    }

    private void stopTracking() {
        Log.d(ETIQUETA_LOG, " stopTracking(): deteniendo tracking");

        // Verificar que los trackers estén inicializados
        if (stepTracker == null || timeTracker == null || gpsTracker == null) {
            Log.e(ETIQUETA_LOG, " stopTracking(): Error - trackers no inicializados");
            return;
        }

        isTracking = false;
        trackButton.setText("Activar Recorrida");

        // Detener trackers (pero mantener los valores actuales)
        stepTracker.stopTracking();
        timeTracker.stopTracking();
        gpsTracker.stopTracking();

        int pasos = stepTracker.getSteps();

        Gamificacion game = new Gamificacion(idUsuario);
        int puntos = game.calcularPuntosMedianteDistancia(pasos);
        game.setUltimosPuntosObtenidos(puntos);

        MeasurementsLogica medidas = new MeasurementsLogica(idUsuario, pasos, puntos, timeTracker.getElapsedTimeHours());

        medidas.guardarDailyStats();





        // --------------------------------------------------------------
        // ---- Abrir resumen de calidad del aire (se envía USER_ID) ----
        Intent intent = new Intent(MainActivity.this, AirQualitySummaryActivity.class);
        intent.putExtra("USER_ID", idUsuario);
        intent.putExtra("PASOS", stepTracker.getSteps() );
        intent.putExtra("TIEMPO", timeTracker.getElapsedTimeMinutes());
        startActivity(intent);
        // --------------------------------------------------------------

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
    // MODIFICADO: Ahora solicita TODOS los permisos juntos incluyendo ACTIVITY_RECOGNITION
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
            // Solo después de tener permisos, intentar habilitar Bluetooth
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

        // Verificar que tenemos el permiso BLUETOOTH_CONNECT
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

        // Obtener el scanner después de habilitar Bluetooth
        if (this.elEscanner == null) {
            this.elEscanner = bta.getBluetoothLeScanner();
            if (this.elEscanner != null) {
                Log.d(ETIQUETA_LOG, " habilitarBluetoothSiEsNecesario(): Scanner BLE obtenido correctamente");
                // Ahora inicializar el vinculador
                inicializarVinculador();
            } else {
                Log.e(ETIQUETA_LOG, " habilitarBluetoothSiEsNecesario(): No se pudo obtener scanner BLE");
            }
        }
    } // ()

    // ------------------------------------------------------------------
    // ELIMINADO: solicitarPermisosTracking() ya no es necesario
    // Ahora todos los permisos se solicitan juntos en inicializarBlueTooth()
    // ------------------------------------------------------------------

    //Funcion para activar boton de inicio de recorrido
    private void estadoBotonRecorrido(boolean estadoDispotivoVinculado){
        trackButton.setEnabled(estadoDispotivoVinculado);
    }

    // ------------------------------------------------------------------
    // Inicializa el vinculador BLE
    // ------------------------------------------------------------------
    private void inicializarVinculador() {
        if (this.elEscanner == null) {
            Log.e(ETIQUETA_LOG, " inicializarVinculador(): Scanner BLE no disponible aún");
            // Reintentaremos cuando tengamos permisos
            return;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(ETIQUETA_LOG, " onCreate(): empieza ");

        // Inicializar vistas
        textMajor = findViewById(R.id.textMajor);
        textMinor = findViewById(R.id.textMinor);
        distanciaTotal = findViewById(R.id.distanciaTotal);
        tiempoTotal = findViewById(R.id.tiempoTotal);
        trackButton = findViewById(R.id.track);

        // Inicializar Bluetooth
        inicializarBlueTooth();

        // Recuperar datos del Intent
        Intent intent = getIntent();
        if (intent != null) {
            idUsuario = intent.getIntExtra("USER_ID", -1); // -1 es valor por defecto
            token = intent.getStringExtra("TOKEN");

            Log.d(ETIQUETA_LOG, "Datos recibidos - USER_ID: " + idUsuario + ", TOKEN: " + (token != null ? "presente" : "null"));
        }
        // ==============================
        // VERIFICAR SI EL USUARIO YA TIENE NODO VINCULADO
        // ==============================
            verificarNodoVinculado();
        // ==============================

// Configurar botón para ir al perfil (esto debe estar en un onClickListener, no ejecutarse automáticamente)
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

// ==============================================================================================================
// CONFIGURACIÓN DEL SISTEMA DE VINCULACIÓN
// - Se inicializa el icono (rojo = no vinculado)
// - Se crea el VinculadorBLE que gestiona el escaneo por nombre del beacon
// - Cuando el beacon se encuentra => estado VINCULADO => registramos en backend => refrescamos MainActivity
// ==============================================================================================================
        iconoVincular = findViewById(R.id.iconoVincular);
        iconoVincular.setImageResource(R.drawable.ic_vincular_rojo);
        estadoBotonRecorrido(false);
        vinculador = new VinculadorBLE(this.elEscanner, new VinculadorBLE.Listener() {
            @Override public void onEstadoCambio(VinculadorBLE.Estado nuevoEstado) {
                Log.d(">>>>", "UI onEstadoCambio = " + nuevoEstado);
                switch (nuevoEstado) {
                    case VINCULADO:
                        //REGISTRO NODO: enviar userId + nombre del beacon al backend
                        String userId = Integer.toString((idUsuario));
                        String nombreNodo = vinculador.getNombreNodoActual();
                        // Enviar vinculación al backend
                        RegistroNodo registro = new RegistroNodo(userId, nombreNodo);
                        registro.registrarNodo();
                        // Actualizar estado local
                        yaVinculado = true;
                        nombreNodoVinculado = nombreNodo;
                        // Detener escaneos activos
                        vinculador.detener();
                        detenerBusquedaDispositivosBTLE();
                        // Recargamos MainActivity para que empiece lectura BLE automática
                        runOnUiThread(() -> refrescarActividad());
                        buscarEsteDispositivoBTLE(nombreNodo);
                        estadoBotonRecorrido(true);

                        // ===================================================
                        // Iniciar el monitor de estado del nodo
                        // ===================================================
                        monitorEstadoNodo = new NotifEstadoNodo(MainActivity.this, nombreNodo);
                        monitorEstadoNodo.iniciarMonitor();
                        // ===================================================

                        // ===================================================================
                        iconoVincular.setImageResource(R.drawable.ic_vincular_verde);
                        break;
                }
            }

            @Override public void onDispositivoEncontrado(BluetoothDevice device, ScanResult result) {
                Log.d(">>>>", "Encontrado: " + device.getName() + " addr=" + device.getAddress()
                        + " rssi=" + result.getRssi());
            }
            @Override public void onError(int errorCode) {
                Log.d(">>>>", "Listener onError: code=" + errorCode);
            }
        });

        Log.d(ETIQUETA_LOG, " inicializarVinculador(): Vinculador BLE inicializado correctamente");
    }

    // ------------------------------------------------------------------
    // NUEVO: Verifica la disponibilidad de sensores de pasos
    // ------------------------------------------------------------------
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

    // ------------------------------------------------------------------
    // Método principal de ciclo de vida: inicializa la actividad y Bluetooth
    // ------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(ETIQUETA_LOG, " onCreate(): empieza ");

        // Inicializar vistas
        textMajor = findViewById(R.id.textMajor);
        textMinor = findViewById(R.id.textMinor);
        textSteps = findViewById(R.id.distanciaTotal); // Ahora muestra pasos
        tiempoTotal = findViewById(R.id.tiempoTotal);
        trackButton = findViewById(R.id.track);

        // Inicializar Bluetooth
        inicializarBlueTooth();

        // Recuperar datos del Intent
        Intent intent = getIntent();
        if (intent != null) {
            idUsuario = intent.getIntExtra("USER_ID", -1); // -1 es valor por defecto
            token = intent.getStringExtra("TOKEN");
        }

// ==============================================================================================================
// VINCULAR
// Descripción: Inicializa el icono de vinculación y crea el VinculadorBLE para gestionar el enlace
// con el beacon. Si se vincula correctamente, registra el nodo en el backend.
// ==============================================================================================================
        iconoVincular = findViewById(R.id.iconoVincular);
        iconoVincular.setImageResource(R.drawable.ic_vincular_rojo);
        estadoBotonRecorrido(false);

        // Inicializar vinculador solo si tenemos el scanner BLE
        inicializarVinculador();
// ==============================================================================================================

        // Inicializar trackers con comprobación de nulidad
        try {
            stepTracker = new StepCounterTracker(this);
            timeTracker = new WalkingTimeTracker();
            gpsTracker = new GPSFondo(this);

            // Configurar listener para step counter (ahora solo pasos)
            stepTracker.setStepListener((steps) -> {
                if (isTracking) {
                    WalkingTimeTracker.TimeComponents time = timeTracker.getTimeComponents();
                    updateTrackingUI(steps, time.hours, time.minutes, time.seconds);
                }
            });

            // Configurar listener para time tracker
            timeTracker.setTimeUpdateListener((hours, minutes, seconds, totalSeconds) -> {
                if (isTracking) {
                    int steps = stepTracker.getSteps();
                    updateTrackingUI(steps, hours, minutes, seconds);
                }
            });

            // Configurar listener para GPS tracker
            gpsTracker.setLocationUpdateListener(new GPSFondo.LocationUpdateListener() {
                @Override
                public void onLocationUpdate(Location location) {
                    if (isTracking) {
                        Log.d(ETIQUETA_LOG, String.format(" GPS Update: Lat=%.6f, Lon=%.6f, Accuracy=%.1fm",
                                location.getLatitude(),
                                location.getLongitude(),
                                location.getAccuracy()));

                        // Aquí puedes guardar la ubicación en tu backend si lo necesitas
                        // Por ejemplo: enviarUbicacionABackend(location);
                    }
                }

                @Override
                public void onLocationError(String error) {
                    Log.e(ETIQUETA_LOG, " GPS Error: " + error);
                }
            });

            // Valores iniciales
            textSteps.setText("0 pasos");
            tiempoTotal.setText("00:00:00");

            Log.d(ETIQUETA_LOG, " Trackers inicializados correctamente");

            // IMPORTANTE: Verificar sensores después de inicializar
            verificarSensores();

        } catch (Exception e) {
            Log.e(ETIQUETA_LOG, " Error al inicializar trackers: " + e.getMessage());
            e.printStackTrace();
        }

        Log.d(ETIQUETA_LOG, " onCreate(): termina ");

    } // onCreate()

    // ------------------------------------------------------------------
    // Callback para el resultado de la petición de permisos
    // MODIFICADO: Ahora maneja todos los permisos juntos
    // ------------------------------------------------------------------
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults);

        if (requestCode == CODIGO_PETICION_PERMISOS) {
            // Revisar qué permisos fueron concedidos
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

            // Si se concedieron los permisos de Bluetooth, continuar
            if (bluetoothGranted && locationGranted) {
                Log.d(ETIQUETA_LOG, " onRequestPermissionResult(): permisos BT y Location concedidos !!!!");
                BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
                habilitarBluetoothSiEsNecesario(bta);
            } else {
                Log.d(ETIQUETA_LOG, " onRequestPermissionResult(): Socorro: permisos BT/Location NO concedidos !!!!");
            }

            // Verificar permiso de ACTIVITY_RECOGNITION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (activityRecognitionGranted) {
                    Log.d(ETIQUETA_LOG, " onRequestPermissionResult(): permiso ACTIVITY_RECOGNITION concedido");
                    if (stepTracker != null) {
                        verificarSensores();
                    }
                } else {
                    Log.w(ETIQUETA_LOG, " onRequestPermissionResult(): permiso ACTIVITY_RECOGNITION DENEGADO");
                    Log.w(ETIQUETA_LOG, " ¡La funcionalidad de contador de pasos no funcionará!");
                    Toast.makeText(this,
                            "Permiso de actividad física denegado. El contador de pasos no funcionará.",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    } // ()

    // ==============================================================================================================
    /**
     * Método para abrir la actividad de perfil
     */
    private void abrirPerfilActivity() {
        if (idUsuario == -1 || token == null) {
            Toast.makeText(this, "Error: No hay datos de usuario disponibles", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(MainActivity.this, PerfilActivity.class);
        intent.putExtra("USER_ID", idUsuario);
        intent.putExtra("TOKEN", token);
        startActivity(intent);

        Log.d(ETIQUETA_LOG, "Abriendo PerfilActivity con USER_ID: " + idUsuario);
    }

// ==============================================================================================================
// botonVincularPulsado()
// Mostrar diálogo para introducir el nombre del beacon (ej: "GTI")
// Si ya está vinculado → mostrar opciones ( aceptar/desvincular )
// Si no → iniciar VinculadorBLE.vincularPorNombre()
// ==============================================================================================================
    public void botonVincularPulsado(View v) {
        // Si YA hay beacon vinculado => mostrar opciones ( aceptar/desvincular )
        if (yaVinculado) {
            new AlertDialog.Builder(this)
                    .setTitle("Nodo ya vinculado")
                    .setMessage(
                            "Actualmente estás vinculado al beacon:\n\n" +
                                    "📡 " + nombreNodoVinculado +
                                    "\n\nPuedes conservarlo o desvincularlo."
                    )
                    .setPositiveButton("Aceptar", null)
                    .setNegativeButton("Desvincular nodo", (dialog, which) -> {
                        // Llamamos a la función de desvincular
                        desvincularNodo();
                    })
                    .show();
            return;
        }
        // Si NO hay beacon vinculado => pedir el nombre para vincular
        EditText input = new EditText(this);
        input.setHint("Ej: GTI");

        new AlertDialog.Builder(this)
                .setTitle("Vincular Beacon")
                .setMessage("Introduce el código (nombre del beacon):")
                .setView(input)
                .setPositiveButton("Vincular", (dlg, which) -> {
                    String codigo = input.getText().toString().trim();
                    if (codigo.isEmpty()) {
                        Log.d(">>>>", "Código vacío");
                        return;
                    }
                    iconoVincular.setImageResource(R.drawable.ic_vincular_rojo);
                    vinculador.vincularPorNombre(codigo, 10_000);// timeout 10 s
                })
                .setNegativeButton("Cancelar", (d, w) -> {})
                .show();
    } //()
// ========================================================================

// ============================================================================
// verificarNodoVinculado()
// Descripción: consulta al backend si este usuario ya tiene un nodo vinculado.
// Si existe, actualiza icono, habilita recorrido y comienza a escanear.
// Diseño: userId -> GET /node/ofUser/:id -> actualizar UI
// ============================================================================
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
                    // Ya tiene nodo
                    String nombreNodo = json.getJSONObject("node").getString("name");


                    yaVinculado = true;
                    nombreNodoVinculado = nombreNodo;

                    runOnUiThread(() -> {
                        iconoVincular.setImageResource(R.drawable.ic_vincular_verde);
                        estadoBotonRecorrido(true);

                        // Empieza a leer del beacon automáticamente
                        buscarEsteDispositivoBTLE(nombreNodo);
                    });



                } else {
                    // No tiene nodo
                    runOnUiThread(() -> {
                        iconoVincular.setImageResource(R.drawable.ic_vincular_rojo);
                        estadoBotonRecorrido(false);
                    });
                }

            } catch (Exception e) {
                Log.e(">>>>", "Error procesando verificación nodo: " + e.getMessage());
            }
        }
    });
}
// ========================================================================

// ========================================================================
// desvincularNodo()
// Descripción: Borra del backend el nodo vinculado al usuario y reinicia el estado.
// Diseño: DELETE /node/ofUser/:id -> limpiar flags -> parar BLE/tracking -> actualizar UI.
// ========================================================================
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

                    iconoVincular.setImageResource(R.drawable.ic_vincular_rojo);
                    estadoBotonRecorrido(false);

                    detenerBusquedaDispositivosBTLE();
                    stopTracking();

                    textSteps.setText("---");
                    tiempoTotal.setText("---");

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Nodo desvinculado")
                            .setMessage("El beacon ha sido desvinculado correctamente.")
                            .setPositiveButton("Aceptar", null)
                            .show();
                });

            }
        });
    }
// ========================================================================

// ========================================================================
// refrescarActividad()
// Se llama DESPUÉS de vincular un nodo para que MainActivity
// se reinicie y comience a leer el beacon automáticamente.
// ========================================================================
private void refrescarActividad() {
    Intent intent = getIntent();
    contadorAndroid = -1;
    finish();
    startActivity(intent);
}



    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Detener monitor del nodo
        if (monitorEstadoNodo != null) {
            monitorEstadoNodo.detenerMonitor();
        }

        // Limpiar recursos de trackers
        if (timeTracker != null) {
            timeTracker.destroy();
        }
        if (stepTracker != null) {
            stepTracker.stopTracking();
        }
        if (gpsTracker != null) {
            gpsTracker.stopTracking();
        }
    }



} // class
// --------------------------------------------------------------
// --------------------------------------------------------------
// --------------------------------------------------------------
// --------------------------------------------------------------