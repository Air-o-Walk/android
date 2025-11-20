package com.example.air_o_walk_sprint0;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

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
        setContentView(R.layout.activity_gamificacion);

        // Obtener el user_id del Intent
        userId = getIntent().getIntExtra("USER_ID", -1);

        if (userId == -1) {
            Toast.makeText(this, "Error: No se pudo obtener el ID de usuario", Toast.LENGTH_SHORT).show();
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
        cargarPuntos();
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
                cargarPuntos();
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

    private void cargarPuntos() {
        // Llamar a la API para obtener puntos actualizados
        PeticionarioREST elPeticionario = new PeticionarioREST();

        elPeticionario.hacerPeticionREST("GET", "http://api.sagucre.upv.edu.es/puntos",
                null,
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String cuerpoRes) {
                        Log.d("GAMIFICACION", "Respuesta recibida - Código: " + codigo);

                        if (codigo == 200) {
                            try {
                                JSONObject json = new JSONObject(cuerpoRes);
                                int puntosTotales = json.getInt("puntos");

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

    private void mostrarError(final String mensaje) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(GamificacionActivity.this, mensaje, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Método público para actualizar los puntos de la última sesión desde otra parte de la app
    public void actualizarUltimosPuntos(int puntos) {
        tvUltimosPuntos.setText(String.valueOf(puntos));
        gamificacion.setUltimosPuntosObtenidos(puntos);
    }

    // Método para sumar puntos temporalmente (sin guardar en BBDD)
    private void sumarPuntos(int cantidad) {
        puntosTemporales += cantidad;
        tvUltimosPuntos.setText(String.valueOf(puntosTemporales));
        gamificacion.setUltimosPuntosObtenidos(puntosTemporales);

        Toast.makeText(this,
                "+" + cantidad + " puntos añadidos. Total sesión: " + puntosTemporales,
                Toast.LENGTH_SHORT).show();
    }

    // Método para guardar los puntos de la sesión actual en la BBDD
    private void guardarPuntosEnBBDD() {
        if (puntosTemporales > 0) {
            gamificacion.sumarPuntosDelaUltimaSesionBBDD();
            Toast.makeText(this, "Guardando " + puntosTemporales + " puntos en la base de datos...", Toast.LENGTH_SHORT).show();

            // Después de guardar, actualizamos los puntos totales
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    cargarPuntos();
                    puntosTemporales = 0; // Reiniciar puntos temporales
                }
            }, 1500); // Esperar 1.5 segundos antes de actualizar
        } else {
            Toast.makeText(this, "No hay puntos nuevos para guardar", Toast.LENGTH_SHORT).show();
        }
    }

    // Método público para guardar los puntos de la última sesión (por compatibilidad)
    public void guardarPuntosDelaUltimaSesion() {
        guardarPuntosEnBBDD();
    }
}