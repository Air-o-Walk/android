package com.example.air_o_walk_sprint0;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * @class NotifEstadoNodo
 * @brief Monitoriza el estado del nodo sensor (beacon) y gestiona notificaciones.
 *
 * CORRECCIONES IMPLEMENTADAS:
 * 1. Notificaciones persistentes que permanecen tras cerrar la app
 * 2. Mensajes claros y comprensibles para el usuario
 * 3. Detección correcta de desconexión (fuera de rango o apagado)
 * 4. Sistema de IDs únicos para cada tipo de notificación
 * 5. PendingIntent para abrir la app al tocar la notificación
 *
 * @author Christopher, Adenor y equipo Air-o-Walk
 * @version 2.0
 */
public class NotifEstadoNodo {

    private static final String TAG = "NotifEstadoNodo";

    // ------------------------------------------------------------
    // Constantes de funcionamiento
    // ------------------------------------------------------------
    private static final long TIMEOUT_BEACON_MS = 15000;   // 15 segundos sin beacons → desconectado
    private static final long INTERVALO_CHECK_MS = 5000;   // revisar cada 5 segundos (más frecuente)

    // Umbrales de mediciones
    private static final float GAS_MIN = 0f;
    private static final float GAS_MAX = 500f;
    private static final float TEMP_MIN = -10f;
    private static final float TEMP_MAX = 62f;

    // ------------------------------------------------------------
    // IDs de notificación únicos y persistentes
    // ------------------------------------------------------------
    private static final int NOTIF_ID_CONECTADO = 3001;
    private static final int NOTIF_ID_DESCONECTADO = 3002;
    private static final int NOTIF_ID_INCOHERENTE = 3003;
    private static final int NOTIF_ID_FUERA_RANGO = 3004;

    // ------------------------------------------------------------
    // Canal de notificaciones
    // ------------------------------------------------------------
    private static final String CANAL_NODO = "canal_estado_sensor";

    // ------------------------------------------------------------
    // Estado interno
    // ------------------------------------------------------------
    private final Context context;
    private final String nombreNodo;

    private long ultimoBeacon = 0;
    private boolean estabaConectado = false;
    private boolean yaNotificoDesconexion = false;

    private final Handler handler;
    private boolean monitorCorriendo = false;

    // Listener para desconexión
    private DesconexionListener desconexionListener;

    // ------------------------------------------------------------
    // Interface para notificar desconexión a MainActivity
    // ------------------------------------------------------------
    public interface DesconexionListener {
        void onNodoDesconectado();
    }

    // ------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------
    public NotifEstadoNodo(Context ctx, String nombreNodo) {
        this.context = ctx.getApplicationContext(); // Usar ApplicationContext
        this.nombreNodo = nombreNodo;
        this.handler = new Handler(Looper.getMainLooper());
        crearCanal();
        Log.d(TAG, "NotifEstadoNodo creado para: " + nombreNodo);
    }

    // ------------------------------------------------------------
    // Configurar listener de desconexión
    // ------------------------------------------------------------
    public void setDesconexionListener(DesconexionListener listener) {
        this.desconexionListener = listener;
    }

    // ------------------------------------------------------------
    // onBeaconRecibido() → llamado desde MainActivity al recibir datos
    // ------------------------------------------------------------
    public void onBeaconRecibido(float gas, float temperatura) {
        Log.d(TAG, "Beacon recibido - Gas: " + gas + ", Temp: " + temperatura);

        ultimoBeacon = System.currentTimeMillis();

        // Si antes estaba desconectado → ahora está conectado
        if (!estabaConectado) {
            Log.d(TAG, "Estado cambió a CONECTADO");
            notificarNodoConectado();
            estabaConectado = true;
            yaNotificoDesconexion = false;

            // Cancelar notificación de desconexión si existe
            cancelarNotificacion(NOTIF_ID_DESCONECTADO);
            cancelarNotificacion(NOTIF_ID_FUERA_RANGO);
        }

        // Verificar valores incoherentes
        verificarValoresIncoherentes(gas, temperatura);
    }

    // ------------------------------------------------------------
    // iniciarMonitor() → vigila periódicamente si se perdieron beacons
    // ------------------------------------------------------------
    public void iniciarMonitor() {
        if (monitorCorriendo) {
            Log.d(TAG, "Monitor ya está corriendo");
            return;
        }

        Log.d(TAG, "Iniciando monitor de estado");
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
        Log.d(TAG, "Deteniendo monitor de estado");
        monitorCorriendo = false;
        handler.removeCallbacksAndMessages(null);
    }

    // ------------------------------------------------------------
    // checkConexion() → detecta pérdida de conexión
    // ------------------------------------------------------------
    private void checkConexion() {
        long ahora = System.currentTimeMillis();
        long tiempoSinBeacon = ahora - ultimoBeacon;

        if (estabaConectado && tiempoSinBeacon > TIMEOUT_BEACON_MS) {
            Log.w(TAG, "DESCONEXIÓN DETECTADA - Tiempo sin beacon: " + tiempoSinBeacon + "ms");

            if (!yaNotificoDesconexion) {
                notificarNodoDesconectado();
                estabaConectado = false;
                yaNotificoDesconexion = true;

                // Notificar a MainActivity
                if (desconexionListener != null) {
                    desconexionListener.onNodoDesconectado();
                }
            }
        }
    }

    // ------------------------------------------------------------
    // notificarFueraDeRango() → llamado desde FindMyNodeActivity
    // ------------------------------------------------------------
    public void notificarFueraDeRango() {
        Log.d(TAG, "Notificando: sensor fuera de rango");

        if (!verificarPermisoNotificaciones()) {
            Log.w(TAG, "Sin permiso para notificaciones");
            return;
        }

        // Crear intent para abrir la app
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CANAL_NODO)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("Sensor fuera de alcance")
                .setContentText("Tu sensor está desconectado. Acércate para reconectar.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("No se puede detectar tu sensor. Asegúrate de que esté encendido y cerca de ti."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false) // NO se borra al tocar
                .setOngoing(false)    // Puede deslizarse para quitar
                .setContentIntent(pendingIntent);

        mostrarNotificacion(NOTIF_ID_FUERA_RANGO, builder.build());
    }

    // ------------------------------------------------------------
    // verificarValoresIncoherentes()
    // ------------------------------------------------------------
    private void verificarValoresIncoherentes(float gas, float temp) {
        boolean gasMalo = (gas < GAS_MIN || gas > GAS_MAX);
        boolean tempMala = (temp < TEMP_MIN || temp > TEMP_MAX);

        if (gasMalo || tempMala) {
            Log.w(TAG, "Valores incoherentes detectados - Gas: " + gas + ", Temp: " + temp);
            notificarValoresIncoherentes(gas, temp);
        }
    }

    // ------------------------------------------------------------
    // NOTIFICACIÓN: Sensor Conectado
    // ------------------------------------------------------------
    private void notificarNodoConectado() {
        Log.d(TAG, "Mostrando notificación de CONEXIÓN");

        if (!verificarPermisoNotificaciones()) {
            Log.w(TAG, "Sin permiso para notificaciones");
            return;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CANAL_NODO)
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setContentTitle("✓ Sensor conectado")
                .setContentText("Tu sensor está midiendo la calidad del aire.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Conexión establecida correctamente. Ya puedes iniciar tu recorrido."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)  // Se borra al tocar
                .setContentIntent(pendingIntent);

        mostrarNotificacion(NOTIF_ID_CONECTADO, builder.build());
    }

    // ------------------------------------------------------------
    // NOTIFICACIÓN: Sensor Desconectado
    // ------------------------------------------------------------
    private void notificarNodoDesconectado() {
        Log.d(TAG, "Mostrando notificación de DESCONEXIÓN");

        if (!verificarPermisoNotificaciones()) {
            Log.w(TAG, "Sin permiso para notificaciones");
            return;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CANAL_NODO)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("⚠ Sensor desconectado")
                .setContentText("Se perdió la conexión con tu sensor.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Tu sensor se ha desconectado. Verifica que esté encendido y cerca de ti."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false) // NO se borra automáticamente
                .setOngoing(false)    // Puede deslizarse
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500, 200, 500}); // Vibración

        mostrarNotificacion(NOTIF_ID_DESCONECTADO, builder.build());
    }

    // ------------------------------------------------------------
    // NOTIFICACIÓN: Mediciones Incoherentes
    // ------------------------------------------------------------
    private void notificarValoresIncoherentes(float gas, float temp) {
        if (!verificarPermisoNotificaciones()) {
            Log.w(TAG, "Sin permiso para notificaciones");
            return;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CANAL_NODO)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("⚠ Problema con las mediciones")
                .setContentText("Los datos del sensor parecen incorrectos.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Las mediciones están fuera del rango normal. Reinicia el sensor si el problema persiste."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        mostrarNotificacion(NOTIF_ID_INCOHERENTE, builder.build());
    }

    // ------------------------------------------------------------
    // Mostrar notificación (método centralizado)
    // ------------------------------------------------------------
    private void mostrarNotificacion(int notificationId, android.app.Notification notification) {
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(notificationId, notification);
            Log.d(TAG, "Notificación mostrada con ID: " + notificationId);
        } catch (SecurityException e) {
            Log.e(TAG, "Error de permisos al mostrar notificación", e);
        } catch (Exception e) {
            Log.e(TAG, "Error mostrando notificación", e);
        }
    }

    // ------------------------------------------------------------
    // Cancelar notificación específica
    // ------------------------------------------------------------
    private void cancelarNotificacion(int notificationId) {
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.cancel(notificationId);
            Log.d(TAG, "Notificación cancelada con ID: " + notificationId);
        } catch (Exception e) {
            Log.e(TAG, "Error cancelando notificación", e);
        }
    }

    // ------------------------------------------------------------
    // Verificar permisos de notificaciones
    // ------------------------------------------------------------
    private boolean verificarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean tienePermiso = ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;

            if (!tienePermiso) {
                Log.w(TAG, "Permiso POST_NOTIFICATIONS no concedido");
            }
            return tienePermiso;
        }
        return true; // Antes de Android 13 no se necesita permiso
    }

    // ------------------------------------------------------------
    // Crear canal de notificaciones (Android 8.0+)
    // ------------------------------------------------------------
    private void crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence nombre = "Estado del Sensor";
            String descripcion = "Notificaciones sobre la conexión y estado de tu sensor de calidad del aire";
            int importancia = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel canal = new NotificationChannel(CANAL_NODO, nombre, importancia);
            canal.setDescription(descripcion);
            canal.enableVibration(true);
            canal.setVibrationPattern(new long[]{0, 500, 200, 500});
            canal.setShowBadge(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(canal);
                Log.d(TAG, "Canal de notificaciones creado: " + CANAL_NODO);
            }
        }
    }
}