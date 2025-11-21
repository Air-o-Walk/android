package com.example.air_o_walk_sprint0;

import android.util.Log;

public class MeasurementsLogica {

    private int userId;
    private int pasos;
    private int puntos;
    private long tiempo;

    private OnFinishedListener listener; // ← ESTO FALTABA


    public MeasurementsLogica(int userId, int pasos, int puntos, long tiempo) {
        this.userId = userId;
        this.pasos = pasos;
        this.puntos = puntos;
        this.tiempo = tiempo;
    }

    /**
     * Establece el listener para saber cuándo termina la petición
     */
    public void setOnFinishedListener(OnFinishedListener listener) {
        this.listener = listener;
    }


    public void guardarDailyStats() {
        PeticionarioREST elPeticionario = new PeticionarioREST();

        String cuerpo = "{"
                + "\"userId\": " + this.userId + ", "
                + "\"points\": " + this.puntos + ", "
                + "\"distance\": " + this.pasos + ", "
                + "\"activeHours\": " + this.tiempo
                + "}";

        elPeticionario.hacerPeticionREST("POST", "http://api.sagucre.upv.edu.es/user/daily-stats",
                cuerpo, // GET no necesita cuerpo
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String cuerpoRes) {
                        Log.d("SUMA_PUNTOS_RESPUESTA", "TENGO RESPUESTA:\nCodigo: " + codigo + "\nCuerpo: \n" + cuerpoRes);

                        if (listener != null) {
                            listener.onFinished( codigo == 200);
                        }
                    }
                }
        );
    }

    /**
     * Interface para notificar cuando la petición termine
     */
    public interface OnFinishedListener {
        void onFinished(boolean success);
    }



}
