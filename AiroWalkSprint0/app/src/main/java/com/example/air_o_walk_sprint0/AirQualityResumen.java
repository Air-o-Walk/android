package com.example.air_o_walk_sprint0;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

// -----------------------------------------------------------------------------
// AirQualityResumen.java
// Descripción:
//   Llama al backend para obtener el resumen de calidad del aire del usuario.
//   Sigue mismo diseño que RegistroNodo, usando PeticionarioREST.
// Diseño general:
//   new AirQualityResumen(userId)
//          -> obtenerResumen( listener )
//                -> GET REST
//                -> callback con objeto AirQualityData
// -----------------------------------------------------------------------------
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
        public int points;

        public JSONArray timestamps;
        public JSONArray o3;
        public JSONArray no2;
        public JSONArray co2;
    }

    // -----------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------
    public AirQualityResumen(int userId) {
        this.userId = userId;
    }

    // -----------------------------------------------------------------
    // obtenerResumen()
    // Descripción:
    //   Llama al backend y devuelve los datos procesados via listener.
    //   Similar a registrarNodo() pero usando GET.
    // -----------------------------------------------------------------
    public void obtenerResumen(Listener listener) {

        String url = "http://api.sagucre.upv.edu.es/usuario/calidad-aire-resumen?userId=" + userId;

        Log.d(TAG, "Haciendo petición GET: " + url);

        PeticionarioREST pet = new PeticionarioREST();

        pet.hacerPeticionREST(
                "GET",
                url,
                null,    // GET → no tiene cuerpo
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
                            Log.e(TAG, "Error parsing JSON", e);
                            listener.onError("Error parseando JSON: " + e.getMessage());
                        }
                    }
                }
        );
    }

    // -----------------------------------------------------------------
    // parsearRespuesta()
    // Descripción: Convierte JSON → objeto AirQualityData
    // -----------------------------------------------------------------
    private AirQualityData parsearRespuesta(String cuerpoJson) throws Exception {

        JSONObject root = new JSONObject(cuerpoJson);

        AirQualityData data = new AirQualityData();

        data.status = root.getString("status");
        data.summaryText = root.getString("summaryText");
        data.points = root.getInt("points");

        JSONObject graph = root.getJSONObject("graph");

        data.timestamps = graph.getJSONArray("timestamps");
        data.o3 = graph.getJSONArray("o3");
        data.no2 = graph.getJSONArray("no2");
        data.co2 = graph.getJSONArray("co2");

        return data;
    }
}
