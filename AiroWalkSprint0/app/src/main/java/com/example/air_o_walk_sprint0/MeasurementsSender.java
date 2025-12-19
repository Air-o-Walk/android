package com.example.air_o_walk_sprint0;


import android.location.Location;
import android.util.Log;

import org.json.JSONObject;

/**
 * @class MeasurementsSender
 * @brief Envía las mediciones completas de una recorrida al backend.
 *
 * Clase responsable de enviar todas las mediciones generadas durante
 * una recorrida al servidor, incluyendo:
 * - Ubicación GPS (latitud, longitud)
 * - Mediciones de gases (O3, CO, NO2)
 * - Pasos totales caminados
 * - Tiempo total caminado
 *
 * Las mediciones se envían en dos situaciones:
 * 1. Cuando el usuario finaliza o pausa la recorrida manualmente
 * 2. Cuando se pierde la conexión con el beacon (desconexión abrupta)
 *
 * Utiliza peticiones REST al endpoint /measurements y notifica
 * el resultado mediante un callback.
 *
 * @author Adenor Buret
 * @version 1.0
 */

public class MeasurementsSender {

    private static final String ETIQUETA_LOG = ">>>>";
    private static final String BASE_URL = "http://api.sagucre.upv.edu.es";


    //Envía todas las mediciones de la recorrida al servidor
    public static void enviarMedicionCompleta(
            //ID del nodo/beacon vinculado
            int nodeId,
            //Última medición de O3 (ozono) en ppm
            float ultimaMedicionO3,
            //Última medición de CO (monóxido de carbono) en ppm
            float ultimaMedicionCO,
            //Última medición de NO2 (dióxido de nitrógeno) en ppm
            float ultimaMedicionNO2,
            //Última ubicación GPS conocida
            Location ubicacion,
            //Total de pasos caminados en la sesión
            //int pasosTotal,
            //Tiempo total caminado en segundos
            //long tiempoTotal,
            //Callback para manejar la respuesta
            MeasurementCallback callback) {

        if (ubicacion == null) {
            Log.w(ETIQUETA_LOG, " MeasurementsSender: No hay ubicación GPS disponible");
            if (callback != null) {
                callback.onError("No hay ubicación GPS disponible");
            }
            return;
        }

        if (nodeId == -1) {
            Log.e(ETIQUETA_LOG, " MeasurementsSender: nodeId es nulo o vacío");
            if (callback != null) {
                callback.onError("nodeId no válido");
            }
            return;
        }

        try {
            // Construir objeto JSON con todas las mediciones
            JSONObject datos = new JSONObject();

            // Datos obligatorios del endpoint /measurements
            datos.put("nodeId", nodeId);
            datos.put("co", ultimaMedicionCO);
            datos.put("o3", ultimaMedicionO3);
            datos.put("no2", ultimaMedicionNO2);
            datos.put("latitude", ubicacion.getLatitude());
            datos.put("longitude", ubicacion.getLongitude());

            // Datos adicionales de la recorrida
            /*datos.put("steps", pasosTotal);
            datos.put("walking_time_seconds", tiempoTotal);
            datos.put("accuracy", ubicacion.getAccuracy());
            datos.put("timestamp", System.currentTimeMillis());*/

            // Datos opcionales si están disponibles

            // Calcular distancia aproximada (0.75 metros por paso)
            //double distanciaAproximadaMetros = pasosTotal * 0.75;
            //datos.put("distance_meters", distanciaAproximadaMetros);

            Log.d(ETIQUETA_LOG, " ===============================================");
            Log.d(ETIQUETA_LOG, " MeasurementsSender: Enviando medición completa");
            Log.d(ETIQUETA_LOG, " - NodeId: " + nodeId);
            Log.d(ETIQUETA_LOG, " - O3: " + ultimaMedicionO3 + " ppm");
            Log.d(ETIQUETA_LOG, " - CO: " + ultimaMedicionCO + " ppm");
            Log.d(ETIQUETA_LOG, " - NO2: " + ultimaMedicionNO2 + " ppm");
            Log.d(ETIQUETA_LOG, " - Ubicación: " + ubicacion.getLatitude() + ", " + ubicacion.getLongitude());
            //Log.d(ETIQUETA_LOG, " - Pasos: " + pasosTotal);
            //Log.d(ETIQUETA_LOG, " - Tiempo: " + tiempoTotal + " segundos");
            //Log.d(ETIQUETA_LOG, " - Distancia aprox: " + String.format("%.2f", distanciaAproximadaMetros) + " metros");
            Log.d(ETIQUETA_LOG, " ===============================================");

            //Especificación de cual parte de la api que estamos solicitando
            String url = BASE_URL + "/measurements";

            // Realizar petición REST usando PeticionarioREST
            PeticionarioREST peticion = new PeticionarioREST();
            peticion.hacerPeticionREST("POST", url, datos.toString(),
                    new PeticionarioREST.RespuestaREST() {
                        @Override
                        public void callback(int codigo, String cuerpo) {
                            Log.d(ETIQUETA_LOG, " MeasurementsSender: Respuesta recibida");
                            Log.d(ETIQUETA_LOG, " - Código HTTP: " + codigo);
                            Log.d(ETIQUETA_LOG, " - Cuerpo: " + cuerpo);

                            if (codigo >= 200 && codigo < 300) {
                                Log.d(ETIQUETA_LOG, "Medición completa enviada exitosamente");
                                if (callback != null) {
                                    callback.onSuccess(cuerpo);
                                }
                            } else {
                                Log.e(ETIQUETA_LOG, "Error al enviar medición: código HTTP " + codigo);
                                if (callback != null) {
                                    callback.onError("Error del servidor: código " + codigo + " - " + cuerpo);
                                }
                            }
                        }
                    });

        } catch (Exception e) {
            Log.e(ETIQUETA_LOG, " MeasurementsSender: Excepción al preparar datos: " + e.getMessage());
            e.printStackTrace();
            if (callback != null) {
                callback.onError("Error al preparar datos: " + e.getMessage());
            }
        }
    }

     //Envía medición con información sobre tipo de desconexión
    public static void enviarMedicionConDesconexion(
            //ID del nodo
            String nodeId,
            //Última medición O3
            float ultimaMedicionO3,
            //Última medición CO
            float ultimaMedicionCO,
            //Última medición NO2
            float ultimaMedicionNO2,
            //Ubicación GPS
            Location ubicacion,
            //Pasos totales
            int pasosTotal,
            //Tiempo total en segundos
            long tiempoTotal,
            //Tipos de conexión "manual" o "abrupta"
            String tipoDesconexion,
            //Callback de respuesta
            MeasurementCallback callback) {

        if (ubicacion == null) {
            Log.w(ETIQUETA_LOG, " MeasurementsSender: No hay ubicación para enviar con desconexión");
            if (callback != null) {
                callback.onError("No hay ubicación GPS disponible");
            }
            return;
        }

        try {
            JSONObject datos = new JSONObject();

            // Datos obligatorios del endpoint
            datos.put("nodeId", nodeId);
            datos.put("co", ultimaMedicionCO);
            datos.put("o3", ultimaMedicionO3);
            datos.put("no2", ultimaMedicionNO2);
            datos.put("latitude", ubicacion.getLatitude());
            datos.put("longitude", ubicacion.getLongitude());

            // Datos adicionales
            datos.put("steps", pasosTotal);
            datos.put("walking_time_seconds", tiempoTotal);
            datos.put("accuracy", ubicacion.getAccuracy());
            datos.put("timestamp", System.currentTimeMillis());
            datos.put("tipo_desconexion", tipoDesconexion);
            datos.put("evento", "fin_recorrida");

            double distanciaAproximadaMetros = pasosTotal * 0.75;
            datos.put("distance_meters", distanciaAproximadaMetros);

            Log.d(ETIQUETA_LOG, " ===============================================");
            Log.d(ETIQUETA_LOG, " MeasurementsSender: Enviando medición por desconexión " + tipoDesconexion);
            Log.d(ETIQUETA_LOG, " - NodeId: " + nodeId);
            Log.d(ETIQUETA_LOG, " - Pasos: " + pasosTotal);
            Log.d(ETIQUETA_LOG, " - Tiempo: " + tiempoTotal + " segundos (" + (tiempoTotal / 60) + " minutos)");
            Log.d(ETIQUETA_LOG, " - Ubicación final: " + ubicacion.getLatitude() + ", " + ubicacion.getLongitude());
            Log.d(ETIQUETA_LOG, " ===============================================");

            String url = BASE_URL + "/measurements";

            PeticionarioREST peticion = new PeticionarioREST();
            peticion.hacerPeticionREST("POST", url, datos.toString(),
                    new PeticionarioREST.RespuestaREST() {
                        @Override
                        public void callback(int codigo, String cuerpo) {
                            Log.d(ETIQUETA_LOG, " Medición con desconexión: código=" + codigo);

                            if (codigo >= 200 && codigo < 300) {
                                Log.d(ETIQUETA_LOG, "Medición con desconexión enviada correctamente");
                                if (callback != null) {
                                    callback.onSuccess(cuerpo);
                                }
                            } else {
                                Log.e(ETIQUETA_LOG, "Error enviando medición con desconexión");
                                if (callback != null) {
                                    callback.onError("Error del servidor: " + codigo);
                                }
                            }
                        }
                    });

        } catch (Exception e) {
            Log.e(ETIQUETA_LOG, " Error preparando medición con desconexión: " + e.getMessage());
            e.printStackTrace();
            if (callback != null) {
                callback.onError("Error: " + e.getMessage());
            }
        }
    }



    //Interface para callbacks de respuestas
    public interface MeasurementCallback {
        void onSuccess(String respuesta);
        void onError(String error);
    }
}