/*header
    AUTOR: SANTIAGO AGUIRRE

* int puntos
* calcurlarPuntosMedianteDistancia
* sumarPuntosLocal
* sumarPuntos
* recibirPuntos
* getPuntos
* */

package com.example.air_o_walk_sprint0;

import android.util.Log;

import org.json.JSONObject;

public class Gamificacion {

    private int puntosTotales;  /*PUNTOS TOTALES DEL USURIO*/
    private int ultimosPuntosObtenidos;  /*PUNTOS OBTENIDOS DURANTE LA ULTIMA SESION*/
    private int id_user;
    private float multiplicador = 10;

    public Gamificacion(int id_user) {
        this.id_user = id_user;
        this.ultimosPuntosObtenidos = 0;
    }

    public void setMultiplicador(float multiplicador) {
        this.multiplicador = multiplicador;
    }

    public int calcularPuntosMedianteDistancia(float distancia) {
        return Math.round(distancia * multiplicador);
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

    public void actualizarPuntosTotales() {

        PeticionarioREST elPeticionario = new PeticionarioREST();

        elPeticionario.hacerPeticionREST("GET", "http://api.sagucre.upv.edu.es/points/" + this.id_user,
                null,
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String cuerpoRes) {
                        Log.d("PUNTOS_RESPUESTA", "TENGO RESPUESTA:\nCodigo: " + codigo + "\nCuerpo: \n" + cuerpoRes);

                        try {
                            JSONObject json = new JSONObject(cuerpoRes);

                            // Extraer "puntos"
                            int puntos = json.getInt("points");

                            // Guardarlo en tu variable de clase
                            puntosTotales = puntos;

                            Log.d("PUNTOS_RESPUESTA", "puntosTotales = " + puntosTotales);

                        } catch (Exception e) {
                            Log.e("PUNTOS_ERROR", "Error parseando JSON: " + e.getMessage());
                        }
                    }
                }
        );
    }

}
