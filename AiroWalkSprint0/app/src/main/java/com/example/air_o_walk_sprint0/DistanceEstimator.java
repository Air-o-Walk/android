package com.example.air_o_walk_sprint0;
// --------------------------------------------------------------
// DistanceEstimator.java
// Autor: Meryame Ait Boumlik
// Descripción:
//   Clase auxiliar que suaviza lecturas RSSI mediante ventana deslizante
//   y clasifica la distancia aproximada del beacon. También asigna un
//   nivel de señal (0–5) para la UI.
// --------------------------------------------------------------
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
        if (rssi > -60) return "Muy cerca";   // ~0–1m
        if (rssi > -70) return "Cerca";       // ~1–2m
        if (rssi > -78) return "Lejos";       // ~2–3m
        return "Señal muy débil";              // >3m
    }

    public int getSignalLevel() {
        float rssi = getFilteredRSSI();

        if (rssi > -60) return 5;   // strongest
        if (rssi > -70) return 4;   // strong
        if (rssi > -78) return 3;   // good
        if (rssi > -85) return 2;   // weak

        return 0;                   // no signal
    }

}
