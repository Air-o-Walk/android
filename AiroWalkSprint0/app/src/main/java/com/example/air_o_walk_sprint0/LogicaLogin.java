package com.example.air_o_walk_sprint0;

import android.util.Log;

/**
 * Clase que encapsula la lógica de negocio para el login de usuarios.
 * Recibe usuario y contraseña y realiza la petición HTTP GET para validar.
 */
public class LogicaLogin {

    private String usuario;
    private String contrasena;

    /**
     * Constructor de la clase LogicaLogin.
     * @param usuario Nombre de usuario
     * @param contrasena Contraseña del usuario
     */
    public LogicaLogin(String usuario, String contrasena) {
        this.usuario = usuario;
        this.contrasena = contrasena;
    }

    /**
     * Realiza el login del usuario mediante una petición REST GET.
     * Utiliza PeticionarioREST para gestionar la petición asíncrona.
     * @param callback Callback para manejar la respuesta del login
     */
    public void realizarLogin(LoginCallback callback) {
        PeticionarioREST elPeticionario = new PeticionarioREST();

        // Construir la URL con los parámetros de usuario y contraseña
        String urlConParametros = construirURL();

        Log.d("LOGIN_DEBUG", "URL de login: " + urlConParametros);

        elPeticionario.hacerPeticionREST("GET", urlConParametros,
                null, // GET no necesita cuerpo
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String cuerpo) {
                        Log.d("LOGIN_RESPUESTA", "Código: " + codigo + ", Cuerpo: " + cuerpo);

                        // Procesar la respuesta
                        procesarRespuestaLogin(codigo, cuerpo, callback);
                    }
                }
        );
    }

    /**
     * Construye la URL completa con los parámetros de usuario y contraseña
     * @return URL completa para la petición GET
     */
    private String construirURL() {
        // URL base del servicio de login - ajusta según tu API
        String urlBase = "http://sagucre.upv.edu.es/api/login";

        // Codificar parámetros para URL (podrías usar URLEncoder para caracteres especiales)
        String parametros = "usuario=" + usuario + "&contrasena=" + contrasena;

        return urlBase + "?" + parametros;
    }

    /**
     * Procesa la respuesta del servidor de login
     * @param codigo Código HTTP de respuesta
     * @param cuerpo Cuerpo de la respuesta (JSON esperado)
     * @param callback Callback para notificar el resultado
     */
    private void procesarRespuestaLogin(int codigo, String cuerpo, LoginCallback callback) {
        try {
            if (codigo == 200) {
                // Login exitoso - procesar la respuesta JSON
                // Asumiendo que el servidor retorna un JSON como:
                // {"success": true, "message": "Login exitoso", "token": "abc123..."}
                // o {"success": false, "message": "Credenciales incorrectas"}

                if (cuerpo != null && cuerpo.contains("\"success\":true")) {
                    callback.onLoginExitoso(cuerpo); // Puedes parsear el JSON aquí si necesitas datos específicos
                } else {
                    callback.onLoginFallido("Credenciales incorrectas");
                }
            } else if (codigo == 401) {
                callback.onLoginFallido("Usuario o contraseña incorrectos");
            } else if (codigo == 404) {
                callback.onLoginFallido("Servicio no encontrado");
            } else if (codigo >= 500) {
                callback.onLoginFallido("Error del servidor");
            } else {
                callback.onLoginFallido("Error desconocido: " + codigo);
            }
        } catch (Exception e) {
            Log.e("LOGIN_ERROR", "Error procesando respuesta: " + e.getMessage());
            callback.onLoginFallido("Error procesando la respuesta");
        }
    }

    /**
     * Interfaz callback para manejar los resultados del login
     */
    public interface LoginCallback {
        void onLoginExitoso(String respuesta);
        void onLoginFallido(String mensajeError);
    }
}