package com.example.air_o_walk;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

// ------------------------------------------------------------------------
// Clase que gestiona peticiones HTTP REST de forma asíncrona usando AsyncTask
// Permite enviar datos al backend y recibir respuestas sin bloquear la UI
// ------------------------------------------------------------------------
public class PeticionarioREST extends AsyncTask<Void, Void, Boolean> {

    // Método HTTP (GET, POST, etc.)
    private String elMetodo;
    // URL destino de la petición
    private String urlDestino;
    // Cuerpo de la petición (JSON), solo para métodos que lo requieran
    private String elCuerpo = null;
    // Interfaz para manejar la respuesta recibida
    private RespuestaREST laRespuesta;
    // Código HTTP recibido en la respuesta
    private int codigoRespuesta;
    // Cuerpo de la respuesta recibido del servidor
    private String cuerpoRespuesta = "";

    // Constructor: solo muestra log de creación
    public PeticionarioREST() {
        Log.d("clienterestandroid", "constructor()");
    }

    /**
     * Método principal para lanzar una petición REST.
     * @param metodo Método HTTP (ej: "POST", "GET")
     * @param urlDestino URL destino de la petición
     * @param cuerpo Cuerpo JSON para la petición (solo si no es GET)
     * @param laRespuesta Callback para manejar la respuesta
     */
    public void hacerPeticionREST(String metodo, String urlDestino, String cuerpo, RespuestaREST laRespuesta) {
        this.elMetodo = metodo;
        this.urlDestino = urlDestino;
        this.elCuerpo = cuerpo;
        this.laRespuesta = laRespuesta;

        this.execute(); // otro thread ejecutará doInBackground()
    }

    /**
     * Método que se ejecuta en segundo plano (hilo aparte).
     * Realiza la conexión HTTP, envía la petición y recibe la respuesta.
     */
    @Override
    protected Boolean doInBackground(Void... params) {
        Log.d("clienterestandroid", "doInBackground()");

        try {

            // envio la peticion

            // pagina web para hacer pruebas: URL url = new URL("https://httpbin.org/html");
            // ordinador del despatx 158.42.144.126 // OK URL url = new URL("http://158.42.144.126:8080");

            Log.d("clienterestandroid", "doInBackground() me conecto a >" + urlDestino + "<");

            URL url = new URL(urlDestino);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestMethod(this.elMetodo);
            // connection.setRequestProperty("Accept", "*/*);

            connection.setDoInput(true);

            // Si el método no es GET y hay cuerpo, lo envía en la petición
            if (!this.elMetodo.equals("GET") && this.elCuerpo != null) {
                Log.d("clienterestandroid", "doInBackground(): no es get, pongo cuerpo");
                connection.setDoOutput(true);

                DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
                Log.d("clienterestandroid","doInBackground(): lo que se escribe: " + this.elCuerpo );

                try {
                    // Convierte el String a JSONObject para asegurar formato correcto
                    JSONObject json = new JSONObject(this.elCuerpo);
                    String elCuerpoFormateado = json.toString();
                    byte[] postData = elCuerpoFormateado.getBytes(StandardCharsets.UTF_8);
                    dos.write(postData);
                    dos.flush();
                    dos.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Petición enviada, ahora obtiene la respuesta
            Log.d("clienterestandroid", "doInBackground(): petición enviada ");

            int rc = connection.getResponseCode();
            String rm = connection.getResponseMessage();
            String respuesta = "" + rc + " : " + rm;
            Log.d("clienterestandroid", "doInBackground() recibo respuesta = " + respuesta);
            this.codigoRespuesta = rc;

            try {
                // Lee el cuerpo de la respuesta si existe
                InputStream is = connection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));

                Log.d("clienterestandroid", "leyendo cuerpo");
                StringBuilder acumulador = new StringBuilder();
                String linea;
                while ((linea = br.readLine()) != null) {
                    Log.d("clienterestandroid", linea);
                    acumulador.append(linea);
                }
                Log.d("clienterestandroid", "FIN leyendo cuerpo");

                this.cuerpoRespuesta = acumulador.toString();
                Log.d("clienterestandroid", "cuerpo recibido=" + this.cuerpoRespuesta);

                connection.disconnect();

            } catch (IOException ex) {
                // Puede ocurrir si la respuesta REST no tiene cuerpo
                Log.d("clienterestandroid", "doInBackground() : parece que no hay cuerpo en la respuesta");
            }

            return true; // doInBackground() termina bien

        } catch (Exception ex) {
            Log.d("clienterestandroid", "doInBackground(): ocurrio alguna otra excepcion: " + ex.getMessage());
        }

        return false; // doInBackground() NO termina bien
    } // ()

    /**
     * Método que se ejecuta en el hilo principal tras doInBackground().
     * Llama al callback con el código y cuerpo de la respuesta.
     */
    protected void onPostExecute(Boolean comoFue) {
        Log.d("clienterestandroid", "onPostExecute() comoFue = " + comoFue);
        this.laRespuesta.callback(this.codigoRespuesta, this.cuerpoRespuesta);
    }

    /**
     * Interfaz para manejar la respuesta REST de forma asíncrona.
     * Implementa el método callback para recibir código y cuerpo.
     */
    public interface RespuestaREST {
        void callback(int codigo, String cuerpo);
    }

} // class


