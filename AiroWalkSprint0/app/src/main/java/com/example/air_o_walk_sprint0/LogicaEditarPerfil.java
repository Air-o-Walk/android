package com.example.air_o_walk_sprint0;

import android.util.Log;
import org.json.JSONObject;

/**
 * Clase que encapsula la lógica de negocio para editar el perfil de usuario.
 * Permite actualizar username, email y password.
 */
public class LogicaEditarPerfil {

    private String token;
    private int userId;

    /**
     * Constructor de la clase LogicaEditarPerfil.
     * @param token Token de autenticación del usuario
     * @param userId ID del usuario a editar
     */
    public LogicaEditarPerfil(String token, int userId) {
        this.token = token;
        this.userId = userId;
    }

    /**
     * Actualiza el username del usuario.
     * @param nuevoUsername Nuevo nombre de usuario
     * @param callback Callback para manejar la respuesta
     */
    public void actualizarUsername(String nuevoUsername, EditarCallback callback) {
        actualizarCampo("username", nuevoUsername, callback);
    }

    /**
     * Actualiza el email del usuario.
     * @param nuevoEmail Nuevo email del usuario
     * @param callback Callback para manejar la respuesta
     */
    public void actualizarEmail(String nuevoEmail, EditarCallback callback) {
        actualizarCampo("email", nuevoEmail, callback);
    }

    /**
     * Actualiza la contraseña del usuario verificando primero la contraseña actual.
     * @param passwordActual Contraseña actual para verificación
     * @param nuevaPassword Nueva contraseña
     * @param callback Callback para manejar la respuesta
     */
    public void actualizarPassword(String passwordActual, String nuevaPassword, EditarCallback callback) {
        PeticionarioREST elPeticionario = new PeticionarioREST();

        String cuerpo = construirCuerpoPassword(passwordActual, nuevaPassword);
        String url = "http://api.sagucre.upv.edu.es/users/" + userId;

        elPeticionario.hacerPeticionREST("PUT", url, cuerpo,
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String cuerpoRes) {
                        Log.d("ACTUALIZAR_PASSWORD_RESPUESTA", "Código: " + codigo + ", Cuerpo: " + cuerpoRes);
                        procesarRespuestaEdicion(codigo, cuerpoRes, "password", callback);
                    }
                }
        );
    }

    /**
     * Construye el cuerpo JSON para la actualización de contraseña.
     * @param passwordActual Contraseña actual
     * @param nuevaPassword Nueva contraseña
     * @return String con el cuerpo JSON
     */
    private String construirCuerpoPassword(String passwordActual, String nuevaPassword) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("current_password", passwordActual);
            jsonBody.put("new_password", nuevaPassword);
            return jsonBody.toString();
        } catch (Exception e) {
            Log.e("PASSWORD_ERROR", "Error construyendo JSON: " + e.getMessage());
            return "{}";
        }
    }

    /**
     * Método genérico para actualizar cualquier campo del perfil.
     * @param campo Nombre del campo a actualizar
     * @param valor Nuevo valor del campo
     * @param callback Callback para manejar la respuesta
     */
    private void actualizarCampo(String campo, String valor, EditarCallback callback) {
        PeticionarioREST elPeticionario = new PeticionarioREST();

        String cuerpo = construirCuerpo(campo, valor);
        String url = "http://api.sagucre.upv.edu.es/users/" + userId;

        elPeticionario.hacerPeticionREST("PUT", url, cuerpo,
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String cuerpoRes) {
                        Log.d("EDITAR_PERFIL_RESPUESTA", "Código: " + codigo + ", Cuerpo: " + cuerpoRes);
                        procesarRespuestaEdicion(codigo, cuerpoRes, campo, callback);
                    }
                }
        );
    }

    /**
     * Construye el cuerpo JSON para la petición de actualización.
     * @param campo Campo a actualizar
     * @param valor Nuevo valor del campo
     * @return String con el cuerpo JSON
     */
    private String construirCuerpo(String campo, String valor) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put(campo, valor);
            return jsonBody.toString();
        } catch (Exception e) {
            Log.e("EDITAR_PERFIL_ERROR", "Error construyendo JSON: " + e.getMessage());
            return "{\"" + campo + "\": \"" + valor + "\"}";
        }
    }

    /**
     * Procesa la respuesta del servidor para la edición del perfil.
     * @param codigo Código HTTP de respuesta
     * @param cuerpo Cuerpo de la respuesta JSON
     * @param campo Campo que se intentó actualizar
     * @param callback Callback para notificar el resultado
     */
    private void procesarRespuestaEdicion(int codigo, String cuerpo, String campo, EditarCallback callback) {
        try {
            if (codigo == 200) {
                // Actualización exitosa
                JSONObject respuestaJson = new JSONObject(cuerpo);

                if (respuestaJson.has("success") && respuestaJson.getBoolean("success")) {
                    callback.onEdicionExitosa(campo, "Campo actualizado correctamente");
                } else {
                    String mensajeError = respuestaJson.optString("message", "Error al actualizar " + campo);
                    callback.onEdicionFallida(campo, mensajeError);
                }
            } else if (codigo == 400) {
                callback.onEdicionFallida(campo, "Solicitud incorrecta");
            } else if (codigo == 401) {
                callback.onEdicionFallida(campo, "No autorizado - token inválido");
            } else if (codigo == 404) {
                callback.onEdicionFallida(campo, "Usuario no encontrado");
            } else if (codigo >= 500) {
                callback.onEdicionFallida(campo, "Error del servidor");
            } else {
                callback.onEdicionFallida(campo, "Error desconocido: " + codigo);
            }
        } catch (Exception e) {
            Log.e("EDITAR_PERFIL_ERROR", "Error procesando respuesta: " + e.getMessage());
            callback.onEdicionFallida(campo, "Error procesando la respuesta del servidor");
        }
    }

    /**
     * Interfaz callback para manejar los resultados de la edición del perfil.
     */
    public interface EditarCallback {
        void onEdicionExitosa(String campo, String mensaje);
        void onEdicionFallida(String campo, String mensajeError);
    }
}