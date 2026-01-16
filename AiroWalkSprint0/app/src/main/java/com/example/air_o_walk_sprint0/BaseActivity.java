package com.example.air_o_walk_sprint0;

import android.content.Intent;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
/**
 * @class BaseActivity
 * @brief Actividad base común para las pantallas de la aplicación.
 *
 * Esta clase centraliza la lógica compartida entre las distintas
 * actividades de la aplicación, incluyendo:
 * - Configuración del header
 * - Gestión del menú lateral (drawer)
 * - Comportamiento del botón de retroceso
 * - Acceso al icono de vinculación del sensor
 *
 * Las actividades que heredan de esta clase pueden activar o no
 * el menú lateral según sus necesidades.
 *
 * @author Meryame Ait Boumlik
 * @version 1.0
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected ImageView btnMenu;
    protected ImageView iconoVincular;
    protected ImageView btnNotif;


    /**
     * Configura el comportamiento del header y del menú lateral.
     *
     * Permite habilitar o deshabilitar el drawer según la pantalla.
     * Si no hay drawer, el botón del header actúa como botón de retroceso.
     *
     * @param hasDrawer indica si la actividad dispone de menú lateral
     */
    protected void setupHeaderAndDrawer(boolean hasDrawer) {

        btnMenu = findViewById(R.id.btnMenu);
        iconoVincular = findViewById(R.id.iconoVincular);
        btnNotif = findViewById(R.id.btnNotif);

        if (hasDrawer) {
            drawerLayout = findViewById(R.id.drawerLayout);
            navigationView = findViewById(R.id.navigationView);

            if (btnMenu != null && drawerLayout != null) {
                btnMenu.setOnClickListener(v ->
                        drawerLayout.openDrawer(GravityCompat.START)
                );
            }

            if (navigationView != null) {
                navigationView.setNavigationItemSelectedListener(item -> {
                    drawerLayout.closeDrawer(GravityCompat.START);

                    if (item.getItemId() == R.id.nav_perfil) {

                        startActivity(new Intent(this, PerfilActivity.class));

                    } else if (item.getItemId() == R.id.nav_recorrido) {

                        startActivity(new Intent(this, AirQualitySummaryActivity.class));

                    } else {
                        Toast.makeText(
                                this,
                                "Pantalla aún no implementada",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    return true;
                });



        }

        } else {
            // No drawer → back button
            if (btnMenu != null) {
                btnMenu.setOnClickListener(v -> finish());
            }
        }

        setupIconoVincular();
        setupNotifications();
        setupBackBehavior();
    }

    /**
     * Configura el comportamiento del icono de vinculación del sensor.
     *
     * Si la actividad actual es la principal, delega la acción.
     * En caso contrario, muestra un mensaje informativo al usuario.
     */
    private void setupIconoVincular() {
        if (iconoVincular == null) return;

        iconoVincular.setOnClickListener(v -> {

            if (this instanceof MainActivity) {
                ((MainActivity) this).botonVincularPulsado(v);
            } else {
                Toast.makeText(
                        this,
                        "La vinculación del sensor se gestiona desde la pantalla principal",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    /**
     * Gestiona el comportamiento del botón físico de retroceso.
     *
     * Si el menú lateral está abierto, lo cierra.
     * En caso contrario, delega el comportamiento estándar del sistema.
     */
    protected void setupBackBehavior() {

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (drawerLayout != null &&
                                drawerLayout.isDrawerOpen(GravityCompat.START)) {
                            drawerLayout.closeDrawer(GravityCompat.START);
                        } else {
                            setEnabled(false);
                            BaseActivity.super.onBackPressed();
                        }
                    }
                });
    }
    //
    private void setupNotifications() {
        if (btnNotif == null) return;

        btnNotif.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            startActivity(intent);
        });
    }

}
