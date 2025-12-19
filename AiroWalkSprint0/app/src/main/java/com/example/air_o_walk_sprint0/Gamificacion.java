

package com.example.air_o_walk_sprint0;

import android.util.Log;

import org.json.JSONObject;
/**
 * @class Gamificacion
 * @brief Gestiona la lógica de gamificación y puntos del usuario.
 *
 * Esta clase se encarga de calcular, almacenar y sincronizar los puntos
 * obtenidos por un usuario. Permite calcular puntos a partir de la distancia,
 * acumular puntos de la última sesión y consultar los puntos totales
 * almacenados en el backend.
 *
 * Funcionalidades principales:
 * - Cálculo de puntos mediante distancia recorrida
 * - Acumulación de puntos de la última sesión
 * - Sincronización de puntos con el backend
 * - Obtención asíncrona de los puntos totales del usuario
 *
 * @author Santiago Aguirre
 * @version 1.0
 */
public class Gamificacion {

    private int puntosTotales;  /*PUNTOS TOTALES DEL USURIO*/
    private int ultimosPuntosObtenidos;  /*PUNTOS OBTENIDOS DURANTE LA ULTIMA SESION*/
    private int id_user;
    private float multiplicador = 10;

    public Gamificacion(int id_user) {
        this.id_user = id_user;
        this.ultimosPuntosObtenidos = 0;
    }

    public int getPuntosTotales() {
        return puntosTotales;
    }

    public void setMultiplicador(float multiplicador) {
        this.multiplicador = multiplicador;
    }

    public int calcularPuntosMedianteDistancia(float distancia) {
        return Math.round(distancia / multiplicador);
    }

    public void setUltimosPuntosObtenidos(int ultimosPuntosObtenidos) {
        this.ultimosPuntosObtenidos = ultimosPuntosObtenidos;
    }

    public void sumarPuntosDelaUltimaSesionBBDD() {
        PeticionarioREST elPeticionario = new PeticionarioREST();

        String cuerpo = "{"
                + "\"userId\": " + this.id_user + ", "
                + "\"points\": " + this.ultimosPuntosObtenidos
                + "}";

        elPeticionario.hacerPeticionREST("PUT", "http://api.sagucre.upv.edu.es/points",
                cuerpo, // GET no necesita cuerpo
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String cuerpoRes) {
                        Log.d("SUMA_PUNTOS_RESPUESTA", "TENGO RESPUESTA:\nCodigo: " + codigo + "\nCuerpo: \n" + cuerpoRes);
                    }
                }
        );
    }


    // ========================================================================
    // VERSIÓN ORIGINAL (sin callback) - mantener compatibilidad
    // ========================================================================

    public void actualizarPuntosTotales() {
        actualizarPuntosTotales(null); // Llama a la versión con callback pero sin callback
    }

    // ========================================================================
    // VERSIÓN CON CALLBACK - AGREGAR ESTE MÉTODO
    // ========================================================================

    public void actualizarPuntosTotales(CallbackPuntos callback) {
        PeticionarioREST elPeticionario = new PeticionarioREST();

        elPeticionario.hacerPeticionREST("GET", "http://api.sagucre.upv.edu.es/points/" + this.id_user,
                null,
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String cuerpoRes) {
                        Log.d("PUNTOS_RESPUESTA", "TENGO RESPUESTA:\nCodigo: " + codigo + "\nCuerpo: \n" + cuerpoRes);

                        if (codigo == 200) {
                            try {
                                JSONObject json = new JSONObject(cuerpoRes);
                                int puntos = json.getInt("points");

                                // Guardarlo en la variable de clase
                                puntosTotales = puntos;

                                Log.d("PUNTOS_RESPUESTA", "puntosTotales = " + puntosTotales);

                                // Si hay callback, notificar
                                if (callback != null) {
                                    callback.onPuntosObtenidos(puntos);
                                }

                            } catch (Exception e) {
                                Log.e("PUNTOS_ERROR", "Error parseando JSON: " + e.getMessage());
                                if (callback != null) {
                                    callback.onError("Error al procesar puntos");
                                }
                            }
                        } else {
                            if (callback != null) {
                                callback.onError("Error del servidor: " + codigo);
                            }
                        }
                    }
                }
        );
    }

    /**
     * Interfaz para recibir los puntos de forma asíncrona
     */
    public interface CallbackPuntos {
        void onPuntosObtenidos(int puntos);
        void onError(String mensaje);
    }

}
