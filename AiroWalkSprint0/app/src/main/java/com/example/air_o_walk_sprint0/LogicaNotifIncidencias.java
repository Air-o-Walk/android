package com.example.air_o_walk_sprint0;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Lógica para obtener notificaciones de incidencias
 */
public class LogicaNotifIncidencias {

    public interface NotificacionesCallback {
        void onNotificacionesRecibidas(List<NotificationIncidencia> lista);
        void onError(String mensaje);
    }

    private int userId;
    private NotificacionesCallback callback;

    public LogicaNotifIncidencias(int userId, NotificacionesCallback callback) {
        this.userId = userId;
        this.callback = callback;
    }

    /**
     * GET /notifications/{userId}
     */
    public void obtenerNotificaciones() {

        String url =
                "http://api.sagucre.upv.edu.es/notifications/" + userId;

        PeticionarioREST peticion = new PeticionarioREST();
        peticion.hacerPeticionREST(
                "GET",
                url,
                null,
                (codigo, respuesta) -> {

                    Log.d("LogicaNotifIncidencias", "Código: " + codigo);
                    Log.d("LogicaNotifIncidencias", "Respuesta: " + respuesta);

                    if (codigo == 200 && respuesta != null && !respuesta.isEmpty()) {
                        procesarRespuesta(respuesta);
                    } else {
                        if (callback != null) {
                            callback.onError("Error al obtener notificaciones");
                        }
                    }
                }
        );
    }

    private void procesarRespuesta(String respuesta) {
        try {
            List<NotificationIncidencia> lista = new ArrayList<>();

            // 1️⃣ Parse ROOT object
            JSONObject root = new JSONObject(respuesta);

            // 2️⃣ Extract array
            JSONArray array = root.getJSONArray("notifications");

            // 3️⃣ Parse notifications
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                NotificationIncidencia notif = new NotificationIncidencia(
                        obj.getInt("id"),
                        obj.getString("title"),
                        obj.getString("status"),
                        obj.optString("resolution", "")
                );

                lista.add(notif);
            }

            if (callback != null) {
                callback.onNotificacionesRecibidas(lista);
            }

        } catch (Exception e) {
            Log.e("LogicaNotifIncidencias", "Error parseando JSON", e);
            if (callback != null) {
                callback.onError("Error procesando notificaciones");
            }
        }
    }
}
