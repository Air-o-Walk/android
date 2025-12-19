package com.example.air_o_walk_sprint0;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;

/**
 * @class LogicaRegistro
 * @brief Encapsula la lógica de negocio para el registro de nuevos usuarios.
 *
 * Esta clase gestiona el proceso de alta de usuarios en la aplicación,
 * incluyendo:
 * - Obtención de la lista de ayuntamientos desde el backend
 * - Selección y mapeo del ayuntamiento correspondiente
 * - Envío de la solicitud de registro de usuario al servidor
 *
 * La comunicación con el backend se realiza mediante peticiones REST
 * y los resultados se devuelven de forma asíncrona a través de
 * interfaces de callback.
 *
 * @author María Algora
 * @version 1.0
 */

public class LogicaRegistro {

    // Método para obtener la lista de ayuntamientos desde el servidor
    public void obtenerListaAyuntamientos(final AyuntamientosCallback callback) {
        PeticionarioREST elPeticionario = new PeticionarioREST();
        String url = "http://api.sagucre.upv.edu.es/getAyuntamientos";

        elPeticionario.hacerPeticionREST("GET", url, null, new PeticionarioREST.RespuestaREST() {
            @Override
            public void callback(int codigo, String cuerpoRes) {
                Log.d("LogicaRegistro", "Código respuesta: " + codigo);
                Log.d("LogicaRegistro", "Respuesta: " + cuerpoRes);

                if (codigo == 200) {
                    try {
                        // La respuesta exitosa es directamente un array JSON
                        JSONArray jsonArray = new JSONArray(cuerpoRes);
                        HashMap<String, String> ayuntamientosMap = parsearAyuntamientos(jsonArray);
                        callback.onAyuntamientosObtenidos(ayuntamientosMap);

                    } catch (Exception e) {
                        // Si falla como array, intentamos como objeto (para errores)
                        try {
                            JSONObject jsonResponse = new JSONObject(cuerpoRes);
                            boolean success = jsonResponse.optBoolean("success", false);
                            if (!success) {
                                String errorMsg = jsonResponse.optString("message", "Error desconocido");
                                callback.onError("Error: " + errorMsg);
                            } else {
                                // Si tiene success:true pero no es un array, intentamos obtener data
                                JSONArray dataArray = jsonResponse.optJSONArray("data");
                                if (dataArray != null) {
                                    HashMap<String, String> ayuntamientosMap = parsearAyuntamientos(dataArray);
                                    callback.onAyuntamientosObtenidos(ayuntamientosMap);
                                } else {
                                    callback.onError("Formato de respuesta inválido");
                                }
                            }
                        } catch (Exception ex) {
                            callback.onError("Error procesando la respuesta: " + ex.getMessage());
                            ex.printStackTrace();
                        }
                    }
                } else if (codigo == 404) {
                    // Para respuestas 404, parseamos como objeto de error
                    try {
                        JSONObject jsonResponse = new JSONObject(cuerpoRes);
                        String errorMsg = jsonResponse.optString("message", "No se encontraron ayuntamientos");
                        callback.onError("Error: " + errorMsg);
                    } catch (Exception e) {
                        callback.onError("Error: No se encontraron ayuntamientos");
                    }
                } else {
                    callback.onError("Error obteniendo los ayuntamientos. Código: " + codigo);
                }
            }
        });
    }

    // Método sobrecargado para manejar tanto JSONArray como String
    private HashMap<String, String> parsearAyuntamientos(JSONArray jsonArray) {
        HashMap<String, String> ayuntamientosMap = new HashMap<>();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject ayuntamiento = jsonArray.getJSONObject(i);
                String id = ayuntamiento.getString("id");
                String nombre = ayuntamiento.getString("name");
                ayuntamientosMap.put(nombre, id);
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
    public void solicitudUsuario(String townHallId, String firstName, String lastName, String email,
                                 String dni, String phone, RegistroCallback callback) {
        PeticionarioREST elPeticionario = new PeticionarioREST();

        // Crear el cuerpo de la petición POST
        String cuerpo = "{"
                + "\"firstName\": \"" + firstName + "\", "
                + "\"lastName\": \"" + lastName + "\", "
                + "\"email\": \"" + email + "\", "
                + "\"dni\": \"" + dni + "\", "
                + "\"phone\": \"" + phone + "\", "
                + "\"townHallId\": \"" + townHallId + "\""
                + "}";

        elPeticionario.hacerPeticionREST("POST", "http://api.sagucre.upv.edu.es/apply", cuerpo, new PeticionarioREST.RespuestaREST() {
            @Override
            public void callback(int codigo, String cuerpoRes) {
                if (codigo == 200) {
                    callback.onRegistroExitoso("Usuario registrado correctamente, comprueba tu correo");
                } else {
                    callback.onRegistroFallido("Error al registrar el usuario"+codigo);
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
