package com.example.air_o_walk_sprint0;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AirQualitySummaryActivity extends AppCompatActivity {

    private static final String TAG = "AirQualitySummary";

    private ImageView emojiQuality;
    private TextView textTiempo;
    private TextView textDistancia;
    private TextView textPuntos;
    private TextView textResumen;  // Optional, only if you add this in XML

    private int idUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_air_quality_summary);

        Log.d(TAG, "onCreate(): Iniciando pantalla.");

        // ---------------------------
        // RECOVER USER ID FROM INTENT
        // ---------------------------
        idUsuario = getIntent().getIntExtra("USER_ID", -1);
        Log.d(TAG, "USER_ID = " + idUsuario);

        // ---------------------------
        // UI REFERENCES
        // ---------------------------
        emojiQuality  = findViewById(R.id.emojiQuality);
        textTiempo    = findViewById(R.id.textTiempo);
        textDistancia = findViewById(R.id.textDistancia);
        textPuntos    = findViewById(R.id.textPuntos);

        // If you add summary text under emoji add this in XML:
        // textResumen   = findViewById(R.id.textResumen);

        // PLACEHOLDER: In future replace these with real trackers
        textTiempo.setText("10:25");
        textDistancia.setText("0.75 Km");

        // ---------------------------
        // CALL BACKEND SERVICE
        // ---------------------------
        AirQualityResumen resumen = new AirQualityResumen(idUsuario);

        resumen.obtenerResumen(new AirQualityResumen.Listener() {

            @Override
            public void onResultado(AirQualityResumen.AirQualityData data) {

                runOnUiThread(() -> {

                    // Emoji
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

                    // Points
                    textPuntos.setText(String.valueOf(data.points));

                    // OPTIONAL text summary
                    // textResumen.setText(data.summaryText);
                });
            }

            @Override
            public void onError(String error) {
                Log.e("AirQuality", "Error: " + error);
            }
        });

    }
}
