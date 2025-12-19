package com.example.air_o_walk_sprint0;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * @class StepCounterTracker
 * @brief Gestiona el seguimiento de pasos del usuario mediante sensores del dispositivo.
 *
 * Esta clase implementa un sistema de conteo de pasos que utiliza
 * los sensores de hardware del dispositivo Android. Intenta usar
 * TYPE_STEP_COUNTER (más preciso) y si no está disponible, usa
 * TYPE_STEP_DETECTOR como alternativa.
 *
 * Funcionalidades principales:
 * - Detección automática de sensores disponibles
 * - Conteo de pasos por sesión
 * - Reset de sesión para iniciar nuevo conteo
 * - Notificación de cambios mediante listener
 *
 * @author Adenor Buret
 * @version 1.0
 */
public class StepCounterTracker implements SensorEventListener {
    //Definición de variables
    private static final String TAG = "StepCounterTracker";

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private Sensor stepDetectorSensor;

    private int totalSteps = 0;
    private int previousSteps = 0;
    private int sessionSteps = 0;

    private boolean isTracking = false;
    private boolean useFallbackDetector = false;

    private StepListener listener;

    // -----------------------------------------------------------------
    // Listener para notificar cambios en el conteo de pasos
    // -----------------------------------------------------------------
    public interface StepListener {
        void onStepCountChanged(int steps);
    }

    // --------------------------------------------------------------
    // Constructor
    // Descripción: Inicializa el gestor de sensores y detecta los sensores
    //              de pasos disponibles en el dispositivo.
    // Diseño: Context -> new StepCounterTracker() -> configura sensores
    // Parámetros: context : contexto de la aplicación para acceder a los sensores
    // --------------------------------------------------------------
    public StepCounterTracker(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        // TYPE_STEP_COUNTER: Total de pasos desde el último reinicio (más preciso)
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        // TYPE_STEP_DETECTOR: Dispara evento por cada paso detectado
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        // Registra disponibilidad de sensores en el log
        if (stepCounterSensor != null) {
            Log.d(TAG, "TYPE_STEP_COUNTER disponible");
        } else {
            Log.w(TAG, "TYPE_STEP_COUNTER NO disponible");
        }

        if (stepDetectorSensor != null) {
            Log.d(TAG, "TYPE_STEP_DETECTOR disponible");
        } else {
            Log.w(TAG, "TYPE_STEP_DETECTOR NO disponible");
        }
    }

    // --------------------------------------------------------------
    // setStepListener()
    // Descripción: Establece el listener que será notificado cuando cambie
    //              el conteo de pasos.
    // Diseño: StepListener -> setStepListener() -> listener configurado
    // Parámetros: listener : callback para recibir actualizaciones de pasos
    // --------------------------------------------------------------
    public void setStepListener(StepListener listener) {
        this.listener = listener;
    }

    // --------------------------------------------------------------
    // startTracking()
    // Descripción: Inicia el rastreo de pasos registrando el sensor apropiado.
    //              Intenta usar TYPE_STEP_COUNTER primero y si no está disponible
    //              usa TYPE_STEP_DETECTOR como alternativa.
    // Diseño: startTracking() -> registra sensor -> isTracking = true
    // --------------------------------------------------------------
    public void startTracking() {
        if (isTracking) {
            Log.w(TAG, "Ya está rastreando pasos");
            return;
        }

        boolean registered = false;

        // Intenta primero STEP_COUNTER (más preciso)
        if (stepCounterSensor != null) {
            registered = sensorManager.registerListener(
                    this,
                    stepCounterSensor,
                    SensorManager.SENSOR_DELAY_UI
            );
            useFallbackDetector = false;
            Log.d(TAG, "Intentando registrar TYPE_STEP_COUNTER: " + registered);
        }

        // Usa STEP_DETECTOR como alternativa si el contador no está disponible
        if (!registered && stepDetectorSensor != null) {
            registered = sensorManager.registerListener(
                    this,
                    stepDetectorSensor,
                    SensorManager.SENSOR_DELAY_UI
            );
            useFallbackDetector = true;
            Log.d(TAG, "Usando TYPE_STEP_DETECTOR como fallback: " + registered);
        }

        if (registered) {
            isTracking = true;
            Log.d(TAG, "Step tracking iniciado correctamente");
        } else {
            Log.e(TAG, "ERROR: No se pudo registrar ningún sensor de pasos");
        }
    }

    // --------------------------------------------------------------
    // stopTracking()
    // Descripción: Detiene el rastreo de pasos desregistrando el listener
    //              del sensor activo.
    // Diseño: stopTracking() -> desregistra sensor -> isTracking = false
    // --------------------------------------------------------------
    public void stopTracking() {
        if (!isTracking) {
            return;
        }

        sensorManager.unregisterListener(this);
        isTracking = false;
        Log.d(TAG, "Step tracking detenido");
    }

    // --------------------------------------------------------------
    // resetSession()
    // Descripción: Reinicia el conteo de pasos de la sesión actual a cero.
    //              Establece el punto de referencia para el contador acumulativo.
    // Diseño: resetSession() -> previousSteps = totalSteps -> sessionSteps = 0
    // --------------------------------------------------------------
    public void resetSession() {
        Log.d(TAG, "Reseteando sesión - previousSteps=" + previousSteps +
                ", totalSteps=" + totalSteps);

        // Si usa step counter, establece la línea base
        if (!useFallbackDetector) {
            previousSteps = totalSteps;
        }

        sessionSteps = 0;

        if (listener != null) {
            listener.onStepCountChanged(0);
        }

        Log.d(TAG, "Sesión reseteada - sessionSteps=0");
    }

    // --------------------------------------------------------------
    // onSensorChanged()
    // Descripción: Callback invocado cuando el sensor detecta un cambio.
    //              Calcula los pasos de la sesión según el tipo de sensor usado.
    // Diseño: SensorEvent -> onSensorChanged() -> actualiza sessionSteps -> notifica listener
    // Parámetros: event : evento del sensor con los datos actualizados
    // --------------------------------------------------------------
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            totalSteps = (int) event.values[0];

            Log.v(TAG, "STEP_COUNTER evento: totalSteps=" + totalSteps +
                    ", previousSteps=" + previousSteps);

            // Inicialización en la primera lectura
            if (previousSteps == 0) {
                previousSteps = totalSteps;
                Log.d(TAG, "Inicializando previousSteps=" + previousSteps);
            }

            sessionSteps = totalSteps - previousSteps;
            Log.v(TAG, "sessionSteps calculados: " + sessionSteps);

        } else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            sessionSteps++;
            Log.v(TAG, "STEP_DETECTOR evento: sessionSteps=" + sessionSteps);
        }

        if (listener != null) {
            listener.onStepCountChanged(sessionSteps);
        }
    }

    // --------------------------------------------------------------
    // onAccuracyChanged()
    // Descripción: Callback invocado cuando cambia la precisión del sensor.
    //              Registra el cambio de precisión en el log.
    // Diseño: Sensor + accuracy -> onAccuracyChanged() -> log de precisión
    // Parámetros: - sensor : sensor cuya precisión ha cambiado
    //             - accuracy : nuevo nivel de precisión
    // --------------------------------------------------------------
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        String sensorName = sensor.getType() == Sensor.TYPE_STEP_COUNTER
                ? "STEP_COUNTER" : "STEP_DETECTOR";
        String accuracyStr;

        switch (accuracy) {
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                accuracyStr = "HIGH";
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                accuracyStr = "MEDIUM";
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                accuracyStr = "LOW";
                break;
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
                accuracyStr = "UNRELIABLE";
                break;
            default:
                accuracyStr = "UNKNOWN";
        }

        Log.d(TAG, sensorName + " precisión cambió a: " + accuracyStr);
    }

    // --------------------------------------------------------------
    // getSteps()
    // Descripción: Obtiene el número de pasos contados en la sesión actual.
    // Diseño: getSteps() -> sessionSteps
    // Retorno: número de pasos de la sesión actual
    // --------------------------------------------------------------
    public int getSteps() {
        return sessionSteps;
    }

    // --------------------------------------------------------------
    // isStepCounterAvailable()
    // Descripción: Verifica si hay algún sensor de pasos disponible en el dispositivo.
    // Diseño: isStepCounterAvailable() -> true/false
    // Retorno: true si hay sensor disponible, false en caso contrario
    // --------------------------------------------------------------
    public boolean isStepCounterAvailable() {
        return stepCounterSensor != null || stepDetectorSensor != null;
    }

    // --------------------------------------------------------------
    // isTracking()
    // Descripción: Indica si el rastreo de pasos está actualmente activo.
    // Diseño: isTracking() -> true/false
    // Retorno: true si está rastreando, false en caso contrario
    // --------------------------------------------------------------
    public boolean isTracking() {
        return isTracking;
    }

    // --------------------------------------------------------------
    // getSensorInfo()
    // Descripción: Obtiene información sobre el sensor de pasos que se está utilizando.
    // Diseño: getSensorInfo() -> String con información del sensor
    // Retorno: cadena descriptiva del sensor activo o su ausencia
    // --------------------------------------------------------------
    public String getSensorInfo() {
        if (stepCounterSensor != null) {
            return "Usando TYPE_STEP_COUNTER";
        } else if (stepDetectorSensor != null) {
            return "Usando TYPE_STEP_DETECTOR (fallback)";
        } else {
            return "Sin sensor de pasos disponible";
        }
    }
}