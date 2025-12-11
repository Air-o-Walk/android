package com.example.airowalkmenu;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.navigation.NavigationView;
import com.example.airowalkmenu.databinding.ActivityMainBinding;
import com.example.airowalkmenu.auth.LoginActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    private int userId;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Verificar autenticación PRIMERO
        if (!verificarSesion()) {
            return; // Ya redirigió a LoginActivity
        }

        // 2. Setup UI
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        // 3. Setup Navigation Drawer
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        // IDs de los fragments principales (top-level destinations)
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_recorrido,    // RecorridoFragment
                R.id.nav_mapa,         // MapaFragment
                R.id.nav_recompensas,  // RecompensasFragment
                R.id.nav_perfil          // PerfilFragment
        )
                .setOpenableLayout(drawer)
                .build();

        // 4. Setup NavController
        NavController navController = Navigation.findNavController(this,
                R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        Log.d(TAG, "MainActivity inicializado - UserId: " + userId);
    }

    /**
     * Verifica si hay sesión activa
     * Si no hay sesión, redirige a LoginActivity
     * @return true si hay sesión válida
     */
    private boolean verificarSesion() {
        Intent intent = getIntent();
        userId = intent.getIntExtra("USER_ID", -1);
        token = intent.getStringExtra("TOKEN");

        if (userId == -1 || token == null || token.isEmpty()) {
            Log.w(TAG, "No hay sesión válida, redirigiendo a Login");
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return false;
        }

        return true;
    }

    /**
     * Obtiene el USER_ID para que los fragments puedan accederlo
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Obtiene el TOKEN para que los fragments puedan accederlo
     */
    public String getToken() {
        return token;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this,
                R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}