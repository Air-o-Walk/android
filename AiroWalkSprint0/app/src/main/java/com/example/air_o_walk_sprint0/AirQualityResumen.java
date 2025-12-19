package com.example.air_o_walk_sprint0;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @class AirQualityResumen
 * @brief Gestiona la obtención del resumen de calidad del aire de un usuario.
 *
 * Esta clase se encarga de comunicarse con la API del backend
 * para obtener una vista resumida de la exposición del usuario
 * a la calidad del aire, incluyendo:
 * - Estado de la calidad del aire
 * - Mensaje resumen
 * - Tiempo activo y distancia recorrida
 * - Pasos y puntos obtenidos
 * - Datos para la gráfica (timestamps, O3, NO2, CO, índice)
 *
 * Los resultados se devuelven de forma asíncrona mediante una
 * interfaz de tipo listener.
 *
 * @author Meryame Ait Boumlik
 * @version 1.0
 */

public class AirQualityResumen {
    private static final String TAG = "AirQualityResumen";
    private int userId;
    // -----------------------------------------------------------------
    // Listener para devolver datos a la Activity
    // -----------------------------------------------------------------
    public interface Listener {
        void onResultado(AirQualityData data);
        void onError(String error);
    }

    // -----------------------------------------------------------------
    // Modelo de datos con la respuesta del backend
    // -----------------------------------------------------------------
    public static class AirQualityData {
        public String status;
        public String summaryText;

        public double timeHours;
        public double distanceKm;
        public int steps;
        public int points;

        public JSONArray timestamps;
        public JSONArray o3;
        public JSONArray no2;
        public JSONArray co;
        public JSONArray index;

    }

    /**
     * Constructor.
     *
     * Descripción: Guarda el ID del usuario cuyos datos se van a consultar.
     *
     * @details
     * Diseño:
     * userId -> new AirQualityResumen()
     *
     * @param userId identificador del usuario
     */
    public AirQualityResumen(int userId) {
        this.userId = userId;
    }

    /**
     * obtenerResumen()
     *
     * Descripción: Hace una llamada REST al backend:
     * GET /usuario/calidad-aire-resumen?userId=...
     *
     * @details
     * Diseño:
     * userId -> obtenerResumen() -> AirQualityData | error
     *
     * @param listener callbacks de éxito o error
     */
    public void obtenerResumen(Listener listener) {

        String url = "http://api.sagucre.upv.edu.es/usuario/calidad-aire-resumen?userId=" + userId;

        Log.d(TAG, "Haciendo petición GET: " + url);

        PeticionarioREST pet = new PeticionarioREST();

        pet.hacerPeticionREST(
                "GET",
                url,
                null,
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String cuerpo) {

                        Log.d(TAG, "Código respuesta = " + codigo);
                        Log.d(TAG, "Cuerpo respuesta = " + cuerpo);

                        if (codigo != 200) {
                            listener.onError("HTTP error: " + codigo);
                            return;
                        }

                        try {
                            AirQualityData data = parsearRespuesta(cuerpo);
                            listener.onResultado(data);

                        } catch (Exception e) {
                            Log.e(TAG, "Error parseando JSON", e);
                            listener.onError("Error parseando JSON: " + e.getMessage());
                        }
                    }
                }
        );
    }

    /**
     * parsearRespuesta()
     *
     * Descripción: Convierte el JSON recibido del backend en un objeto
     * AirQualityData listo para usar en la Activity.
     *
     * @details
     * Diseño:
     * JSON -> parsearRespuesta() -> AirQualityData
     *
     * @param cuerpoJson JSON recibido del backend
     * @return objeto AirQualityData parseado
     * @throws Exception si el JSON es inválido
     */
    private AirQualityData parsearRespuesta(String cuerpoJson) throws Exception {

        JSONObject root = new JSONObject(cuerpoJson);

        AirQualityData data = new AirQualityData();

        data.status = root.getString("status");
        data.summaryText = root.getString("summaryText");

        data.timeHours = root.optDouble("timeHours", 0);
        data.distanceKm = root.optDouble("distanceKm", 0);

        // steps unknown → safe fallback
        data.steps = root.optInt("steps", 0);

        // Backend returns points sometimes as "0" (string)
        data.points = root.optInt("points", 0);

        JSONObject graph = root.getJSONObject("graph");

        data.timestamps = graph.getJSONArray("timestamps");
        data.o3 = graph.getJSONArray("o3");
        data.no2 = graph.getJSONArray("no2");
        data.co = graph.getJSONArray("co");

        // NEW: index might not exist on server yet
        data.index = graph.optJSONArray("index");
        if (data.index == null) {
            data.index = new JSONArray(); // Avoid crash
        }

        return data;
    }

}
