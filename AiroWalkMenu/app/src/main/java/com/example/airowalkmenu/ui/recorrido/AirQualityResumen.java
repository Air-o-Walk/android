package com.example.airowalkmenu.ui.recorrido;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import com.example.airowalkmenu.data.network.PeticionarioREST;


// --------------------------------------------------------------
// AirQualityResumen.java
// Autor: Meryame Ait Boumlik
// Descripción: Clase responsable de consultar al backend el resumen de calidad del aire de un usuario, incluyendo:
//- Estado (buena / regular / picos / mala)
//- Mensaje resumen
//- Tiempo activo (h)
//- Distancia recorrida (km)
//- Puntos obtenidos
//- Datos para la gráfica (timestamps, O3, NO2, CO)
// --------------------------------------------------------------
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
    }

    // --------------------------------------------------------------
    // Constructor
    // Descripción: Guarda el ID del usuario cuyos datos se van a consultar.
    // Parámetros: userId : identificador del usuario.
    // Diseño: userId -> new AirQualityResumen()
    // --------------------------------------------------------------
    public AirQualityResumen(int userId) {
        this.userId = userId;
    }

    // --------------------------------------------------------------
    // obtenerResumen()
    // Descripción: Hace una llamada REST al backend: GET /usuario/calidad-aire-resumen?userId=...
    // Diseño:  userId -> obtenerResumen() -> AirQualityData | error
    // Parámetros: - listener : callbacks de éxito o error
    // --------------------------------------------------------------
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

    // --------------------------------------------------------------
    // parsearRespuesta()
    // Descripción: Convierte el JSON recibido del backend en un objeto AirQualityData listo para usar en la Activity.
    // Diseño: JSON -> parsearRespuesta() -> AirQualityData
    // --------------------------------------------------------------
    private AirQualityData parsearRespuesta(String cuerpoJson) throws Exception {

        JSONObject root = new JSONObject(cuerpoJson);

        AirQualityData data = new AirQualityData();

        data.status = root.getString("status");
        data.summaryText = root.getString("summaryText");

        data.timeHours = root.getDouble("timeHours");
        data.distanceKm = root.getDouble("distanceKm");
        data.steps = root.getInt("steps");
        data.points = root.getInt("points");

        JSONObject graph = root.getJSONObject("graph");

        data.timestamps = graph.getJSONArray("timestamps");
        data.o3 = graph.getJSONArray("o3");
        data.no2 = graph.getJSONArray("no2");
        data.co = graph.getJSONArray("co");

        return data;
    }
}
