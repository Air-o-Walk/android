package com.example.air_o_walk_sprint0;

import android.util.Log;

/**
 * @class LogicaIncidencias
 * @brief Gestiona la creación de incidencias (quejas) en el backend.
 *
 * Esta clase se encarga de enviar una petición REST POST al backend
 * para registrar una incidencia asociada a un usuario.
 *
 * Diseño general:
 * new LogicaIncidencias(userId, tipo, descripcion)
 *      → enviarIncidencia()
 *      → PeticionarioREST
 *
 * @author Meryame Ait Boumlik
 * @version 1.0
 */
public class LogicaIncidencias {

    private int userId;
    private String tipo;
    private String descripcion;

    /**
     * Constructor.
     *
     * @param userId ID del usuario que crea la incidencia
     * @param tipo Tipo de incidencia (ej: "Sensor no responde")
     * @param descripcion Descripción detallada del problema
     */
    public LogicaIncidencias(int userId, String tipo, String descripcion) {
        this.userId = userId;
        this.tipo = tipo;
        this.descripcion = descripcion;
    }

    /**
     * enviarIncidencia()
     *
     * Descripción:
     * Envía una petición POST al backend para crear una nueva incidencia.
     *
     * Diseño:
     * enviarIncidencia()
     * → POST /problems
     * → JSON { userId, title, description }
     */
    public void enviarIncidencia() {

        PeticionarioREST peticion = new PeticionarioREST();
        String cuerpo = construirCuerpo();

        peticion.hacerPeticionREST(
                "POST",
                "http://api.sagucre.upv.edu.es/problems",
                cuerpo,
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String respuesta) {
                        Log.d("LogicaIncidencias", "Respuesta del servidor:");
                        Log.d("LogicaIncidencias", "Código: " + codigo);
                        Log.d("LogicaIncidencias", "Cuerpo: " + respuesta);
                    }
                }
        );
    }

    /**
     * construirCuerpo()
     *
     * @return String JSON con los datos de la incidencia
     */
    private String construirCuerpo() {
        return "{"
                + "\"userId\": " + userId + ", "
                + "\"title\": \"" + tipo + "\", "
                + "\"description\": \"" + descripcion + "\""
                + "}";
    }
}
