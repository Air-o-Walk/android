package com.example.air_o_walk_sprint0;

/**
 * Modelo de notificación de incidencia
 */
public class NotificationIncidencia {

    public int id;
    public String title;
    public String status;
    public String resolution;

    public NotificationIncidencia(
            int id,
            String title,
            String status,
            String resolution
    ) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.resolution = resolution;
    }
}
