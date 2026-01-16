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
            Toast.makeText(this,
                    "No pudimos identificar tu cuenta.\n\nVuelve a iniciar sesión para acceder a los canjeos.",
                    Toast.LENGTH_LONG).show();
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
        cargarPuntosUsuario();  // ← Usa callback
        cargarPremios();

        // Botón volver
        btnVolver.setOnClickListener(v -> finish());

        if (puntosUsuario == 0) {
            mostrarMensajeSinPuntos();
        }
    }

    private void mostrarMensajeSinPuntos() {
        new AlertDialog.Builder(this)
                .setTitle("Empieza a ganar puntos")
                .setMessage(
                        "Aún no tienes puntos para canjear premios.\n\n" +
                                "¿Cómo ganar puntos?\n" +
                                "• Realiza recorridos con tu sensor\n" +
                                "• Camina más distancia = más puntos\n" +
                                "• Mantén tu sensor conectado durante todo el recorrido\n\n" +
                                "¡Haz tu primer recorrido y vuelve para canjear premios!"
                )
                .setPositiveButton("Ir a hacer un recorrido", (d, w) -> {
                    finish(); // Volver a MainActivity
                })
                .setNegativeButton("Ver premios de todos modos", null)
                .show();
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
                        txtNoPremios.setText(
                                "No hay premios disponibles en este momento.\n\n" +
                                        "Vuelve más tarde para ver nuevas recompensas."
                        );
                        recyclerViewPremios.setVisibility(View.GONE);

                        // Opcional: Mostrar toast adicional
                        Toast.makeText(CanjeoActivity.this,
                                "Estamos actualizando el catálogo de premios.\n\nVuelve pronto para ver las novedades.",
                                Toast.LENGTH_SHORT).show();
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
                runOnUiThread(() ->
                        Toast.makeText(CanjeoActivity.this,
                                "No pudimos obtener tus puntos disponibles.\n\nVerifica tu conexión a internet e intenta nuevamente.",
                                Toast.LENGTH_LONG).show()
                );
            }
        });
    }


    /**
     * Mostrar diálogo de confirmación
     */
    private void mostrarDialogoConfirmacion(Premio premio) {
        // Verificar si tiene puntos suficientes ANTES de mostrar diálogo
        if (puntosUsuario < premio.getPuntosRequeridos()) {
            int puntosNecesarios = premio.getPuntosRequeridos() - puntosUsuario;

            new AlertDialog.Builder(this)
                    .setTitle("Puntos insuficientes")
                    .setMessage(
                            "Te faltan " + puntosNecesarios + " puntos para canjear este premio.\n\n" +
                                    "Tus puntos: " + puntosUsuario + "\n" +
                                    "Precio: " + premio.getPuntosRequeridos() + " puntos\n\n" +
                                    "¿Cómo ganar más puntos?\n" +
                                    "• Realiza más recorridos\n" +
                                    "• Camina distancias más largas\n" +
                                    "• Mantén tu sensor conectado"
                    )
                    .setPositiveButton("Entendido", null)
                    .setNeutralButton("Ver otros premios", (d, w) -> {
                        d.dismiss();
                    })
                    .show();
            return;
        }

        // Continuar con diálogo de confirmación normal...
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmar Canje");
        builder.setMessage(
                "¿Deseas canjear " + premio.getPuntosRequeridos() + " puntos por:\n\n" +
                        premio.getNombre() + "?\n\n" +
                        "Después del canje te quedarán: " +
                        (puntosUsuario - premio.getPuntosRequeridos()) + " puntos"
        );

        builder.setPositiveButton("Sí, canjear", (dialog, which) -> {
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

                    String mensajeAmigable = interpretarErrorCanje(mensaje);

                    new AlertDialog.Builder(CanjeoActivity.this)
                            .setTitle("No se pudo realizar el canje")
                            .setMessage(mensajeAmigable)
                            .setPositiveButton("Entendido", null)
                            .setNeutralButton("Reintentar", (d, w) -> {
                                canjearPremio(premio);
                            })
                            .show();
                });
            }

        });
    }

    /**
     * Mostrar diálogo de éxito con opción de copiar código
     */
    private void mostrarDialogoExito(String nombrePremio, String codigoCupon) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎉 ¡Canje Exitoso!");
        builder.setMessage(
                "Has canjeado: " + nombrePremio + "\n\n" +
                        "Tu código de cupón es:\n" +
                        codigoCupon + "\n\n" +
                        "📋 Guarda este código para usar tu premio.\n" +
                        "Puedes copiarlo tocando el botón de abajo."
        );

        builder.setNegativeButton("📋 Copiar Código", (dialog, which) -> {
            copiarAlPortapapeles(codigoCupon);
            Toast.makeText(CanjeoActivity.this,
                    "Código copiado al portapapeles.\n\nPega el código donde lo necesites.",
                    Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        builder.setPositiveButton("Entendido", (dialog, which) -> {
            Toast.makeText(CanjeoActivity.this,
                    "Recuerda usar tu código antes de que expire.",
                    Toast.LENGTH_SHORT).show();
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

    private String interpretarErrorCanje(String errorBackend) {
        if (errorBackend == null || errorBackend.isEmpty()) {
            return "Ocurrió un problema inesperado.\n\nIntenta nuevamente o contacta con soporte.";
        }

        // Puntos insuficientes
        if (errorBackend.contains("insufficient") ||
                errorBackend.contains("puntos") ||
                errorBackend.contains("not enough")) {
            return "No tienes suficientes puntos para este premio.\n\n" +
                    "Realiza más recorridos para ganar puntos.";
        }

        // Stock agotado
        if (errorBackend.contains("stock") ||
                errorBackend.contains("agotado") ||
                errorBackend.contains("sold out")) {
            return "Este premio se ha agotado.\n\n" +
                    "Elige otro premio o espera a que vuelva a estar disponible.";
        }

        // Error de red
        if (errorBackend.contains("timeout") ||
                errorBackend.contains("connection") ||
                errorBackend.contains("network")) {
            return "No pudimos conectar con el servidor.\n\n" +
                    "Verifica tu conexión a internet e intenta nuevamente.";
        }

        // Error de sesión
        if (errorBackend.contains("401") ||
                errorBackend.contains("unauthorized") ||
                errorBackend.contains("sesión")) {
            return "Tu sesión ha expirado.\n\n" +
                    "Cierra sesión y vuelve a iniciar sesión para continuar.";
        }

        // Error del servidor
        if (errorBackend.contains("500") ||
                errorBackend.contains("server error")) {
            return "El servidor está teniendo problemas temporales.\n\n" +
                    "Intenta nuevamente en unos minutos.";
        }

        // Error genérico pero con contexto
        return "No pudimos procesar tu canje.\n\n" +
                "Verifica tu conexión e intenta nuevamente.\n" +
                "Si el problema persiste, contacta con soporte.";
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