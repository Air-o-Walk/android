package com.example.air_o_walk_sprint0;

import android.content.Intent;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public abstract class BaseActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected ImageView btnMenu;
    protected ImageView iconoVincular;

    /**
     * Sets up header behavior and drawer/back button
     */
    protected void setupHeaderAndDrawer(boolean hasDrawer) {

        btnMenu = findViewById(R.id.btnMenu);
        iconoVincular = findViewById(R.id.iconoVincular);

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
        setupBackBehavior();
    }

    /**
     * Vinculación icon behavior
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
     * Handles system back button (drawer-aware)
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
}
