package com.example.air_o_walk_sprint0;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;

/**
 * María Algora
 * Clase que encapsula la lógica de negocio para el registro de usuarios.
 * Recibe _ y realiza la petición HTTP _.
 */
public class LogicaRegistro {

    private String firstName;
    private String lastName;
    private String email;
    private String dni;
    private String phone;
    private String townHallName;

    // Constructor para inicializar los datos del usuario
    public LogicaRegistro(String firstName, String lastName, String email, String dni, String phone, String townHallName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.dni = dni;
        this.phone = phone;
        this.townHallName = townHallName;
    }

    // Método para obtener la lista de ayuntamientos desde el servidor
    public void obtenerListaAyuntamientos(final AyuntamientosCallback callback) {
        PeticionarioREST elPeticionario = new PeticionarioREST();
        String url = "http://api.sagucre.upv.edu.es/getAyuntamientos";

        elPeticionario.hacerPeticionREST("GET", url, null, new PeticionarioREST.RespuestaREST() {
            @Override
            public void callback(int codigo, String cuerpoRes) {
                if (codigo == 200) {
                    HashMap<String, String> ayuntamientosMap = parsearAyuntamientos(cuerpoRes);
                    callback.onAyuntamientosObtenidos(ayuntamientosMap);
                } else {
                    callback.onError("Error obteniendo los ayuntamientos");
                }
            }
        });
    }

    // Método para parsear la respuesta JSON de los ayuntamientos
    private HashMap<String, String> parsearAyuntamientos(String jsonResponse) {
        HashMap<String, String> ayuntamientosMap = new HashMap<>();

        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject ayuntamiento = jsonArray.getJSONObject(i);
                String id = ayuntamiento.getString("id");
                String nombre = ayuntamiento.getString("name");
                ayuntamientosMap.put(nombre, id); // Guardamos en el HashMap: nombre -> id
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ayuntamientosMap;
    }

    // Método para obtener el ID del ayuntamiento basado en su nombre
    public String obtenerIdAyuntamiento(String townHallName, HashMap<String, String> ayuntamientosMap) {
        return ayuntamientosMap.get(townHallName); // Retorna el ID usando el nombre como clave
    }

    // Método para realizar el registro del usuario (POST)
    public void realizarRegistroUsuario(String townHallId, final RegistroCallback callback) {
        PeticionarioREST elPeticionario = new PeticionarioREST();

        // Crear el cuerpo de la petición POST
        String cuerpo = "{"
                + "\"firstName\": \"" + this.firstName + "\", "
                + "\"lastName\": \"" + this.lastName + "\", "
                + "\"email\": \"" + this.email + "\", "
                + "\"dni\": \"" + this.dni + "\", "
                + "\"phone\": \"" + this.phone + "\", "
                + "\"townHallId\": \"" + townHallId + "\""
                + "}";

        elPeticionario.hacerPeticionREST("POST", "http://api.sagucre.upv.edu.es/registro", cuerpo, new PeticionarioREST.RespuestaREST() {
            @Override
            public void callback(int codigo, String cuerpoRes) {
                if (codigo == 200) {
                    callback.onRegistroExitoso("Usuario registrado correctamente, comprueba tu correo");
                } else {
                    callback.onRegistroFallido("Error al registrar el usuario");
                }
            }
        });
    }

    // Interfaces de callback para la respuesta del servidor
    public interface RegistroCallback {
        void onRegistroExitoso(String mensaje);
        void onRegistroFallido(String mensajeError);
    }

    public interface AyuntamientosCallback {
        void onAyuntamientosObtenidos(HashMap<String, String> ayuntamientosMap);
        void onError(String mensajeError);
    }
}
