package com.example.airowalkmenu.ui.recorrido;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.airowalkmenu.MainActivity;
import com.example.airowalkmenu.R;
import com.example.airowalkmenu.ble.TramaIBeacon;
import com.example.airowalkmenu.data.models.Gamificacion;
import com.example.airowalkmenu.data.models.NotifEstadoNodo;
import com.example.airowalkmenu.domain.MeasurementsLogica;
import com.example.airowalkmenu.AdminNotificaciones;
import com.example.airowalkmenu.services.GPSFondo;
import com.example.airowalkmenu.services.StepCounterTracker;
import com.example.airowalkmenu.services.Utilidades;
import com.example.airowalkmenu.services.WalkingTimeTracker;
import java.util.ArrayList;
import java.util.List;

public class RecorridoFragment extends Fragment {

    private static final String TAG = "RecorridoFragment";
    private static final int CODIGO_PETICION_PERMISOS = 11223344;

    // ViewModel
    private RecorridoViewModel viewModel;

    // BLE
    private BluetoothLeScanner elEscanner;
    private ScanCallback callbackDelEscaneo;
    private int contadorAndroid = 0;
    private NotifEstadoNodo monitorEstadoNodo;

    // Trackers
    private StepCounterTracker stepTracker;
    private WalkingTimeTracker timeTracker;
    private GPSFondo gpsTracker;
    private boolean isTracking = false;

    // UI
    private TextView textMajor;
    private TextView textMinor;
    private TextView textSteps;
    private TextView tiempoTotal;
    private Button trackButton;

    // Usuario
    private int userId;
    private String nombreNodoVinculado;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(RecorridoViewModel.class);
        View root = inflater.inflate(R.layout.fragment_recorrido, container, false);

        // Obtener userId del MainActivity
        if (getActivity() instanceof MainActivity) {
            userId = ((MainActivity) getActivity()).getUserId();
        }

        // Inicializar vistas
        textMajor = root.findViewById(R.id.textMajor);
        textMinor = root.findViewById(R.id.textMinor);
        textSteps = root.findViewById(R.id.distanciaTotal);
        tiempoTotal = root.findViewById(R.id.tiempoTotal);
        trackButton = root.findViewById(R.id.track);

        // Valores iniciales
        textMajor.setText("CO2: ---");
        textMinor.setText("Temp: ---");
        textSteps.setText("0 pasos");
        tiempoTotal.setText("00:00:00");

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Observar datos del ViewModel
        observarViewModel();

        // Solicitar permisos necesarios
        solicitarPermisos();

        // Configurar botón de tracking
        trackButton.setOnClickListener(v -> {
            if (!isTracking) {
                startTracking();
            } else {
                stopTracking();
            }
        });

        // Inicializar trackers
        inicializarTrackers();
    }

    // ==============================================================================================================
    // OBSERVAR VIEWMODEL
    // ==============================================================================================================
    private void observarViewModel() {
        // Observar el nombre del nodo vinculado desde el ViewModel
        viewModel.getNombreNodoVinculado().observe(getViewLifecycleOwner(), nombre -> {
            if (nombre != null && !nombre.isEmpty()) {
                nombreNodoVinculado = nombre;
                Log.d(TAG, "Nodo vinculado detectado: " + nombre);

                // Inicializar Bluetooth
                inicializarBluetooth();

                // Iniciar escaneo automático del nodo vinculado
                buscarEsteDispositivoBTLE(nombre);

                // Iniciar monitor de estado del nodo
                monitorEstadoNodo = new NotifEstadoNodo(requireContext(), nombre);
                monitorEstadoNodo.iniciarMonitor();

                // Habilitar botón de tracking
                trackButton.setEnabled(true);
            } else {
                Log.w(TAG, "No hay nodo vinculado");
                trackButton.setEnabled(false);
                Toast.makeText(requireContext(),
                        "Primero vincula un nodo desde la sección 'Vincular'",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // ==============================================================================================================
    // SOLICITAR PERMISOS
    // ==============================================================================================================
    private void solicitarPermisos() {
        ArrayList<String> permisosNecesarios = new ArrayList<>();

        // Permisos de Bluetooth
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            permisosNecesarios.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            permisosNecesarios.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        // Permiso de localización
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permisosNecesarios.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // Permiso de Activity Recognition (Android Q+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                permisosNecesarios.add(Manifest.permission.ACTIVITY_RECOGNITION);
            }
        }

        // Permiso de notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permisosNecesarios.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // Solicitar permisos si es necesario
        if (!permisosNecesarios.isEmpty()) {
            Log.d(TAG, "Solicitando " + permisosNecesarios.size() + " permisos");
            requestPermissions(permisosNecesarios.toArray(new String[0]), CODIGO_PETICION_PERMISOS);
        } else {
            Log.d(TAG, "Todos los permisos ya concedidos");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CODIGO_PETICION_PERMISOS) {
            boolean todosOtorgados = true;

            for (int i = 0; i < permissions.length; i++) {
                String permiso = permissions[i];
                boolean otorgado = grantResults[i] == PackageManager.PERMISSION_GRANTED;

                Log.d(TAG, "Permiso " + permiso + ": " + (otorgado ? "OTORGADO" : "DENEGADO"));

                if (!otorgado) {
                    todosOtorgados = false;

                    // Si es Activity Recognition, advertir que el contador de pasos no funcionará
                    if (permiso.equals(Manifest.permission.ACTIVITY_RECOGNITION)) {
                        Toast.makeText(requireContext(),
                                "Sin permiso de actividad física, el contador de pasos no funcionará",
                                Toast.LENGTH_LONG).show();
                    }
                }
            }

            if (todosOtorgados) {
                Log.d(TAG, "Todos los permisos otorgados");
                inicializarBluetooth();
            } else {
                Log.w(TAG, "Algunos permisos fueron denegados");
            }
        }
    }

    // ==============================================================================================================
    // INICIALIZAR BLUETOOTH
    // ==============================================================================================================
    private void inicializarBluetooth() {
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();

        if (bta == null) {
            Log.e(TAG, "Bluetooth no disponible en este dispositivo");
            Toast.makeText(requireContext(), "Bluetooth no disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bta.isEnabled()) {
            Log.w(TAG, "Bluetooth desactivado");
            Toast.makeText(requireContext(), "Por favor, activa el Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        elEscanner = bta.getBluetoothLeScanner();

        if (elEscanner == null) {
            Log.e(TAG, "Scanner BLE no disponible");
            Toast.makeText(requireContext(), "Scanner BLE no disponible", Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "Scanner BLE inicializado correctamente");
        }
    }

    // ==============================================================================================================
    // BUSCAR DISPOSITIVO BLE ESPECÍFICO
    // ==============================================================================================================
    private void buscarEsteDispositivoBTLE(final String dispositivoBuscado) {
        if (elEscanner == null) {
            Log.e(TAG, "Scanner BLE no inicializado");
            inicializarBluetooth();
            return;
        }

        Log.d(TAG, "Iniciando búsqueda del dispositivo: " + dispositivoBuscado);

        callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                super.onScanResult(callbackType, resultado);
                mostrarMedicion(resultado);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                Log.d(TAG, "Batch scan results recibidos: " + results.size());
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e(TAG, "Error en escaneo BLE, código: " + errorCode);
                Toast.makeText(requireContext(), "Error en escaneo BLE: " + errorCode, Toast.LENGTH_SHORT).show();
            }
        };

        // Filtro por nombre de dispositivo
        ScanFilter sf = new ScanFilter.Builder()
                .setDeviceName(dispositivoBuscado)
                .build();

        List<ScanFilter> filtros = new ArrayList<>();
        filtros.add(sf);

        // Configuración de escaneo (baja latencia)
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        try {
            elEscanner.startScan(filtros, settings, callbackDelEscaneo);
            Log.d(TAG, "Escaneo BLE iniciado para: " + dispositivoBuscado);
        } catch (SecurityException e) {
            Log.e(TAG, "Error de permisos al iniciar escaneo: " + e.getMessage());
            Toast.makeText(requireContext(), "Error: permisos insuficientes", Toast.LENGTH_SHORT).show();
        }
    }

    // ==============================================================================================================
    // MOSTRAR MEDICIÓN
    // Procesa la trama iBeacon y actualiza la UI con los valores de CO2 y temperatura
    // ==============================================================================================================
    private void mostrarMedicion(ScanResult resultado) {
        try {
            // Obtener bytes del scan record
            byte[] bytes = resultado.getScanRecord().getBytes();

            // Parsear trama iBeacon
            TramaIBeacon tib = new TramaIBeacon(bytes);

            // Extraer datos
            byte[] major = tib.getMajor();
            float medicionGas = Utilidades.bytesToFloat(major) / 100;

            byte[] minor = tib.getMinor();
            float medicionTemperatura = Utilidades.bytesToFloat(minor) / 100;

            int contadorArduino = tib.getTxPower();

            // Verificar si es una medición nueva (evitar duplicados)
            if (contadorArduino == this.contadorAndroid) {
                Log.d(TAG, "Medición duplicada ignorada (contador: " + contadorArduino + ")");
                return;
            }

            // Actualizar contador
            this.contadorAndroid = contadorArduino;

            Log.d(TAG, String.format("Nueva medición - CO2: %.2f ppm, Temp: %.2f °C, Contador: %d",
                    medicionGas, medicionTemperatura, contadorArduino));

            // Notificar al monitor de estado del nodo
            if (monitorEstadoNodo != null) {
                monitorEstadoNodo.onBeaconRecibido(medicionGas, medicionTemperatura);
            }

            // Actualizar UI en el hilo principal
            requireActivity().runOnUiThread(() -> {
                // Revisar y notificar si hay mala calidad del aire
                AdminNotificaciones.revisarYNotificar(requireContext(), medicionGas);

                // Actualizar textos
                textMajor.setText(String.format("CO2: %.2f ppm", medicionGas));
                textMinor.setText(String.format("Temp: %.2f °C", medicionTemperatura));
            });

        } catch (Exception e) {
            Log.e(TAG, "Error procesando medición: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==============================================================================================================
    // INICIALIZAR TRACKERS
    // ==============================================================================================================
    private void inicializarTrackers() {
        try {
            // Inicializar StepCounterTracker
            stepTracker = new StepCounterTracker(requireContext());

            // Verificar disponibilidad del sensor de pasos
            if (!stepTracker.isStepCounterAvailable()) {
                Log.w(TAG, "Sensor de pasos no disponible");
                Toast.makeText(requireContext(),
                        "Este dispositivo no tiene sensor de pasos",
                        Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "Sensor de pasos disponible: " + stepTracker.getSensorInfo());
            }

            // Inicializar TimeTracker
            timeTracker = new WalkingTimeTracker();

            // Inicializar GPSTracker
            gpsTracker = new GPSFondo(requireContext());

            // Configurar listener para step counter
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
                        Log.d(TAG, String.format("GPS Update - Lat: %.6f, Lon: %.6f, Accuracy: %.1fm",
                                location.getLatitude(), location.getLongitude(), location.getAccuracy()));

                        // Aquí puedes guardar la ubicación en el backend si es necesario
                        // viewModel.guardarUbicacion(location);
                    }
                }

                @Override
                public void onLocationError(String error) {
                    Log.e(TAG, "GPS Error: " + error);
                }
            });

            Log.d(TAG, "Trackers inicializados correctamente");

        } catch (Exception e) {
            Log.e(TAG, "Error inicializando trackers: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error inicializando trackers", Toast.LENGTH_SHORT).show();
        }
    }

    // ==============================================================================================================
    // INICIAR TRACKING
    // ==============================================================================================================
    private void startTracking() {
        // Verificar que los trackers estén inicializados
        if (stepTracker == null || timeTracker == null || gpsTracker == null) {
            Toast.makeText(requireContext(), "Error: trackers no inicializados", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar que el sensor de pasos esté disponible
        if (!stepTracker.isStepCounterAvailable()) {
            Toast.makeText(requireContext(),
                    "Este dispositivo no tiene sensor de pasos",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Verificar que haya nodo vinculado
        if (nombreNodoVinculado == null || nombreNodoVinculado.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Primero vincula un nodo desde la sección 'Vincular'",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "Iniciando tracking...");

        isTracking = true;
        trackButton.setText("Detener Recorrido");

        // Reiniciar trackers
        stepTracker.resetSession();
        timeTracker.reset();
        gpsTracker.resetTracking();

        // Iniciar trackers
        stepTracker.startTracking();
        timeTracker.startTracking();
        gpsTracker.startTracking();

        Toast.makeText(requireContext(), "Recorrido iniciado", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Tracking iniciado correctamente");
    }

    // ==============================================================================================================
    // DETENER TRACKING
    // ==============================================================================================================
    private void stopTracking() {
        // Verificar que los trackers estén inicializados
        if (stepTracker == null || timeTracker == null || gpsTracker == null) {
            Toast.makeText(requireContext(), "Error: trackers no inicializados", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Deteniendo tracking...");

        isTracking = false;
        trackButton.setText("Iniciar Recorrido");

        // Detener trackers
        stepTracker.stopTracking();
        timeTracker.stopTracking();
        gpsTracker.stopTracking();

        // Obtener valores finales
        int pasos = stepTracker.getSteps();
        float horasTotales = timeTracker.getElapsedTimeHours();
        int minutosTotales = (int) timeTracker.getElapsedTimeMinutes();

        Log.d(TAG, String.format("Recorrido finalizado - Pasos: %d, Tiempo: %.2f horas", pasos, horasTotales));

        // Calcular puntos
        Gamificacion game = new Gamificacion(userId);
        int puntos = game.calcularPuntosMedianteDistancia(pasos);
        game.setUltimosPuntosObtenidos(puntos);

        // Guardar estadísticas diarias
        MeasurementsLogica medidas = new MeasurementsLogica(userId, pasos, puntos, horasTotales);
        medidas.guardarDailyStats();

        Toast.makeText(requireContext(),
                String.format("Recorrido guardado: %d pasos, %d puntos", pasos, puntos),
                Toast.LENGTH_LONG).show();

        // Abrir pantalla de resumen
        Intent intent = new Intent(requireActivity(), AirQualityResumen.class);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("PASOS", pasos);
        intent.putExtra("TIEMPO", minutosTotales);
        startActivity(intent);

        Log.d(TAG, "Tracking detenido correctamente");
    }

    // ==============================================================================================================
    // ACTUALIZAR UI DE TRACKING
    // ==============================================================================================================
    private void updateTrackingUI(int steps, long hours, long minutes, long seconds) {
        requireActivity().runOnUiThread(() -> {
            textSteps.setText(String.format("%d pasos", steps));
            tiempoTotal.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
        });
    }

    // ==============================================================================================================
    // DETENER BÚSQUEDA DE DISPOSITIVOS BLE
    // ==============================================================================================================
    private void detenerBusquedaDispositivosBTLE() {
        if (callbackDelEscaneo != null && elEscanner != null) {
            try {
                elEscanner.stopScan(callbackDelEscaneo);
                callbackDelEscaneo = null;
                Log.d(TAG, "Escaneo BLE detenido");
            } catch (SecurityException e) {
                Log.e(TAG, "Error deteniendo escaneo: " + e.getMessage());
            }
        }
    }

    // ==============================================================================================================
    // LIFECYCLE - onDestroyView
    // ==============================================================================================================
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        Log.d(TAG, "onDestroyView - Limpiando recursos");

        // Detener escaneo BLE
        detenerBusquedaDispositivosBTLE();

        // Detener monitor de estado del nodo
        if (monitorEstadoNodo != null) {
            monitorEstadoNodo.detenerMonitor();
            monitorEstadoNodo = null;
        }

        // Limpiar trackers
        if (timeTracker != null) {
            timeTracker.destroy();
            timeTracker = null;
        }

        if (stepTracker != null) {
            stepTracker.stopTracking();
            stepTracker = null;
        }

        if (gpsTracker != null) {
            gpsTracker.stopTracking();
            gpsTracker = null;
        }

        // Limpiar referencias a vistas
        textMajor = null;
        textMinor = null;
        textSteps = null;
        tiempoTotal = null;
        trackButton = null;

        Log.d(TAG, "Recursos limpiados correctamente");
    }
}