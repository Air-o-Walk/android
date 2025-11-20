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

public class Gamificacion {

    private int puntos;
    private int id_user;
    private float multiplicador = 10;

    public Gamificacion(int id_user) {
        this.id_user = id_user;

    }

    public void setMultiplicador(float multiplicador) {
        this.multiplicador = multiplicador;
    }

    public int calcularPuntosMedianteDistancia(float distancia) {
        return Math.round(distancia * multiplicador);
    }

    public void sumarPuntosLocal(int puntosGandos) {
        this.puntos = this.puntos + puntosGandos;
    }

    public void sumarPuntosActualesBBDD() {
        PeticionarioREST elPeticionario = new PeticionarioREST();

        String cuerpo = "{"
                + "\"username\": \"" + this.id_user + "\", "
                + "\"puntos\": \"" + this.puntos + "\""
                +"}";;

        elPeticionario.hacerPeticionREST("PUT", "http://api.sagucre.upv.edu.es/puntos",
                cuerpo, // GET no necesita cuerpo
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String cuerpoRes) {
                        Log.d("SUMA_PUNTOS_RESPUESTA", "TENGO RESPUESTA:\nCodigo: " + codigo + "\nCuerpo: \n" + cuerpoRes);
                    }
                }
        );
    }

    private void recibirPuntos() {
        PeticionarioREST elPeticionario = new PeticionarioREST();

        elPeticionario.hacerPeticionREST("GET", "http://api.sagucre.upv.edu.es/puntos",
                null, //
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String cuerpoRes) {
                        Log.d("PUNTOS_RESPUESTA", "TENGO RESPUESTA:\nCodigo: " + codigo + "\nCuerpo: \n" + cuerpoRes);
                    }
                }
        );
    }

}
