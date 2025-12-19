package com.example.air_o_walk_sprint0;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.TimeUnit;

/**
 * @class WalkingTimeTracker
 * @brief Gestiona el seguimiento del tiempo de caminata del usuario.
 *
 * Esta clase proporciona funcionalidades para medir y controlar el tiempo
 * de actividad del usuario durante sus caminatas. Permite iniciar, pausar,
 * reanudar y detener el seguimiento, además de calcular métricas derivadas
 * como velocidad y ritmo.
 *
 * Funcionalidades principales:
 * - Seguimiento de tiempo en tiempo real
 * - Control de inicio, pausa y reanudación
 * - Cálculo de tiempo transcurrido en diferentes unidades
 * - Formateo de tiempo para visualización
 * - Cálculo de velocidad y ritmo basado en distancia
 * - Notificaciones periódicas mediante listener
 *
 * @author Adenor Buret
 * @version 1.0
 */
public class WalkingTimeTracker {

    //Definición de variables
    private long startTime = 0;
    private long totalElapsedTime = 0; // Tiempo total en milisegundos
    private long pausedTime = 0;

    private boolean isTracking = false;
    private boolean isPaused = false;

    private Handler handler;
    private Runnable updateRunnable;

    private TimeUpdateListener listener;

    // -----------------------------------------------------------------
    // Listener para actualizaciones de tiempo en tiempo real
    // -----------------------------------------------------------------
    public interface TimeUpdateListener {
        void onTimeUpdate(long hours, long minutes, long seconds, long totalSeconds);
    }

    // --------------------------------------------------------------
    // Constructor
    // Descripción: Inicializa el tracker con un Handler para actualizaciones periódicas.
    // Diseño: new WalkingTimeTracker() -> inicializa Handler
    // --------------------------------------------------------------
    public WalkingTimeTracker() {
        handler = new Handler(Looper.getMainLooper());
    }

    // --------------------------------------------------------------
    // startTracking()
    // Descripción: Inicia el seguimiento de tiempo. Si ya está rastreando pero
    //              pausado, reanuda en lugar de reiniciar.
    // Diseño: startTracking() -> startTime = ahora -> isTracking = true -> startUpdating()
    // --------------------------------------------------------------
    public void startTracking() {
        if (!isTracking) {
            startTime = System.currentTimeMillis();
            isTracking = true;
            isPaused = false;
            startUpdating();
        } else if (isPaused) {
            resume();
        }
    }

    // --------------------------------------------------------------
    // pause()
    // Descripción: Pausa el seguimiento manteniendo el tiempo acumulado.
    //              El tiempo transcurrido se guarda para continuar después.
    // Diseño: pause() -> pausedTime = ahora -> totalElapsedTime += delta -> stopUpdating()
    // --------------------------------------------------------------
    public void pause() {
        if (isTracking && !isPaused) {
            isPaused = true;
            pausedTime = System.currentTimeMillis();
            totalElapsedTime += (pausedTime - startTime);
            stopUpdating();
        }
    }

    // --------------------------------------------------------------
    // resume()
    // Descripción: Reanuda el seguimiento después de una pausa, manteniendo
    //              el tiempo acumulado anterior.
    // Diseño: resume() -> startTime = ahora -> isPaused = false -> startUpdating()
    // --------------------------------------------------------------
    public void resume() {
        if (isTracking && isPaused) {
            isPaused = false;
            startTime = System.currentTimeMillis();
            startUpdating();
        }
    }

    // --------------------------------------------------------------
    // stopTracking()
    // Descripción: Detiene completamente el seguimiento, actualizando el
    //              tiempo total transcurrido.
    // Diseño: stopTracking() -> totalElapsedTime += delta -> isTracking = false
    // --------------------------------------------------------------
    public void stopTracking() {
        if (isTracking) {
            if (!isPaused) {
                totalElapsedTime += (System.currentTimeMillis() - startTime);
            }
            isTracking = false;
            isPaused = false;
            stopUpdating();
        }
    }

    // --------------------------------------------------------------
    // reset()
    // Descripción: Reinicia todos los contadores de tiempo a cero y detiene
    //              el seguimiento si estaba activo.
    // Diseño: reset() -> stopTracking() -> todos los tiempos = 0 -> notifica listener
    // --------------------------------------------------------------
    public void reset() {
        stopTracking();
        startTime = 0;
        totalElapsedTime = 0;
        pausedTime = 0;

        if (listener != null) {
            listener.onTimeUpdate(0, 0, 0, 0);
        }
    }

    // --------------------------------------------------------------
    // getElapsedTimeMillis()
    // Descripción: Calcula y devuelve el tiempo transcurrido en milisegundos,
    //              incluyendo el tiempo de la sesión actual si está activa.
    // Diseño: getElapsedTimeMillis() -> calcula tiempo total -> milisegundos
    // Retorno: tiempo transcurrido en milisegundos
    // --------------------------------------------------------------
    public long getElapsedTimeMillis() {
        if (!isTracking) {
            return totalElapsedTime;
        }

        if (isPaused) {
            return totalElapsedTime;
        }

        return totalElapsedTime + (System.currentTimeMillis() - startTime);
    }

    // --------------------------------------------------------------
    // getElapsedTimeSeconds()
    // Descripción: Obtiene el tiempo transcurrido en segundos.
    // Diseño: getElapsedTimeSeconds() -> millis -> segundos
    // Retorno: tiempo transcurrido en segundos
    // --------------------------------------------------------------
    public long getElapsedTimeSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(getElapsedTimeMillis());
    }

    // --------------------------------------------------------------
    // getElapsedTimeMinutes()
    // Descripción: Obtiene el tiempo transcurrido en minutos.
    // Diseño: getElapsedTimeMinutes() -> millis -> minutos
    // Retorno: tiempo transcurrido en minutos
    // --------------------------------------------------------------
    public long getElapsedTimeMinutes() {
        return TimeUnit.MILLISECONDS.toMinutes(getElapsedTimeMillis());
    }

    // --------------------------------------------------------------
    // getElapsedTimeHours()
    // Descripción: Obtiene el tiempo transcurrido en horas.
    // Diseño: getElapsedTimeHours() -> millis -> horas
    // Retorno: tiempo transcurrido en horas
    // --------------------------------------------------------------
    public long getElapsedTimeHours() {
        return TimeUnit.MILLISECONDS.toHours(getElapsedTimeMillis());
    }

    // --------------------------------------------------------------
    // getFormattedTime()
    // Descripción: Formatea el tiempo transcurrido en formato HH:MM:SS.
    // Diseño: getFormattedTime() -> calcula componentes -> String "HH:MM:SS"
    // Retorno: string con el tiempo formateado en formato largo
    // --------------------------------------------------------------
    public String getFormattedTime() {
        long millis = getElapsedTimeMillis();

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    // --------------------------------------------------------------
    // getFormattedTimeShort()
    // Descripción: Formatea el tiempo transcurrido en formato MM:SS.
    //              Útil para visualizaciones compactas.
    // Diseño: getFormattedTimeShort() -> calcula componentes -> String "MM:SS"
    // Retorno: string con el tiempo formateado en formato corto
    // --------------------------------------------------------------
    public String getFormattedTimeShort() {
        long millis = getElapsedTimeMillis();

        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    // --------------------------------------------------------------
    // getTimeComponents()
    // Descripción: Obtiene los componentes de tiempo (horas, minutos, segundos)
    //              como un objeto estructurado.
    // Diseño: getTimeComponents() -> calcula componentes -> TimeComponents
    // Retorno: objeto TimeComponents con los valores desglosados
    // --------------------------------------------------------------
    public TimeComponents getTimeComponents() {
        long millis = getElapsedTimeMillis();

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        return new TimeComponents(hours, minutes, seconds, totalSeconds);
    }

    // --------------------------------------------------------------
    // setTimeUpdateListener()
    // Descripción: Establece el listener que recibirá actualizaciones de tiempo
    //              cada segundo mientras el tracking esté activo.
    // Diseño: TimeUpdateListener -> setTimeUpdateListener() -> listener configurado
    // Parámetros: listener : callback para recibir actualizaciones periódicas
    // --------------------------------------------------------------
    public void setTimeUpdateListener(TimeUpdateListener listener) {
        this.listener = listener;
    }

    // --------------------------------------------------------------
    // startUpdating()
    // Descripción: Inicia las actualizaciones periódicas (cada segundo) del tiempo
    //              notificando al listener registrado.
    // Diseño: startUpdating() -> crea Runnable -> Handler.post cada 1000ms
    // --------------------------------------------------------------
    private void startUpdating() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTracking && !isPaused && listener != null) {
                    TimeComponents time = getTimeComponents();
                    listener.onTimeUpdate(time.hours, time.minutes,
                            time.seconds, time.totalSeconds);
                }
                handler.postDelayed(this, 1000); // Actualizar cada segundo
            }
        };
        handler.post(updateRunnable);
    }

    // --------------------------------------------------------------
    // stopUpdating()
    // Descripción: Detiene las actualizaciones periódicas del Handler.
    // Diseño: stopUpdating() -> Handler.removeCallbacks()
    // --------------------------------------------------------------
    private void stopUpdating() {
        if (updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }

    // --------------------------------------------------------------
    // isTracking()
    // Descripción: Indica si el seguimiento de tiempo está actualmente activo.
    // Diseño: isTracking() -> true/false
    // Retorno: true si está rastreando, false en caso contrario
    // --------------------------------------------------------------
    public boolean isTracking() {
        return isTracking;
    }

    // --------------------------------------------------------------
    // isPaused()
    // Descripción: Indica si el seguimiento está actualmente pausado.
    // Diseño: isPaused() -> true/false
    // Retorno: true si está pausado, false en caso contrario
    // --------------------------------------------------------------
    public boolean isPaused() {
        return isPaused;
    }

    // --------------------------------------------------------------
    // destroy()
    // Descripción: Libera los recursos del Handler y detiene todas las
    //              actualizaciones pendientes. Debe llamarse al destruir el tracker.
    // Diseño: destroy() -> stopUpdating() -> limpia Handler
    // --------------------------------------------------------------
    public void destroy() {
        stopUpdating();
        handler.removeCallbacksAndMessages(null);
    }

    // -----------------------------------------------------------------
    // Clase interna para componentes de tiempo
    // -----------------------------------------------------------------
    /**
     * @class TimeComponents
     * @brief Contenedor para los componentes individuales del tiempo transcurrido.
     *
     * Esta clase almacena las horas, minutos, segundos y el total de segundos
     * de forma estructurada para facilitar su uso en la interfaz.
     */
    public static class TimeComponents {
        public long hours;
        public long minutes;
        public long seconds;
        public long totalSeconds;

        // --------------------------------------------------------------
        // Constructor
        // Descripción: Inicializa los componentes de tiempo.
        // Parámetros: - h : horas
        //             - m : minutos
        //             - s : segundos
        //             - total : total de segundos
        // --------------------------------------------------------------
        public TimeComponents(long h, long m, long s, long total) {
            this.hours = h;
            this.minutes = m;
            this.seconds = s;
            this.totalSeconds = total;
        }
    }

    // --------------------------------------------------------------
    // calculatePaceMinPerKm()
    // Descripción: Calcula el ritmo promedio en minutos por kilómetro
    //              basándose en la distancia recorrida.
    // Diseño: distancia + tiempo -> calculatePaceMinPerKm() -> min/km
    // Parámetros: distanceMeters : distancia recorrida en metros
    // Retorno: ritmo en minutos por kilómetro (0 si distancia es 0)
    // --------------------------------------------------------------
    public double calculatePaceMinPerKm(double distanceMeters) {
        if (distanceMeters == 0) return 0;

        long totalMinutes = getElapsedTimeMinutes();
        double distanceKm = distanceMeters / 1000.0;

        return totalMinutes / distanceKm;
    }

    // --------------------------------------------------------------
    // calculateSpeedKmPerHour()
    // Descripción: Calcula la velocidad promedio en kilómetros por hora
    //              basándose en la distancia recorrida.
    // Diseño: distancia + tiempo -> calculateSpeedKmPerHour() -> km/h
    // Parámetros: distanceMeters : distancia recorrida en metros
    // Retorno: velocidad en km/h (0 si tiempo es 0)
    // --------------------------------------------------------------
    public double calculateSpeedKmPerHour(double distanceMeters) {
        long totalSeconds = getElapsedTimeSeconds();
        if (totalSeconds == 0) return 0;

        double distanceKm = distanceMeters / 1000.0;
        double hours = totalSeconds / 3600.0;

        return distanceKm / hours;
    }
}