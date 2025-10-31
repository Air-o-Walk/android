package com.example.air_o_walk_sprint0;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;

/**
 * Clase encargada de mostrar notificaciones locales
 * cuando se detecta un valor alto (>= 670) proveniente del sensor.
 * El valor es solo de prueba a futuro se utilizara un umbral de valores para
 * cada tipo de medicion.
 *
 * No requiere modificar otras partes del proyecto.
 * Solo hay que llamarla desde cualquier sitio con:
 *     AdminNotificaciones.revisarYNotificar(context, valorMedicion);
 */
public class AdminNotificaciones {

    // ID del canal de notificaciones (requerido desde Android 8+)
    private static final String CANAL_ID = "canal_alertas_airowalk";

    // ID único para la notificación (sirve para actualizarla o reemplazarla)
    private static final int NOTIFICACION_ID = 1001;

    /**
     * Revisa si el valor supera el umbral fijo (670)
     * y, si es así, muestra una notificación en pantalla.
     * Incluye comprobación de permiso POST_NOTIFICATIONS para Android 13+.
     *
     * @param context        Contexto de la app (por ejemplo: "this" desde MainActivity)
     * @param valorMedicion  Valor numérico recibido del sensor
     */
    public static void revisarYNotificar(Context context, int valorMedicion) {

        // 1 Umbral fijo de prueba (no se calcula dinámicamente por ahora)
        final int UMBRAL_ALERTA = 670;

        // 2 Si el valor del sensor es igual o superior al umbral, mostramos notificación
        if (valorMedicion >= UMBRAL_ALERTA) {

            // Android 13+ (API 33): hay que comprobar el permiso POST_NOTIFICATIONS
            if (Build.VERSION.SDK_INT >= 33) {
                int permiso = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                );
                if (permiso != PackageManager.PERMISSION_GRANTED) {
                    // Si el permiso no está concedido, salimos sin notificar
                    return;
                }
            }

            // 3 Creamos el canal si no existe (obligatorio desde Android 8)
            crearCanal(context);

            // 4 Construimos la notificación
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CANAL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle("⚠️ Alerta de calidad del aire (CO2)")
                    .setContentText("¡Valor alto detectado en tu zona: " + valorMedicion + "!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true); // Se borra al pulsar la notificación

            // 5️ Enviamos la notificación al sistema
            NotificationManagerCompat.from(context).notify(NOTIFICACION_ID, builder.build());
        }
    }

    /**
     * Crea el canal de notificaciones (solo Android 8 o superior).
     * Los canales permiten que el usuario gestione las preferencias
     * de sonido, vibración, etc., de este tipo de alertas.
     */
    private static void crearCanal(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Nombre y descripción visibles en los ajustes del sistema
            CharSequence nombre = "Alertas de sensor Air-o-Walk";
            String descripcion = "Notificaciones cuando el valor del sensor supera el límite permitido";

            // Nivel de importancia (HIGH = se muestra inmediatamente con sonido/vibración)
            int importancia = NotificationManager.IMPORTANCE_HIGH;

            // Se crea el canal con los parámetros definidos
            NotificationChannel canal = new NotificationChannel(CANAL_ID, nombre, importancia);
            canal.setDescription(descripcion);

            // Se registra el canal en el sistema (solo la primera vez)
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(canal);
        }
    }
}
