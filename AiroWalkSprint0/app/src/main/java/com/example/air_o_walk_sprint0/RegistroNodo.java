package com.example.air_o_walk_sprint0;

import android.util.Log;

// -----------------------------------------------------------------------------
// RegistroNodo.java
// Autor : Meryame Ait Boumlik
// Descripción: Registra en el backend el nodo (beacon) vinculado por un usuario.
// Envía una petición POST con el userId y el nombre del beacon al endpoint REST.
// Diseño: -> new RegistroNodo(userId, nombreNodo) -> registrarNodo() -> PeticionarioREST
// -----------------------------------------------------------------------------
public class RegistroNodo {

    private String userId;     // ID del usuario que vincula el nodo
    private String nombreNodo; // Nombre del beacon (ej: "GTI")

    // --------------------------------------------------------------
    // Constructor
    // Descripción: Inicializa los datos del nodo que se va a registrar.
    // Parámetros:
    //   - userId: ID del usuario actual (sesión o configuración)
    //   - nombreNodo: nombre del beacon vinculado
    // --------------------------------------------------------------

    public RegistroNodo(String userId, String nombreNodo) {
        this.userId = userId;
        this.nombreNodo = nombreNodo;
    }

    // --------------------------------------------------------------
    // registrarNodo()
    // Descripción: Envía al backend una petición POST con los datos del nodo.
    // Diseño: registrarNodo() -> POST JSON (userId, nombreNodo) al backend
    // --------------------------------------------------------------
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

    // --------------------------------------------------------------
    // construirCuerpo()
    // Descripción: Construye el cuerpo JSON con los datos del usuario y el nodo.
    // Diseno: construirCuerpo()-> String con formato JSON.
    // --------------------------------------------------------------
    private String construirCuerpo() {
        // formato JSON esperado por el backend
        return "{"
                + "\"userId\": \"" + userId + "\", "
                + "\"nodeName\": \"" + nombreNodo + "\""
                + "}";
    }
    
}
