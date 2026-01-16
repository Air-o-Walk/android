package com.example.air_o_walk_sprint0;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Pantalla de notificaciones de incidencias
 */
public class NotificationsActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Header sin drawer
        setupHeaderAndDrawer(false);

        // Obtener userId desde sesión (LoginActivity)
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        userId = prefs.getInt("userId", 0);

        if (userId == 0) {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerView = findViewById(R.id.recyclerNotifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        cargarNotificaciones();
    }

    private void cargarNotificaciones() {

        LogicaNotifIncidencias logica =
                new LogicaNotifIncidencias(userId,
                        new LogicaNotifIncidencias.NotificacionesCallback() {

                            @Override
                            public void onNotificacionesRecibidas(List<NotificationIncidencia> lista) {
                                runOnUiThread(() -> {
                                    recyclerView.setAdapter(
                                            new NotificationsAdapter(lista)
                                    );
                                });
                            }


                            @Override
                            public void onError(String mensaje) {
                                runOnUiThread(() ->
                                        Toast.makeText(NotificationsActivity.this,
                                                mensaje,
                                                Toast.LENGTH_SHORT).show()
                                );
                            }
                        });

        logica.obtenerNotificaciones();
    }
}
