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
//      - Gráfica de O3 / NO2 / CO en las últimas 8 horas
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
    // Descripción: Construye y configura una gráfica LineChart con MPAndroidChart
    //               usando los arrays O3 / NO2 / CO y las etiquetas temporales.
    // Diseño:
    // LineChart chart, AirQualityData data → dibujarGrafica() → gráfico renderizado con O₃, NO₂, CO y eje X con horas
    // Parámetros:- chart : el LineChart de la UI
    //            - data  : datos recibidos del backend
    //
    // --------------------------------------------------------------
    private void dibujarGrafica(LineChart chart, AirQualityResumen.AirQualityData data) {

        try {
            List<Entry> o3Entries = new ArrayList<>();
            List<Entry> no2Entries = new ArrayList<>();
            List<Entry> coEntries = new ArrayList<>();
            List<String> etiquetasX = new ArrayList<>();

            // Construcción de entradas
            for (int i = 0; i < data.timestamps.length(); i++) {
                float x = i;

                o3Entries.add(new Entry(x, (float) data.o3.getDouble(i)));
                no2Entries.add(new Entry(x, (float) data.no2.getDouble(i)));
                coEntries.add(new Entry(x, (float) data.co.getDouble(i)));

                // Etiqueta real de tiempo (ej: "14:30")
                etiquetasX.add(data.timestamps.getString(i));
            }

            // Crear DataSets
            LineDataSet setO3 = new LineDataSet(o3Entries, "O₃ (µg/m³)");
            LineDataSet setNO2 = new LineDataSet(no2Entries, "NO₂ (µg/m³)");
            LineDataSet setCO = new LineDataSet(coEntries, "CO (ppm)");

            setO3.setColor(Color.BLUE);
            setNO2.setColor(Color.RED);
            setCO.setColor(Color.GREEN);

            setO3.setCircleRadius(3f);
            setNO2.setCircleRadius(3f);
            setCO.setCircleRadius(3f);

            LineData lineData = new LineData(setO3, setNO2, setCO);
            chart.setData(lineData);

            // ======== CONFIGURAR EJE X ========
            XAxis xAxis = chart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setGranularityEnabled(true);

            // Aplica las etiquetas reales de tiempo
            xAxis.setValueFormatter(new IndexAxisValueFormatter(etiquetasX));

            Description desc = new Description();
            desc.setText("Tiempo (últimas 8 horas)");
            desc.setTextSize(9f);
            chart.setDescription(desc);


            // Refresh
            chart.invalidate();

        } catch (Exception e) {
            Log.e("AirQualitySummary", "Error dibujando gráfica", e);
        }
    }

}
