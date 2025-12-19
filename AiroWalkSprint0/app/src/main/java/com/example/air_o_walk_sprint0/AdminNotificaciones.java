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
 * @class AdminNotificaciones
 * @brief Gestiona el envío de notificaciones locales de alerta.
 *
 * Esta clase se encarga de mostrar notificaciones locales cuando se detecta
 * un valor elevado proveniente de un sensor. Actualmente se utiliza un
 * umbral fijo de prueba, aunque en futuras versiones se prevé el uso de
 * umbrales dinámicos específicos para cada tipo de medición.
 *
 *
 * Uso:
 * AdminNotificaciones.revisarYNotificar(context, valorMedicion);
 *
 * @author Christopher Yoris Pulgar
 * @version 1.0
 */
public class AdminNotificaciones {

    // ID del canal de notificaciones (requerido desde Android 8+)
    private static final String CANAL_ID = "canal_alertas_airowalk";

    // ID único para la notificación (sirve para actualizarla o reemplazarla)
    private static final int NOTIFICACION_ID = 1001;

    /**
     * Comprueba si el valor recibido supera el umbral definido y,
     * en caso afirmativo, muestra una notificación de alerta.
     *
     * En dispositivos con Android 13 o superior, se verifica previamente
     * que el permiso POST_NOTIFICATIONS haya sido concedido.
     *
     * @param context Contexto de la aplicación
     * @param valorMedicion Valor numérico recibido desde el sensor
     */
    public static void revisarYNotificar(Context context, float valorMedicion) {

        // 1 Umbral fijo de prueba (no se calcula dinámicamente por ahora)
        final int UMBRAL_ALERTA = 5;

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
                    .setContentTitle("⚠️ Alerta de calidad del aire (03)")
                    .setContentText("¡Valor alto detectado en tu zona: " + valorMedicion + "!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true); // Se borra al pulsar la notificación

            // 5️ Enviamos la notificación al sistema
            NotificationManagerCompat.from(context).notify(NOTIFICACION_ID, builder.build());
        }
    }

    /**
     * Crea el canal de notificaciones requerido por Android 8 o superior.
     *
     * Los canales permiten al usuario configurar las preferencias
     * de sonido, vibración y visibilidad de este tipo de alertas.
     *
     * @param context Contexto de la aplicación
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
