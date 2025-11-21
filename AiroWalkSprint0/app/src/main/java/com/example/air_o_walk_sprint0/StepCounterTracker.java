package com.example.air_o_walk_sprint0;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class StepCounterTracker implements SensorEventListener {

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

    public interface StepListener {
        void onStepCountChanged(int steps);
    }

    public StepCounterTracker(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        // TYPE_STEP_COUNTER: Total steps since last reboot (more accurate)
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        // TYPE_STEP_DETECTOR: Triggers event for each step detected
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        // Log sensor availability
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

    public void setStepListener(StepListener listener) {
        this.listener = listener;
    }

    public void startTracking() {
        if (isTracking) {
            Log.w(TAG, "Ya está rastreando pasos");
            return;
        }

        boolean registered = false;

        // Try STEP_COUNTER first (more accurate)
        if (stepCounterSensor != null) {
            registered = sensorManager.registerListener(
                    this,
                    stepCounterSensor,
                    SensorManager.SENSOR_DELAY_UI  // Changed to UI for better responsiveness
            );
            useFallbackDetector = false;
            Log.d(TAG, "Intentando registrar TYPE_STEP_COUNTER: " + registered);
        }

        // Fallback to STEP_DETECTOR if counter not available or registration failed
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

    public void stopTracking() {
        if (!isTracking) {
            return;
        }

        sensorManager.unregisterListener(this);
        isTracking = false;
        Log.d(TAG, "Step tracking detenido");
    }

    public void resetSession() {
        Log.d(TAG, "Reseteando sesión - previousSteps=" + previousSteps +
                ", totalSteps=" + totalSteps);

        // If using step counter, set the baseline
        if (!useFallbackDetector) {
            previousSteps = totalSteps;
        }

        sessionSteps = 0;

        if (listener != null) {
            listener.onStepCountChanged(0);
        }

        Log.d(TAG, "Sesión reseteada - sessionSteps=0");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            totalSteps = (int) event.values[0];

            Log.v(TAG, "STEP_COUNTER evento: totalSteps=" + totalSteps +
                    ", previousSteps=" + previousSteps);

            // First time initialization
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

    public int getSteps() {
        return sessionSteps;
    }

    public boolean isStepCounterAvailable() {
        return stepCounterSensor != null || stepDetectorSensor != null;
    }

    public boolean isTracking() {
        return isTracking;
    }

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