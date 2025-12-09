package com.example.air_o_walk_sprint0;

public class DistanceEstimator {

    private static final int WINDOW_SIZE = 5;
    private final float[] window = new float[WINDOW_SIZE];
    private int index = 0;
    private boolean filled = false;

    public void addReading(float rssi) {
        window[index] = rssi;
        index = (index + 1) % WINDOW_SIZE;
        if (index == 0) filled = true;
    }

    public float getFilteredRSSI() {
        int count = filled ? WINDOW_SIZE : index;
        float sum = 0;
        for (int i = 0; i < count; i++) sum += window[i];
        return sum / count;
    }

    public String getDistanceCategory() {
        float rssi = getFilteredRSSI();

        if (rssi > -60) return "Muy cerca";
        if (rssi > -75) return "Cerca";
        if (rssi > -85) return "Lejos";

        return "Fuera de rango";
    }

    public int getSignalLevel() {
        float rssi = getFilteredRSSI();

        if (rssi > -55) return 5;   // strongest
        if (rssi > -65) return 4;   // strong
        if (rssi > -75) return 3;   // good
        if (rssi > -85) return 2;   // weak

        return 0;                   // no signal
    }

}
