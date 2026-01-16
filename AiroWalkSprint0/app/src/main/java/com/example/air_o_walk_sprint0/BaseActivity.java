package com.example.air_o_walk_sprint0;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
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
 * - Verificación de sesión activa
 *
 * Las actividades que heredan de esta clase pueden activar o no
 * el menú lateral según sus necesidades.
 *
 * @author Meryame Ait Boumlik
 * @version 2.0
 */
public abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity";
    private static final String PREFS_NAME = "app_prefs";

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected ImageView btnMenu;
    protected ImageView iconoVincular;
    protected ImageView btnNotif;

    // Variables protegidas para que las actividades hijas puedan acceder
    protected int userId = -1;
    protected String userToken = null;

    /**
     * Verifica si existe una sesión activa válida.
     * Si no hay sesión, redirige automáticamente a LoginActivity.
     *
     * @return true si hay sesión activa, false si no hay sesión (y redirige a Login)
     */
    protected boolean verificarSesionActivaBase() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedUserId = prefs.getInt("user_id", -1);
        String savedToken = prefs.getString("token", null);
        boolean sesionActiva = prefs.getBoolean("sesion_activa", false);

        // Validar que todos los datos necesarios estén presentes
        boolean hayDatosCompletos = (savedUserId != -1 &&
                savedToken != null &&
                !savedToken.isEmpty());

        Log.d(TAG, "Verificación de sesión en " + getClass().getSimpleName() + ":");
        Log.d(TAG, "  - userId: " + savedUserId);
        Log.d(TAG, "  - token presente: " + (savedToken != null && !savedToken.isEmpty()));
        Log.d(TAG, "  - sesion_activa: " + sesionActiva);
        Log.d(TAG, "  - Sesión válida: " + (hayDatosCompletos && sesionActiva));

        if (!hayDatosCompletos || !sesionActiva) {
            // No hay sesión activa - redirigir a login
            Log.w(TAG, "Sesión inválida o expirada - Redirigiendo a LoginActivity");

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

            return false;
        }

        // Guardar userId y token para uso de las actividades hijas
        userId = savedUserId;
        userToken = savedToken;

        return true;
    }

    /**
     * Carga los datos de sesión desde SharedPreferences.
     * Las actividades hijas pueden llamar a este método para obtener userId y token.
     */
    protected void cargarDatosSesion() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
        userToken = prefs.getString("token", null);

        Log.d(TAG, "Datos de sesión cargados - userId: " + userId);
    }

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

                    int itemId = item.getItemId();

                    if (itemId == R.id.nav_perfil) {
                        // Navegar a PerfilActivity
                        Intent intent = new Intent(this, PerfilActivity.class);
                        intent.putExtra("USER_ID", userId);
                        intent.putExtra("TOKEN", userToken);
                        startActivity(intent);

                    } else if (itemId == R.id.nav_recorrido) {
                        // Navegar a AirQualitySummaryActivity
                        Intent intent = new Intent(this, AirQualitySummaryActivity.class);
                        intent.putExtra("USER_ID", userId);
                        startActivity(intent);

                    } else if (itemId == R.id.nav_recompensa) {
                        //abrirPantallaGamificacion(null);
                        Intent intent = new Intent(this, CanjeoActivity.class);
                         intent.putExtra("USER_ID", userId);
                         startActivity(intent);

                    } else if (itemId == R.id.nav_mapa) {
                        // Navegar a MainActivity (Home)
                        if (!(this instanceof MainActivity)) {
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.putExtra("USER_ID", userId);
                            intent.putExtra("TOKEN", userToken);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this,
                                    "Ya estás en la pantalla principal",
                                    Toast.LENGTH_SHORT).show();
                        }

                    } else if (itemId == R.id.nav_notificaciones) {
                        // Mostrar información de notificaciones
                        Toast.makeText(this,
                                "Gestiona tus notificaciones desde Configuración",
                                Toast.LENGTH_SHORT).show();
                        // TODO: Implementar NotificacionesActivity

                    } else if (itemId == R.id.nav_info) {
                        // Mostrar información sobre gases
                        new AlertDialog.Builder(this)
                                .setTitle("Información sobre Calidad del Aire")
                                .setMessage(
                                        "¿QUÉ ES EL AQI?\n" +
                                                "El Índice de Calidad del Aire (AQI) mide qué tan limpio o contaminado está el aire. " +
                                                "Va de 0 (excelente) a 500 (peligroso).\n\n" +

                                                "━━━━━━━━━━━━━━━━━━━━━━\n\n" +

                                                "OZONO TROPOSFÉRICO (O₃)\n\n" +

                                                "¿Qué es?\n" +
                                                "Se forma cuando la luz solar reacciona con gases de vehículos e industrias. " +
                                                "Es más común en verano y días soleados.\n\n" +

                                                "✅ Nivel seguro: AQI 0-50 (Bueno)\n" +
                                                "⚠️ Nivel alto: AQI 51-100 (Moderado)\n" +
                                                "🚨 Nivel peligroso: AQI 101-150+ (Insalubre para grupos sensibles o superior)\n\n" +

                                                "Efectos en la salud:\n" +
                                                "Irritación de ojos y garganta, tos, dificultad para respirar, empeora el asma.\n\n" +

                                                "Consejo: Evita ejercicio intenso entre 14:00-20:00 en días soleados.\n\n" +

                                                "━━━━━━━━━━━━━━━━━━━━━━\n\n" +

                                                "MONÓXIDO DE CARBONO (CO)\n\n" +

                                                "¿Qué es?\n" +
                                                "Gas tóxico sin olor ni color producido por combustión incompleta en vehículos, " +
                                                "estufas y calderas mal ventiladas.\n\n" +

                                                "✅ Nivel seguro: AQI 0-50 (Bueno)\n" +
                                                "⚠️ Nivel alto: AQI 51-100 (Moderado)\n" +
                                                "🚨 Nivel peligroso: AQI 101+ (Insalubre para grupos sensibles o superior)\n\n" +

                                                "Efectos en la salud:\n" +
                                                "Impide que tu sangre transporte oxígeno. Causa dolor de cabeza, mareos, náuseas " +
                                                "y en casos graves, pérdida de conciencia.\n\n" +

                                                "Consejo: No hagas ejercicio cerca de carreteras con mucho tráfico.\n\n" +

                                                "━━━━━━━━━━━━━━━━━━━━━━\n\n" +

                                                "DIÓXIDO DE NITRÓGENO (NO₂)\n\n" +

                                                "¿Qué es?\n" +
                                                "Proviene principalmente de vehículos diésel, calefacciones de gas y centrales térmicas.\n\n" +

                                                "✅ Nivel seguro: AQI 0-50 (Bueno)\n" +
                                                "⚠️ Nivel alto: AQI 51-100 (Moderado)\n" +
                                                "🚨 Nivel peligroso: AQI 101+ (Insalubre para grupos sensibles o superior)\n\n" +

                                                "Efectos en la salud:\n" +
                                                "Irrita las vías respiratorias, empeora el asma, aumenta alergias y reduce la función pulmonar.\n\n" +

                                                "Consejo: Evita avenidas con mucho tráfico y ventila tu casa en horas con menos coches.\n\n" +

                                                "━━━━━━━━━━━━━━━━━━━━━━\n\n" +

                                                "💡 TIP: Usa esta app para conocer los niveles en tiempo real durante tus recorridos."
                                )
                                .setPositiveButton("Entendido", null)
                                .show();

                    } else {
                        Toast.makeText(this,
                                "Pantalla aún no implementada",
                                Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this,
                        "La vinculación del sensor se gestiona desde la pantalla principal",
                        Toast.LENGTH_SHORT).show();
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

    /**
     * Cierra la sesión del usuario y redirige a LoginActivity.
     * Se usa cuando hay errores críticos de autenticación.
     */
    protected void cerrarSesionPorError() {
        Log.w(TAG, "Cerrando sesión por error de autenticación");

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        Toast.makeText(this,
                "Tu sesión ha expirado por seguridad.\n\nVuelve a iniciar sesión para continuar.",
                Toast.LENGTH_LONG).show();

        finish();
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