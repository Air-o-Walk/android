package com.example.air_o_walk_sprint0;

import android.util.Log;

/**
 * Clase que encapsula la lógica de negocio para el envío de mediciones al backend.
 * Recibe el tipo y valor de la medición y realiza la petición HTTP POST.
 */
public class Logica {

    private int tipoMedida;   // Tipo de la medición (11 = gas, 12 = temperatura)
    private int valorMedida;  // Valor numérico de la medición

    /**
     * Constructor de la clase Logica.
     * @param tipo Tipo de medición (11 para gas, 12 para temperatura)
     * @param valor Valor de la medición
     */
    public Logica(int tipo, int valor) {
        this.tipoMedida = tipo;
        this.valorMedida = valor;
    }

    /**
     * Envía la medición al backend mediante una petición REST POST.
     * Utiliza PeticionarioREST para gestionar la petición asíncrona.
     */
    public void guardarMedcion(){
        PeticionarioREST elPeticionario = new PeticionarioREST();
        String cuerpo = constructorDeCuerpo(); // Construye el cuerpo JSON de la petición

        elPeticionario.hacerPeticionREST("POST",  "http://sagucre.upv.edu.es/api/medicion",
                cuerpo, new PeticionarioREST.RespuestaREST () {
                    @Override
                    public void callback(int codigo, String cuerpo) {
                        // Log para depuración: muestra el código de respuesta y el cuerpo recibido
                        Log.d( "pruebasPeticionario", "TENGO RESPUESTA:\ncodigo = " + codigo + "\ncuerpo: \n" + cuerpo);

                    }
                }
        );
    }

    /**
     * Construye el cuerpo JSON para la petición POST.
     * Convierte el tipoMedida numérico a su representación en texto ("gas" o "temperatura").
     * @return Cadena JSON con los datos de la medición.
     */
    private String constructorDeCuerpo(){

        String tipoStr = "";

        // Mapeo de tipoMedida numérico a texto
        if(this.tipoMedida == 11){
            tipoStr = "gas";
        } else if (this.tipoMedida == 12) {
            tipoStr = "temperatura";
        }

        // Log para depuración del tipo de medición
        Log.d("PROBLEMA DE LA TEMPERATURA", "EL tipoStr = " + tipoStr);

        // Retorna el cuerpo en formato JSON
        return "{\"tipo\": \"" + tipoStr + "\", \"valor\": " + this.valorMedida + "}";
    }

    // Puedes añadir aquí métodos adicionales si necesitas ampliar la lógica

}
