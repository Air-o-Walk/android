package com.example.air_o_walk_sprint0;

import android.util.Log;

/**
 * @class RegistroNodo
 * @brief Registra en el backend un nodo BLE (beacon) vinculado a un usuario.
 *
 * Esta clase se encarga de enviar una petición REST de tipo POST al backend
 * para asociar un nodo (beacon) a un usuario concreto. La petición incluye
 * el identificador del usuario y el nombre del nodo.
 *
 * Diseño general:
 * new RegistroNodo(userId, nombreNodo) → registrarNodo() → PeticionarioREST
 *
 * @author Meryame Ait Boumlik
 * @version 1.0
 */
public class RegistroNodo {

    private String userId;     // ID del usuario que vincula el nodo
    private String nombreNodo; // Nombre del beacon (ej: "GTI")

    /**
     * Constructor.
     *
     * Descripción: Inicializa los datos del nodo que se va a registrar.
     *
     * @param userId ID del usuario actual (sesión o configuración)
     * @param nombreNodo Nombre del beacon vinculado
     */

    public RegistroNodo(String userId, String nombreNodo) {
        this.userId = userId;
        this.nombreNodo = nombreNodo;
    }

    /**
     * registrarNodo()
     *
     * Descripción: Envía al backend una petición POST con los datos del nodo.
     *
     * Diseño:
     * registrarNodo() → POST JSON (userId, nombreNodo) → backend
     */
    public void registrarNodo() {
        PeticionarioREST peticion = new PeticionarioREST();
        String cuerpo = construirCuerpo();

        peticion.hacerPeticionREST(
                "POST",
                "http://api.sagucre.upv.edu.es/node/link",  // <-- endpoint del backend
                cuerpo,
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String respuesta) {
                        Log.d("RegistroNodo", "Respuesta del servidor:");
                        Log.d("RegistroNodo", "Código: " + codigo);
                        Log.d("RegistroNodo", "Cuerpo: " + respuesta);
                    }
                }
        );
    }

    /**
     * construirCuerpo()
     *
     * Descripción: Construye el cuerpo JSON con los datos del usuario y el nodo.
     *
     * Diseño:
     * construirCuerpo() → String con formato JSON
     *
     * @return String JSON con userId y nodeName
     */
    private String construirCuerpo() {
        // formato JSON esperado por el backend
        return "{"
                + "\"userId\": \"" + userId + "\", "
                + "\"nodeName\": \"" + nombreNodo + "\""
                + "}";
    }
    
}
