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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

    // Referencias a las vistas
    private TextView textMajor;
    private TextView textMinor;
    private TextView distanciaTotal;
    private TextView tiempoTotal;
    private Button trackButton;

    //Variables vinculacion
    private VinculadorBLE vinculador;
    private ImageView iconoVincular;

    // Trackers para distancia y tiempo
    private StepCounterTracker stepTracker;
    private WalkingTimeTracker timeTracker;

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


        // Llamamos a la notificación desde el hilo principal (UI thread)
        runOnUiThread(() -> {
            AdminNotificaciones.revisarYNotificar(this, medicionGas);
        });
        textMajor.setText("03(ppm): " + medicionGas);
        textMinor.setText("Temperatura(ºC): " + medicionTemperatura);
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

    // ------------------------------------------------------------------
    // NUEVO: Método para controlar el tracking de distancia y tiempo
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
        Log.d(ETIQUETA_LOG, " startTracking(): iniciando tracking de distancia y tiempo");

        isTracking = true;
        trackButton.setText("Detener Recorrida");

        // Reiniciar trackers
        stepTracker.resetSession();
        timeTracker.reset();

        // Iniciar step counter
        stepTracker.startTracking();

        // Iniciar time tracker
        timeTracker.startTracking();

        Log.d(ETIQUETA_LOG, " startTracking(): tracking iniciado");
    }

    private void stopTracking() {
        Log.d(ETIQUETA_LOG, " stopTracking(): deteniendo tracking");

        isTracking = false;
        trackButton.setText("Activar Recorrida");

        // Detener trackers (pero mantener los valores actuales)
        stepTracker.stopTracking();
        timeTracker.stopTracking();

        Log.d(ETIQUETA_LOG, " stopTracking(): tracking detenido - valores congelados");
    }

    // ------------------------------------------------------------------
    // Actualiza la UI con los valores de distancia y tiempo
    // ------------------------------------------------------------------
    private void updateTrackingUI(double distanceMeters, int steps,
                                  long hours, long minutes, long seconds) {
        runOnUiThread(() -> {
            // Actualizar distancia en metros
            if (distanceMeters >= 1000) {
                // Si es más de 1 km, mostrar en kilómetros
                distanciaTotal.setText(String.format("%.2f km", distanceMeters / 1000.0));
            } else {
                // Mostrar en metros
                distanciaTotal.setText(String.format("%.0f m", distanceMeters));
            }

            // Actualizar tiempo
            tiempoTotal.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));

            Log.d(ETIQUETA_LOG, String.format(" UI actualizada: %.2f m, %d pasos, %02d:%02d:%02d",
                    distanceMeters, steps, hours, minutes, seconds));
        });
    }

    // ------------------------------------------------------------------
    // Inicializa el adaptador Bluetooth y solicita permisos si es necesario
    // ------------------------------------------------------------------
    private void inicializarBlueTooth() {
        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): obtenemos adaptador BT ");

        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();

        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): habilitamos adaptador BT ");

        if (!bta.isEnabled()) {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): Bluetooth desactivado, solicitando activación...");

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, CODIGO_PETICION_PERMISOS);
        } else {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): Bluetooth ya está activado");
        }

        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): habilitado =  " + bta.isEnabled() );
        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): estado =  " + bta.getState() );
        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): obtenemos escaner btle ");

        this.elEscanner = bta.getBluetoothLeScanner();

        if ( this.elEscanner == null ) {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): Socorro: NO hemos obtenido escaner btle  !!!!");
        }

        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): voy a perdir permisos (si no los tuviera) !!!!");

        // Solicita permisos necesarios para Bluetooth y localización
        if (
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        )
        {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    CODIGO_PETICION_PERMISOS
            );

        }
        else {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): parece que YA tengo los permisos necesarios !!!!");
        }
    } // ()

    // ------------------------------------------------------------------
    // Solicita permisos adicionales para step counter
    // ------------------------------------------------------------------
    private void solicitarPermisosTracking() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                        CODIGO_PETICION_PERMISOS + 1
                );
            }
        }
    }


    //Funcion para activar boton de inicio de recorrido
    private void estadoBotonRecorrido(boolean estadoDispotivoVinculado){
        trackButton.setEnabled(estadoDispotivoVinculado);
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
        }

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
// VINCULAR
// Descripción: Inicializa el icono de vinculación y crea el VinculadorBLE para gestionar el enlace
// con el beacon. Si se vincula correctamente, registra el nodo en el backend.
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
                        String userId = Integer.toString((idUsuario));  // !!! temporal REPLACE WITH REAL USERID
                        String nombreNodo = vinculador.getNombreNodoActual();
                        RegistroNodo registro = new RegistroNodo(userId, nombreNodo);
                        registro.registrarNodo();
                        buscarEsteDispositivoBTLE(nombreNodo);
                        estadoBotonRecorrido(true);
                        // ===================================================================
                        iconoVincular.setImageResource(R.drawable.ic_vincular_verde);
                        break;
                    case TIMEOUT:
                    case ERROR:
                        iconoVincular.setImageResource(R.drawable.ic_vincular_rojo);
                        estadoBotonRecorrido(false);
                        break;
                    case ESCANEANDO:
                    case IDLE:
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
// ==============================================================================================================

        // Solicitar permisos para step counter
        solicitarPermisosTracking();

        // Inicializar trackers
        stepTracker = new StepCounterTracker(this);
        timeTracker = new WalkingTimeTracker();

        // Configurar listener para step counter
        stepTracker.setDistanceListener((distanceMeters, steps) -> {
            if (isTracking) {
                WalkingTimeTracker.TimeComponents time = timeTracker.getTimeComponents();
                updateTrackingUI(distanceMeters, steps, time.hours, time.minutes, time.seconds);
            }
        });

        // Configurar listener para time tracker
        timeTracker.setTimeUpdateListener((hours, minutes, seconds, totalSeconds) -> {
            if (isTracking) {
                double distance = stepTracker.getDistanceMeters();
                int steps = stepTracker.getSteps();
                updateTrackingUI(distance, steps, hours, minutes, seconds);
            }
        });

        // Valores iniciales
        distanciaTotal.setText("---");
        tiempoTotal.setText("---");

        Log.d(ETIQUETA_LOG, " onCreate(): termina ");

    } // onCreate()

    // ------------------------------------------------------------------
    // Callback para el resultado de la petición de permisos
    // ------------------------------------------------------------------
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults);

        switch (requestCode) {
            case CODIGO_PETICION_PERMISOS:
                // Si se conceden los permisos, se puede continuar con la funcionalidad BLE
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.d(ETIQUETA_LOG, " onRequestPermissionResult(): permisos concedidos  !!!!");
                }  else {
                    Log.d(ETIQUETA_LOG, " onRequestPermissionResult(): Socorro: permisos NO concedidos  !!!!");
                }
                return;
            case CODIGO_PETICION_PERMISOS + 1:
                // Permisos para ACTIVITY_RECOGNITION
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(ETIQUETA_LOG, " onRequestPermissionResult(): permiso ACTIVITY_RECOGNITION concedido");
                } else {
                    Log.d(ETIQUETA_LOG, " onRequestPermissionResult(): permiso ACTIVITY_RECOGNITION denegado");
                }
                return;
        }
    } // ()

// ==============================================================================================================
// botonVincularPulsado()
// Descripción: Muestra un diálogo para introducir el nombre del beacon (ej: "GTI") y
// llama al VinculadorBLE para iniciar la vinculación. Si el código es válido, registra el nodo
// en el backend y actualiza el icono de estado.
//
// Diseño: vista:View -> botonVincularPulsado() -> muestra diálogo / vincula / registra nodo
// ==============================================================================================================
    public void botonVincularPulsado(View v) {
        EditText input = new EditText(this);
        input.setHint("Ej: GTI");

        new AlertDialog.Builder(this)
                .setTitle("Vincular Beacon")
                .setMessage("Introduce el código (nombre del beacon):")
                .setView(input)
                .setPositiveButton("Vincular", (dlg, which) -> {
                    String codigo = input.getText().toString().trim();
                    vinculador.vincularPorNombre(codigo, 10_000);
                    // registra el nodo inmediatamente
                    RegistroNodo registro = new RegistroNodo("12345", codigo);
                    registro.registrarNodo();
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


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpiar recursos
        if (timeTracker != null) {
            timeTracker.destroy();
        }
        if (stepTracker != null) {
            stepTracker.stopTracking();
        }
    }

} // class
// --------------------------------------------------------------
// --------------------------------------------------------------
// --------------------------------------------------------------
// --------------------------------------------------------------