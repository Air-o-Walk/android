package com.example.air_o_walk_sprint0;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;
/**
 * @class GamificacionActivity
 * @brief Pantalla de gestión y prueba de la gamificación del usuario.
 *
 * Esta actividad permite visualizar los puntos totales del usuario,
 * acumular puntos de forma temporal durante una sesión y guardarlos
 * posteriormente en el backend.
 *
 * Incluye funcionalidades para:
 * - Consultar los puntos totales almacenados
 * - Sumar puntos temporalmente
 * - Persistir los puntos de la sesión en la base de datos
 *
 * La comunicación con el backend se realiza de forma asíncrona.
 *
 * @author Santiago Aguirre y Adenor Buret
 * @version 1.0
 */

public class GamificacionActivity extends AppCompatActivity {

    private TextView tvPuntosTotales;
    private TextView tvUltimosPuntos;
    private Button btnActualizarPuntos;
    private Button btnVolver;
    private Button btnSumar10;
    private Button btnSumar50;
    private Button btnGuardarPuntos;

    private Gamificacion gamificacion;
    private int userId;
    private int puntosTemporales = 0; // Puntos acumulados en esta sesión

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ⭐ VERIFICAR SESIÓN ACTIVA
        if (!verificarSesionActiva()) {
            return; // Si no hay sesión, redirige a Login y detiene ejecución
        }

        setContentView(R.layout.activity_gamificacion);

        // Obtener userId del Intent o SharedPreferences
        userId = getIntent().getIntExtra("USER_ID", -1);

        if (userId == -1) {
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            userId = prefs.getInt("user_id", -1);
        }

        if (userId == -1) {
            Toast.makeText(this, "Error de sesión", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Inicializar vistas
        inicializarVistas();

        // Crear instancia de Gamificacion
        gamificacion = new Gamificacion(userId);

        // Configurar listeners
        configurarListeners();

        // Cargar puntos iniciales
        cargarPuntosTotales();
    }

    private void inicializarVistas() {
        tvPuntosTotales = findViewById(R.id.tvPuntosTotales);
        tvUltimosPuntos = findViewById(R.id.tvUltimosPuntos);
        btnActualizarPuntos = findViewById(R.id.btnActualizarPuntos);
        btnVolver = findViewById(R.id.btnVolver);
        btnSumar10 = findViewById(R.id.btnSumar10);
        btnSumar50 = findViewById(R.id.btnSumar50);
        btnGuardarPuntos = findViewById(R.id.btnGuardarPuntos);
    }

    private void configurarListeners() {
        // Botón para sumar 10 puntos
        btnSumar10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sumarPuntos(10);
            }
        });

        // Botón para sumar 50 puntos
        btnSumar50.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sumarPuntos(50);
            }
        });

        // Botón para guardar puntos en la base de datos
        btnGuardarPuntos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarPuntosEnBBDD();
            }
        });

        // Botón para actualizar puntos desde la BBDD
        btnActualizarPuntos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cargarPuntosTotales();
                Toast.makeText(GamificacionActivity.this, "Actualizando puntos...", Toast.LENGTH_SHORT).show();
            }
        });

        // Botón para volver
        btnVolver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Cierra la activity y vuelve a la anterior
            }
        });
    }

    /**
     * Carga los puntos totales desde la BBDD y actualiza la UI
     */
    private void cargarPuntosTotales() {
        PeticionarioREST elPeticionario = new PeticionarioREST();

        // Usar el método público de Gamificacion
        elPeticionario.hacerPeticionREST("GET", "http://api.sagucre.upv.edu.es/points/" + userId,
                null,
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String cuerpoRes) {
                        Log.d("GAMIFICACION", "Respuesta recibida - Código: " + codigo);

                        if (codigo == 200) {
                            try {
                                JSONObject json = new JSONObject(cuerpoRes);
                                int puntosTotales = json.getInt("points");

                                // Actualizar UI en el hilo principal
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        tvPuntosTotales.setText(String.valueOf(puntosTotales));
                                        Toast.makeText(GamificacionActivity.this,
                                                "Puntos actualizados correctamente",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });

                            } catch (Exception e) {
                                Log.e("GAMIFICACION_ERROR", "Error parseando JSON: " + e.getMessage());
                                mostrarError("Error al procesar los datos");
                            }
                        } else {
                            mostrarError("Error al cargar puntos. Código: " + codigo);
                        }
                    }
                }
        );
    }

    /**
     * Suma puntos temporalmente (sin guardar en BBDD)
     */
    private void sumarPuntos(int cantidad) {
        puntosTemporales += cantidad;
        tvUltimosPuntos.setText(String.valueOf(puntosTemporales));
        gamificacion.setUltimosPuntosObtenidos(puntosTemporales);

        Toast.makeText(this,
                "+" + cantidad + " puntos añadidos. Total sesión: " + puntosTemporales,
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Guarda los puntos de la sesión actual en la BBDD
     */
    private void guardarPuntosEnBBDD() {
        if (puntosTemporales > 0) {
            gamificacion.sumarPuntosDelaUltimaSesionBBDD();
            Toast.makeText(this, "Guardando " + puntosTemporales + " puntos en la base de datos...", Toast.LENGTH_SHORT).show();

            // Después de guardar, actualizamos los puntos totales
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Usar el método público de Gamificacion
                    gamificacion.actualizarPuntosTotales();

                    // También actualizamos la UI manualmente
                    cargarPuntosTotales();

                    // Reiniciar puntos temporales
                    puntosTemporales = 0;
                    tvUltimosPuntos.setText("0");
                }
            }, 1500); // Esperar 1.5 segundos antes de actualizar
        } else {
            Toast.makeText(this, "No hay puntos nuevos para guardar", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarError(final String mensaje) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(GamificacionActivity.this, mensaje, Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean verificarSesionActiva() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        int savedUserId = prefs.getInt("user_id", -1);
        String savedToken = prefs.getString("token", null);
        boolean sesionActiva = prefs.getBoolean("sesion_activa", false);

        if (savedUserId == -1 || savedToken == null || !sesionActiva) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return false;
        }
        return true;
    }
}