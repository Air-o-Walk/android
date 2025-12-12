package com.example.air_o_walk_sprint0;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

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
// --------------------------------------------------------------
// AirQualitySummaryActivity.java
// Autor: Meryame Ait Boumlik
// Descripción: Pantalla que muestra el resumen de calidad del aire paraun usuario concreto.
//      - Emoji de calidad del aire
//      - Tiempo activo
//      - Distancia recorrida
//      - Puntos obtenidos
//      - Resumen textual
//      - Gráfica de indice normalizado en las últimas 8 horas
// --------------------------------------------------------------
public class AirQualitySummaryActivity extends AppCompatActivity {

    private static final String TAG = "AirQualitySummary";

    private ImageView emojiQuality;
    private TextView textTiempo;
    private TextView textDistancia;
    private TextView textPuntos;
    private TextView textResumen;
    private int idUsuario;
    // --------------------------------------------------------------
    // onCreate()
    // Descripción: Inicializa la UI, recupera el USER_ID recibido desde la Activity anterior y lanza la petición al backend.
    // Diseño: UI + USER_ID -> obtenerResumen -> actualizar pantalla
    // Parámetros:
    //      - savedInstanceState : estado previo (Android)
    // --------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_air_quality_summary);

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
            }
        });

    }
    // --------------------------------------------------------------
    // dibujarGrafica()
    // Descripción: // gráfica del índice normalizado (0–1) con líneas de umbrales (buena / regular / mala)
    // Diseño:
    // LineChart chart, AirQualityData data → dibujarGrafica() → gráfica del índice normalizado y eje X con horas
    // Parámetros:- chart : el LineChart de la UI
    //            - data  : datos recibidos del backend
    //
    // --------------------------------------------------------------
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
        }
    }
}
