package com.example.air_o_walk_sprint0;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class AirQualitySummaryActivity extends AppCompatActivity {

    private static final String TAG = "AirQualitySummary";

    private ImageView emojiQuality;
    private TextView textTiempo;
    private TextView textDistancia;
    private TextView textPuntos;
    private TextView textResumen;

    private int idUsuario;

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
    private void dibujarGrafica(LineChart chart, AirQualityResumen.AirQualityData data) {

        try {
            List<Entry> o3Entries = new ArrayList<>();
            List<Entry> no2Entries = new ArrayList<>();
            List<Entry> co2Entries = new ArrayList<>();

            for (int i = 0; i < data.timestamps.length(); i++) {
                float x = i; // simple index

                o3Entries.add(new Entry(x, (float)data.o3.getDouble(i)));
                no2Entries.add(new Entry(x, (float)data.no2.getDouble(i)));
                co2Entries.add(new Entry(x, (float)data.co2.getDouble(i)));
            }

            LineDataSet setO3 = new LineDataSet(o3Entries, "O₃");
            LineDataSet setNO2 = new LineDataSet(no2Entries, "NO₂");
            LineDataSet setCO2 = new LineDataSet(co2Entries, "CO₂");

            setO3.setCircleRadius(3f);
            setNO2.setCircleRadius(3f);
            setCO2.setCircleRadius(3f);

            // colors (auto)
            setO3.setColor(Color.BLUE);
            setNO2.setColor(Color.RED);
            setCO2.setColor(Color.GREEN);

            LineData lineData = new LineData(setO3, setNO2, setCO2);

            chart.setData(lineData);
            chart.invalidate(); // refresh

        } catch (Exception e) {
            Log.e("AirQualitySummary", "Error dibujando gráfica", e);
        }
    }

}
