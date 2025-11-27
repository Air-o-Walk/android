package com.example.air_o_walk_sprint0;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase que encapsula toda la lógica de peticiones REST
 * relacionadas con el sistema de canjes de premios
 */
public class LogicaCanjeos {

    private static final String TAG = "LogicaCanjeos";
    // ========================================================================
    // INTERFACES PARA CALLBACKS
    // ========================================================================

    /**
     * Callback para cuando se obtienen los puntos del usuario
     */
    public interface CallbackPuntos {
        void onPuntosObtenidos(int puntos);
        void onError(String mensaje);
    }

    /**
     * Callback para cuando se obtienen los premios
     */
    public interface CallbackPremios {
        void onPremiosObtenidos(List<Premio> premios);
        void onError(String mensaje);
    }

    /**
     * Callback para cuando se realiza un canje
     */
    public interface CallbackCanje {
        void onCanjeExitoso(String mensaje, String codigoCupon, int puntosRestantes);
        void onError(String mensaje);
    }

    // ========================================================================
    // MÉTODOS PÚBLICOS
    // ========================================================================
    /**
     * Obtener lista de premios disponibles
     * GET /prizes
     */
    public void obtenerPremios(CallbackPremios callback) {
        PeticionarioREST peticionario = new PeticionarioREST();

        peticionario.hacerPeticionREST("GET", "http://api.sagucre.upv.edu.es/prizes",
                null,
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String cuerpo) {
                        Log.d(TAG, "obtenerPremios - Código: " + codigo + ", Cuerpo: " + cuerpo);

                        if (codigo == 200) {
                            try {
                                JSONObject json = new JSONObject(cuerpo);

                                if (json.getBoolean("success")) {
                                    JSONArray premiosArray = json.getJSONArray("prizes");
                                    List<Premio> listaPremios = parsearPremios(premiosArray);
                                    callback.onPremiosObtenidos(listaPremios);
                                } else {
                                    callback.onError("Error al obtener premios");
                                }

                            } catch (JSONException e) {
                                Log.e(TAG, "Error parseando premios: " + e.getMessage());
                                callback.onError("Error al procesar premios");
                            }
                        } else {
                            callback.onError("Error del servidor: " + codigo);
                        }
                    }
                }
        );
    }

    /**
     * Canjear un premio
     * POST /redeem
     */
    public void canjearPremio(int userId, int prizeId, CallbackCanje callback) {
        PeticionarioREST peticionario = new PeticionarioREST();

        try {
            // Crear body JSON
            JSONObject body = new JSONObject();
            body.put("userId", userId);
            body.put("prizeId", prizeId);

            peticionario.hacerPeticionREST("POST", "http://api.sagucre.upv.edu.es/redeem",
                    body.toString(),
                    new PeticionarioREST.RespuestaREST() {
                        @Override
                        public void callback(int codigo, String cuerpo) {
                            Log.d(TAG, "canjearPremio - Código: " + codigo + ", Cuerpo: " + cuerpo);

                            try {
                                JSONObject json = new JSONObject(cuerpo);

                                if (json.optBoolean("success", false)) {
                                    // Canje exitoso
                                    String mensaje = json.getString("message");
                                    String codigoCupon = json.getString("couponCode");
                                    int puntosRestantes = json.getInt("remainingPoints");

                                    callback.onCanjeExitoso(mensaje, codigoCupon, puntosRestantes);

                                } else {
                                    // Canje fallido
                                    String mensaje = json.optString("message", "Error al canjear premio");
                                    callback.onError(mensaje);
                                }

                            } catch (JSONException e) {
                                Log.e(TAG, "Error parseando respuesta de canje: " + e.getMessage());
                                callback.onError("Error al procesar respuesta");
                            }
                        }
                    }
            );

        } catch (JSONException e) {
            Log.e(TAG, "Error creando JSON: " + e.getMessage());
            callback.onError("Error al preparar petición");
        }
    }

    // ========================================================================
    // MÉTODOS PRIVADOS DE PARSEO
    // ========================================================================

    /**
     * Parsear JSONArray de premios a List<Premio>
     */
    private List<Premio> parsearPremios(JSONArray array) throws JSONException {
        List<Premio> premios = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);

            Premio premio = new Premio();
            premio.setId(obj.getInt("id"));
            premio.setNombre(obj.getString("name"));
            premio.setDescripcion(obj.optString("description", null));
            premio.setPuntosRequeridos(obj.getInt("points_required"));
            premio.setCantidadDisponible(obj.getInt("quantity_available"));
            premio.setCantidadInicial(obj.getInt("initial_quantity"));
            premio.setActivo(obj.getInt("active"));

            premios.add(premio);
        }

        return premios;
    }

    /**
     * Parsear JSONArray de redenciones a List<Redencion>
     */
    private List<Canjeo> parsearRedenciones(JSONArray array) throws JSONException {
        List<Canjeo> redenciones = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);

            Canjeo redencion = new Canjeo();
            redencion.setId(obj.getInt("id"));
            redencion.setCodigoCupon(obj.getString("coupon_code"));
            redencion.setFechaRedencion(obj.getString("redemption_date"));
            redencion.setNombrePremio(obj.getString("prize_name"));
            redencion.setDescripcion(obj.optString("description", null));
            redencion.setPuntosRequeridos(obj.getInt("points_required"));

            redenciones.add(redencion);
        }

        return redenciones;
    }

} // class