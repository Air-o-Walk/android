package com.example.air_o_walk_sprint0;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


/*
// ===================================================================================================
//  NotifEstadoNodo.java
//  Autor : Christopher y Adenor
//
//  Descripción:
//  -----------------------------------------------------------------------------------------------
//  Clase responsable de monitorizar el estado del nodo sensor (beacon) después de la vinculación.
//  Se encarga de:
//
//     1) Detectar si se están recibiendo beacons → "Nodo conectado"
//     2) Detectar si dejan de recibirse beacons durante X segundos → "Nodo desconectado"
//     3) Analizar las mediciones recibidas (gas, temperatura, etc.) y determinar si son incoherentes
//        usando un threshold.
//     4) NUEVO: Notificar mediante listener cuando se pierde la conexión
//
//  Todo está encapsulado aquí para no modificar las otras clases del proyecto.
// ===================================================================================================
*/

public class NotifEstadoNodo {

    // ------------------------------------------------------------
    // Constantes de funcionamiento
    // ------------------------------------------------------------
    private static final long TIMEOUT_BEACON_MS = 15000;   // 15 segundos sin beacons → desconectado
    private static final long INTERVALO_CHECK_MS = 20000;  // revisar cada 20 segundo

    private static final float GAS_MIN = 0f;
    private static final float GAS_MAX = 500f;
    private static final float TEMP_MIN = -10f;
    private static final float TEMP_MAX = 62f;

    // ------------------------------------------------------------
    // Estado interno
    // ------------------------------------------------------------
    private final Context context;
    private final String nombreNodo;

    private long ultimoBeacon = 0;
    private boolean estabaConectado = false;

    private final Handler handler = new Handler();
    private boolean monitorCorriendo = false;

    // Canal de notificaciones
    private static final String CANAL_NODO = "canal_estado_nodo";

    // NUEVO: Listener para desconexión
    private DesconexionListener desconexionListener;

    // ------------------------------------------------------------
    // NUEVO: Interface para notificar desconexión
    // ------------------------------------------------------------
    public interface DesconexionListener {
        void onNodoDesconectado();
    }

    // ------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------
    public NotifEstadoNodo(Context ctx, String nombreNodo) {
        this.context = ctx;
        this.nombreNodo = nombreNodo;
        crearCanal();
    }

    // ------------------------------------------------------------
    // NUEVO: Método para configurar listener de desconexión
    // ------------------------------------------------------------
    public void setDesconexionListener(DesconexionListener listener) {
        this.desconexionListener = listener;
    }

    // ------------------------------------------------------------
    // onBeaconRecibido() → llamado desde MainActivity al recibir datos válidos
    // ------------------------------------------------------------
    public void onBeaconRecibido(float gas, float temperatura) {

        ultimoBeacon = System.currentTimeMillis();

        // Si antes estaba desconectado → ahora está conectado
        if (!estabaConectado) {
            notificarNodoConectado();
            estabaConectado = true;
        }

        // Verificar valores incoherentes
        verificarValoresIncoherentes(gas, temperatura);
    }

    // ------------------------------------------------------------
    // iniciarMonitor() → vigila cada 1s si se perdieron beacons
    // ------------------------------------------------------------
    public void iniciarMonitor() {
        if (monitorCorriendo) return;

        monitorCorriendo = true;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkConexion();
                if (monitorCorriendo) {
                    handler.postDelayed(this, INTERVALO_CHECK_MS);
                }
            }
        }, INTERVALO_CHECK_MS);
    }

    // ------------------------------------------------------------
    // detenerMonitor()
    // ------------------------------------------------------------
    public void detenerMonitor() {
        monitorCorriendo = false;
        handler.removeCallbacksAndMessages(null);
    }

    // ------------------------------------------------------------
    // checkConexion() → si pasan X segundos sin beacons → desconectado
    // MODIFICADO: Ahora notifica mediante listener
    // ------------------------------------------------------------
    private void checkConexion() {

        long ahora = System.currentTimeMillis();

        if (estabaConectado && (ahora - ultimoBeacon) > TIMEOUT_BEACON_MS) {
            notificarNodoDesconectado();
            estabaConectado = false;

            // NUEVO: Notificar a MainActivity mediante listener
            if (desconexionListener != null) {
                desconexionListener.onNodoDesconectado();
            }
        }
    }

    // ------------------------------------------------------------
    // verificarValoresIncoherentes()
    // ------------------------------------------------------------
    private void verificarValoresIncoherentes(float gas, float temp) {

        boolean gasMalo = (gas < GAS_MIN || gas > GAS_MAX);
        boolean tempMala = (temp < TEMP_MIN || temp > TEMP_MAX);

        if (gasMalo || tempMala) {
            notificarValoresIncoherentes(gas, temp);
        }
    }

    // ------------------------------------------------------------
    // Notificación: Nodo Conectado
    // ------------------------------------------------------------
    private void notificarNodoConectado() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CANAL_NODO)
                        .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                        .setContentTitle("Nodo conectado")
                        .setContentText("Recibiendo datos de: " + nombreNodo)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        try {
            NotificationManagerCompat.from(context).notify(2001, builder.build());
        } catch (Exception e) {
            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(2001, builder.build());
        }
    }


    // ------------------------------------------------------------
    // Notificación: Nodo Desconectado
    // ------------------------------------------------------------
    private void notificarNodoDesconectado() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CANAL_NODO)
                        .setSmallIcon(android.R.drawable.stat_notify_error)
                        .setContentTitle("Nodo desconectado")
                        .setContentText("No se reciben beacons de: " + nombreNodo)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        try {
            NotificationManagerCompat.from(context).notify(2002, builder.build());
        } catch (Exception e) {
            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(2002, builder.build());
        }
    }


    // ------------------------------------------------------------
    // Notificación: Mediciones incoherentes
    // ------------------------------------------------------------
    private void notificarValoresIncoherentes(float gas, float temp) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CANAL_NODO)
                        .setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setContentTitle("⚠ Mediciones incoherentes")
                        .setContentText("Gas=" + gas + ", Temp=" + temp + " fuera de rango")
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        try {
            NotificationManagerCompat.from(context).notify(2003, builder.build());
        } catch (Exception e) {
            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(2003, builder.build());
        }
    }


    // ------------------------------------------------------------
    // Verificar permisos POST_NOTIFICATIONS (Android 13+)
    // ------------------------------------------------------------
    private boolean tienePermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Antes de Android 13 no hace falta permiso
    }

    // ------------------------------------------------------------
    // Crear canal de notificaciones
    // ------------------------------------------------------------
    private void crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            CharSequence nombre = "Estado del nodo";
            String descripcion = "Notificaciones sobre conexión y lecturas del nodo";
            int importancia = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel canal = new NotificationChannel(CANAL_NODO, nombre, importancia);
            canal.setDescription(descripcion);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(canal);
        }
    }
}