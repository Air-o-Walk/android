package com.example.air_o_walk_sprint0;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class StepCounterTracker implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private Sensor stepDetectorSensor;

    private int totalSteps = 0;
    private int previousSteps = 0;
    private int sessionSteps = 0;

    // Average step length in meters (adjustable per user)
    private double stepLengthMeters = 0.762; // ~30 inches average
    //private double stepLengthMeters = 0.45;
    private DistanceListener listener;

    public interface DistanceListener {
        void onDistanceChanged(double distanceMeters, int steps);
    }

    public StepCounterTracker(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        // TYPE_STEP_COUNTER: Total steps since last reboot (more accurate)
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        // TYPE_STEP_DETECTOR: Triggers event for each step detected
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
    }

    public void setStepLength(double meters) {
        this.stepLengthMeters = meters;
    }

    public void setDistanceListener(DistanceListener listener) {
        this.listener = listener;
    }

    public void startTracking() {
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        } else if (stepDetectorSensor != null) {
            // Fallback to step detector if counter not available
            sensorManager.registerListener(this, stepDetectorSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void stopTracking() {
        sensorManager.unregisterListener(this);
    }

    public void resetSession() {
        previousSteps = totalSteps;
        sessionSteps = 0;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            totalSteps = (int) event.values[0];

            if (previousSteps == 0) {
                previousSteps = totalSteps;
            }

            sessionSteps = totalSteps - previousSteps;

        } else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            sessionSteps++;
        }

        double distanceMeters = sessionSteps * stepLengthMeters;

        if (listener != null) {
            listener.onDistanceChanged(distanceMeters, sessionSteps);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accuracy changes if needed
    }

    public double getDistanceMeters() {
        return sessionSteps * stepLengthMeters;
    }

    public double getDistanceKilometers() {
        return getDistanceMeters() / 1000.0;
    }

    public int getSteps() {
        return sessionSteps;
    }

    // Calculate personalized step length based on height
    public static double calculateStepLength(double heightCm) {
        // Rule of thumb: step length ≈ height * 0.43
        return (heightCm / 100.0) * 0.43;
    }

    public boolean isStepCounterAvailable() {
        return stepCounterSensor != null || stepDetectorSensor != null;
    }
}