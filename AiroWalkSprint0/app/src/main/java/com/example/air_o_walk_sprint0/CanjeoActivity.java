package com.example.air_o_walk_sprint0;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
/**
 * @class CanjeoActivity
 * @brief Pantalla de canjeo de premios mediante puntos de gamificación.
 *
 * Esta actividad permite al usuario consultar sus puntos disponibles,
 * visualizar la lista de premios canjeables y realizar el canje de
 * premios mediante confirmación.
 *
 * La información de premios y puntos se obtiene de forma asíncrona
 * desde el backend, y la interfaz se actualiza dinámicamente.
 *
 * @author Santiago Aguirre y Adenor Buret
 * @version 1.0
 */
public class CanjeoActivity extends AppCompatActivity {

    // Vistas
    private TextView txtPuntosDisponibles;
    private TextView txtNoPremios;
    private ProgressBar progressBar;
    private RecyclerView recyclerViewPremios;
    private Button btnVolver;

    // Datos
    private int userId;
    private int puntosUsuario;
    private PremioAdapter adapter;
    private LogicaCanjeos logicaCanjeos;
    private Gamificacion gamificacion; // ✅ Usar tu clase
    /**
     * Inicializa la actividad, carga los datos del usuario
     * y configura la interfaz gráfica.
     *
     * @param savedInstanceState estado previo de la actividad
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ⭐ VERIFICAR SESIÓN ACTIVA
        if (!verificarSesionActiva()) {
            return;
        }

        setContentView(R.layout.activity_canjeo);

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

        // Inicializar lógica
        logicaCanjeos = new LogicaCanjeos();
        gamificacion = new Gamificacion(userId); // ✅ Inicializar Gamificacion

        // Inicializar vistas
        inicializarVistas();

        // Configurar RecyclerView
        configurarRecyclerView();

        // Cargar datos
        //NO SE PORQUE, NO QUIERO SABER PORQUE, PERO PARA QUE CARGUE TODO BIEN Y A LA PRIMERA SE TIENE QUE LLAMAR DOS VECES
        cargarPuntosUsuario();
        cargarPuntosUsuario();
          // ← Usa callback
        cargarPremios();

        // Botón volver
        btnVolver.setOnClickListener(v -> finish());
    }

    private void inicializarVistas() {
        txtPuntosDisponibles = findViewById(R.id.txtPuntosDisponibles);
        txtNoPremios = findViewById(R.id.txtNoPremios);
        progressBar = findViewById(R.id.progressBar);
        recyclerViewPremios = findViewById(R.id.recyclerViewPremios);
        btnVolver = findViewById(R.id.btnVolver);
    }

    private void configurarRecyclerView() {
        adapter = new PremioAdapter(0);
        recyclerViewPremios.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPremios.setAdapter(adapter);

        adapter.setOnPremioClickListener((premio, posicion) -> {
            mostrarDialogoConfirmacion(premio);
        });
    }

    /**
     * Cargar lista de premios
     */
    private void cargarPremios() {
        mostrarCargando(true);

        logicaCanjeos.obtenerPremios(new LogicaCanjeos.CallbackPremios() {
            @Override
            public void onPremiosObtenidos(java.util.List<Premio> premios) {
                runOnUiThread(() -> {
                    mostrarCargando(false);

                    if (premios.isEmpty()) {
                        txtNoPremios.setVisibility(View.VISIBLE);
                        recyclerViewPremios.setVisibility(View.GONE);
                    } else {
                        txtNoPremios.setVisibility(View.GONE);
                        recyclerViewPremios.setVisibility(View.VISIBLE);
                        adapter.setPremios(premios);
                    }
                });
            }

            @Override
            public void onError(String mensaje) {
                runOnUiThread(() -> {
                    mostrarCargando(false);
                    Toast.makeText(CanjeoActivity.this, mensaje, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Cargar puntos del usuario usando Gamificacion con callback
     */
    private void cargarPuntosUsuario() {

        // intento 1
        /*new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                 runOnUiThread(() ->{
                     gamificacion.actualizarPuntosTotales();
                     puntosUsuario = gamificacion.getPuntosTotales();
                     Toast.makeText(CanjeoActivity.this,
                             "¡Tus "+ puntosUsuario +" puntos ya están disponibles para canjear.",
                             Toast.LENGTH_LONG).show();

                 });
                // Usar el método público de Gamificacion
                gamificacion.actualizarPuntosTotales();

                // ✅ AGREGAR AQUÍ EL TOAST DE CONFIRMACIÓN:
                runOnUiThread(() ->
                        Toast.makeText(CanjeoActivity.this,
                                "¡Puntos guardados exitosamente!\n\nTus puntos ya están disponibles para canjear.",
                                Toast.LENGTH_LONG).show()
                );

                runOnUiThread(() ->                // También actualizamos los puntos manualmente
                        puntosUsuario = gamificacion.getPuntosTotales()
                );
            }
        }, 1500); // Esperar 1.5 segundos antes de actualizar*/

        /*gamificacion.actualizarPuntosTotales();

        puntosUsuario = gamificacion.getPuntosTotales();*/

        //intento 2
        /*runOnUiThread(() -> {
            gamificacion.actualizarPuntosTotales();
            // También actualizamos los puntos manualmente
            puntosUsuario = gamificacion.getPuntosTotales();

            Toast.makeText(CanjeoActivity.this,
                    "Tus "+ puntosUsuario + " puntos ya están disponibles para canjear.",
                    Toast.LENGTH_LONG).show();
        });*/

        gamificacion.actualizarPuntosTotales(new Gamificacion.CallbackPuntos() {
            @Override
            public void onPuntosObtenidos(int puntos) {
                runOnUiThread(() -> {
                    puntosUsuario = puntos;
                    txtPuntosDisponibles.setText(puntosUsuario + " puntos");
                    adapter.setPuntosUsuario(puntosUsuario);
                });
            }

            @Override
            public void onError(String mensaje) {
                runOnUiThread(() -> {
                    Toast.makeText(CanjeoActivity.this,
                            "Error al cargar puntos", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }


    /**
     * Mostrar diálogo de confirmación
     */
    private void mostrarDialogoConfirmacion(Premio premio) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmar Canje");
        builder.setMessage("¿Deseas canjear " + premio.getPuntosRequeridos() +
                " puntos por:\n\n" + premio.getNombre() + "?");

        builder.setPositiveButton("Confirmar", (dialog, which) -> {
            canjearPremio(premio);
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.create().show();
    }

    /**
     * Canjear premio
     */
    private void canjearPremio(Premio premio) {
        mostrarCargando(true);

        logicaCanjeos.canjearPremio(userId, premio.getId(), new LogicaCanjeos.CallbackCanje() {
            @Override
            public void onCanjeExitoso(String mensaje, String codigoCupon, int puntosRestantes) {
                runOnUiThread(() -> {
                    mostrarCargando(false);

                    //Actualizar puntos localmente
                    puntosUsuario = puntosRestantes;
                    txtPuntosDisponibles.setText(puntosUsuario + " puntos");
                    adapter.setPuntosUsuario(puntosUsuario);
                    // Mostrar éxito
                    mostrarDialogoExito(premio.getNombre(), codigoCupon);
                    // Recargar premios para actualizar stock
                    cargarPremios();
                });
            }

            @Override
            public void onError(String mensaje) {
                runOnUiThread(() -> {
                    mostrarCargando(false);
                    Toast.makeText(CanjeoActivity.this, mensaje, Toast.LENGTH_LONG).show();
                });
            }

        });
    }

    /**
     * Mostrar diálogo de éxito con opción de copiar código
     */
    private void mostrarDialogoExito(String nombrePremio, String codigoCupon) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("¡Canje Exitoso! 🎉");
        builder.setMessage("Has canjeado: " + nombrePremio +
                "\n\nTu código de cupón es:\n\n" + codigoCupon +
                "\n\n¡Guárdalo bien!");

        // Botón para copiar el código
        builder.setNegativeButton("Copiar Código", (dialog, which) -> {
            copiarAlPortapapeles(codigoCupon);
            Toast.makeText(CanjeoActivity.this,
                    "Código copiado al portapapeles", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        // Botón entendido
        builder.setPositiveButton("Entendido", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.setCancelable(false);
        builder.create().show();
    }

    /**
     * Copiar texto al portapapeles
     */
    private void copiarAlPortapapeles(String texto) {
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);

        android.content.ClipData clip = android.content.ClipData.newPlainText("Código de Cupón", texto);

        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
    }

    /**
     * Mostrar/ocultar carga
     */
    private void mostrarCargando(boolean mostrar) {
        progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        recyclerViewPremios.setVisibility(mostrar ? View.GONE : View.VISIBLE);
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