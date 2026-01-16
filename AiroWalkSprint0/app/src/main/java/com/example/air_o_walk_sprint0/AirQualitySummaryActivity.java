package com.example.air_o_walk_sprint0;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;
/**
 * @class AirQualitySummaryActivity
 * @brief Pantalla que muestra el resumen de calidad del aire de un usuario.
 *
 * Esta actividad presenta al usuario un resumen visual y textual de su
 * exposición a la calidad del aire, incluyendo:
 * - Emoji representativo del estado de la calidad del aire
 * - Tiempo activo
 * - Distancia recorrida
 * - Puntos obtenidos
 * - Resumen textual
 * - Gráfica del índice de calidad del aire normalizado en las últimas 8 horas
 *
 * Los datos se obtienen del backend de forma asíncrona.
 *
 * @author Meryame Ait Boumlik y Adenor Buret
 * @version 1.0
 */
public class AirQualitySummaryActivity extends BaseActivity {

    private static final String TAG = "AirQualitySummary";

    private ImageView emojiQuality;
    private TextView textTiempo;
    private TextView textDistancia;
    private TextView textPuntos;
    private TextView textResumen;
    private Button botonRecompensas;
    private int idUsuario;
    /**
     * onCreate()
     *
     * Descripción: Inicializa la UI, recupera el USER_ID recibido desde la
     * Activity anterior y lanza la petición al backend.
     *
     * @details
     * Diseño:
     * UI + USER_ID -> obtenerResumen -> actualizar pantalla
     *
     * @param savedInstanceState estado previo (Android)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ⭐ VERIFICAR SESIÓN ACTIVA
        if (!verificarSesionActiva()) {
            return;
        }

        setContentView(R.layout.activity_air_quality_summary);

        // Obtener userId del Intent o SharedPreferences
        userId = getIntent().getIntExtra("USER_ID", -1);

        if (userId == -1) {
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            userId = prefs.getInt("user_id", -1);
        }

        if (userId == -1) {
            Toast.makeText(this,
                    "No pudimos identificar tu cuenta.\n\nVuelve a iniciar sesión para ver tu resumen.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        setupHeaderAndDrawer(true);   // true = has drawer
        setupBackBehavior();

        Log.d(TAG, "onCreate(): Iniciando pantalla.");

        // ---------------------------
        // RECUPERAR USER ID
        // ---------------------------
        idUsuario = getIntent().getIntExtra("USER_ID", -1);
        Log.d(TAG, "USER_ID = " + idUsuario);

        // ---------------------------
        // REFERENCIAS UI
        // ---------------------------
        emojiQuality  = findViewById(R.id.emojiQuality);
        textTiempo    = findViewById(R.id.textTiempo);
        textDistancia = findViewById(R.id.textDistancia);
        textPuntos    = findViewById(R.id.textPuntos);
        textResumen = findViewById(R.id.textResumen);
        botonRecompensas = findViewById(R.id.botonRecompensas);

        // ---------------------------
        // CONFIGURAR BOTÓN RECOMPENSAS
        // ---------------------------
        botonRecompensas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirPantallaCanjeos(v);
            }
        });

        // ---------------------------
        // LLAMADA AL BACKEND
        // ---------------------------
        AirQualityResumen resumen = new AirQualityResumen(idUsuario);

        resumen.obtenerResumen(new AirQualityResumen.Listener() {

            @Override
            public void onResultado(AirQualityResumen.AirQualityData data) {

                runOnUiThread(() -> {

                    // ------ EMOJI ------
                    switch (data.status) {
                        case "buena":
                            emojiQuality.setImageResource(R.drawable.ic_air_good);
                            break;
                        case "regular":
                            emojiQuality.setImageResource(R.drawable.ic_air_regular);
                            break;
                        case "picos":
                            emojiQuality.setImageResource(R.drawable.ic_air_spikes);
                            break;
                        default:
                            emojiQuality.setImageResource(R.drawable.ic_air_bad);
                    }

                    // ------ TIEMPO ------
                    String tiempoStr = String.format("%.2f h", data.timeHours);
                    textTiempo.setText(tiempoStr);

                    // ------ DISTANCIA ------
                    String distStr = String.format("%.2f Km", data.distanceKm);
                    textDistancia.setText(distStr);

                    // ------ PUNTOS ------
                    textPuntos.setText(String.valueOf(data.points));

                    // ------ GRAFICA ------
                    LineChart chart = findViewById(R.id.airQualityChart);
                    dibujarGrafica(chart, data);


                    // ------ RESUMEN (opcional) ------
                    if (textResumen != null) textResumen.setText(data.summaryText);

                });
            }

            @Override
            public void onError(String error) {
                Log.e("AirQuality", "Error: " + error);

                runOnUiThread(() -> {
                    String mensajeAmigable = interpretarErrorResumen(error);

                    // Mostrar diálogo en vez de Toast para errores críticos
                    new AlertDialog.Builder(AirQualitySummaryActivity.this)
                            .setTitle("No pudimos cargar tu resumen")
                            .setMessage(mensajeAmigable)
                            .setPositiveButton("Reintentar", (d, w) -> {
                                // Volver a cargar datos
                                recreate();
                            })
                            .setNegativeButton("Volver", (d, w) -> {
                                finish();
                            })
                            .show();
                });
            }

            private String interpretarErrorResumen(String error) {
                if (error == null || error.isEmpty()) {
                    return "Ocurrió un problema al cargar tus datos.\n\n" +
                            "Verifica tu conexión a internet e intenta nuevamente.";
                }

                if (error.contains("timeout") || error.contains("connection")) {
                    return "No pudimos conectar con el servidor.\n\n" +
                            "Verifica tu conexión a internet e intenta nuevamente.";
                }

                if (error.contains("404") || error.contains("not found")) {
                    return "No encontramos datos de recorridos para tu cuenta.\n\n" +
                            "Realiza tu primer recorrido para ver tu resumen.";
                }

                if (error.contains("401") || error.contains("unauthorized")) {
                    return "Tu sesión ha expirado.\n\n" +
                            "Cierra sesión y vuelve a iniciar sesión.";
                }

                if (error.contains("500") || error.contains("server")) {
                    return "El servidor está teniendo problemas temporales.\n\n" +
                            "Intenta nuevamente en unos minutos.";
                }

                return "No pudimos cargar tu resumen de calidad del aire.\n\n" +
                        "Intenta nuevamente o contacta con soporte si el problema persiste.";
            }
        });

    }

    /**
     * abrirPantallaCanjeos()
     *
     * Descripción: Abre la pantalla de recompensas/canjeos
     *
     * @param view Vista del botón
     */
    public void abrirPantallaCanjeos(View view) {
        Intent intent = new Intent(this, CanjeoActivity.class);
        intent.putExtra("USER_ID", idUsuario);
        startActivity(intent);
    }

    /**
     * dibujarGrafica()
     *
     * Descripción: Gráfica del índice normalizado (0–1) con líneas de
     * umbrales (buena / regular / mala).
     *
     * @details
     * Diseño:
     * LineChart chart, AirQualityData data
     * → dibujarGrafica()
     * → gráfica del índice normalizado y eje X con horas
     *
     * @param chart LineChart de la interfaz
     * @param data datos recibidos del backend
     */
    private void dibujarGrafica(LineChart chart, AirQualityResumen.AirQualityData data) {

        try {
            List<Entry> indexEntries = new ArrayList<>();
            List<String> etiquetasX = new ArrayList<>();

            // 1. Construcción de puntos del índice normalizado
            for (int i = 0; i < data.timestamps.length(); i++) {
                float x = i;
                float idx = (float) data.index.getDouble(i);

                indexEntries.add(new Entry(x, idx));
                etiquetasX.add(data.timestamps.getString(i));
            }

            // 2. Dataset de la línea INDEX
            LineDataSet setIndex = new LineDataSet(indexEntries, "Índice (0–1)");
            setIndex.setColor(Color.BLUE);
            setIndex.setLineWidth(2.5f);
            setIndex.setCircleRadius(3f);
            setIndex.setDrawValues(false);

            // ---------- 3. Horizontal threshold lines ----------
            List<Entry> buena = new ArrayList<>();
            List<Entry> regular = new ArrayList<>();
            List<Entry> mala = new ArrayList<>();

            float maxX = data.timestamps.length() - 1;

            buena.add(new Entry(0, 0.3f));
            buena.add(new Entry(maxX, 0.3f));

            regular.add(new Entry(0, 0.5f));
            regular.add(new Entry(maxX, 0.5f));

            mala.add(new Entry(0, 0.8f));
            mala.add(new Entry(maxX, 0.8f));

            LineDataSet setBuena = new LineDataSet(buena, "Buena (<0.3)");
            LineDataSet setRegular = new LineDataSet(regular, "Regular (<0.5)");
            LineDataSet setMala = new LineDataSet(mala, "Mala (>0.8)");

            setBuena.setColor(Color.GREEN);
            setRegular.setColor(Color.YELLOW);
            setMala.setColor(Color.RED);

            setBuena.setDrawCircles(false);
            setRegular.setDrawCircles(false);
            setMala.setDrawCircles(false);

            setBuena.setLineWidth(1.5f);
            setRegular.setLineWidth(1.5f);
            setMala.setLineWidth(1.5f);

            // ---------- 4. Add EVERYTHING into chart ----------
            LineData lineData = new LineData(setIndex, setBuena, setRegular, setMala);
            chart.setData(lineData);

            // 5. Eje X con horas
            XAxis xAxis = chart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setValueFormatter(new IndexAxisValueFormatter(etiquetasX));

            // Description text
            Description desc = new Description();
            desc.setText("Índice normalizado (8h)");
            desc.setTextSize(9f);
            chart.setDescription(desc);

            chart.invalidate();

        } catch (Exception e) {
            Log.e("AirQualitySummary", "Error dibujando gráfica", e);

            // Mostrar mensaje al usuario
            runOnUiThread(() -> {
                chart.setVisibility(View.GONE);

                Toast.makeText(this,
                        "No pudimos generar la gráfica de calidad del aire.\n\n" +
                                "Los datos numéricos siguen disponibles arriba.",
                        Toast.LENGTH_LONG).show();
            });
        }
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