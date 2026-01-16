package com.example.air_o_walk_sprint0;

import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @class DistanceEstimator
 * @brief Estima la distancia aproximada a un beacon BLE a partir del RSSI.
 *
 * Esta clase actúa como un componente auxiliar que suaviza las lecturas
 * de intensidad de señal (RSSI) mediante una ventana deslizante y
 * clasifica la distancia aproximada al beacon.
 *
 * Además, asigna un nivel de señal (0–5) que se utiliza para
 * la representación gráfica en la interfaz de usuario.
 *
 * No depende de componentes Android, por lo que puede reutilizarse
 * fácilmente en otras partes de la aplicación.
 *
 * @author Meryame Ait Boumlik
 * @version 1.0
 */
public class DistanceEstimator {

    private static final int WINDOW_SIZE = 5;
    private final float[] window = new float[WINDOW_SIZE];
    private int index = 0;
    private boolean filled = false;



    private ScheduledExecutorService scheduler;
    private boolean isMonitoring = false;


    public interface OnSignalChangedListener {
        void onSignalLevelChanged(int nivel);
    }
    private OnSignalChangedListener listener;

    // 2. Método para asignar el listener desde el Activity
    public void setOnSignalChangedListener(OnSignalChangedListener listener) {
        this.listener = listener;
    }
    /**
     * Añade una nueva lectura RSSI a la ventana deslizante.
     *
     * @param rssi valor RSSI recibido del beacon
     */
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
/*



    // --- MÉTODOS PARA EL BACKGROUND TASK ---

    /**
     * Inicia el monitoreo automático.
     * @param intervaloMs Tiempo entre cada ejecución (ej. 1000 para 1 segundo)
     */
    /*public void startAutoMonitoring(long intervaloMs) {
        if (isMonitoring) return; // Evita duplicar tareas

        scheduler = Executors.newSingleThreadScheduledExecutor();
        isMonitoring = true;

        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                // Aquí se ejecuta lo que quieres cada X tiempo
                int nivel = getSignalLevel();

                if (nivel == 0 && listener != null) {
                    listener.onSignalLevelChanged(nivel);
                }
                ;
                // NOTA: Si necesitas actualizar la UI desde aquí,
                // deberás usar un runOnUiThread o un Callback.
            }
        }, 0, intervaloMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Detiene el hilo de ejecución para liberar memoria.
     */
    /*public void stopAutoMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            isMonitoring = false;
            Log.d("DistanceEstimator", "Monitoreo detenido.");
        }
    }*/

}
