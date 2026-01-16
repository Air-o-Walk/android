package com.example.air_o_walk_sprint0;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @class LoginActivity
 * @brief Pantalla de inicio de sesión de la aplicación.
 *
 * Esta actividad implementa la interfaz gráfica del login y actúa
 * como puente entre la interfaz de usuario y la lógica de negocio.
 * Gestiona:
 * - Los campos del formulario de usuario y contraseña
 * - La validación de los datos introducidos
 * - La llamada a la clase LogicaLogin para autenticar al usuario
 * - La persistencia de la sesión del usuario
 * - La verificación automática de sesión activa
 *
 * Además, gestiona la persistencia de la sesión mediante
 * SharedPreferences y redirige al usuario a la pantalla principal
 * cuando el login es exitoso.
 *
 * @author María Algora
 * @version 2.0
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final String PREFS_NAME = "app_prefs"; // ⭐ IMPORTANTE: Usar el mismo nombre en toda la app

    private EditText editTextUsuario, editTextContrasena;
    private Button buttonLogin;
    private SharedPreferences prefs;

    /**
     * Inicializa los elementos del layout y configura el listener del botón de login.
     * Verifica si ya existe una sesión activa antes de mostrar el formulario.
     * @param savedInstanceState Estado previo de la actividad, si lo hubiera
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ⭐ IMPORTANTE: Usar "app_prefs" en toda la aplicación para consistencia
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // ========================================================================
        // VERIFICAR SESIÓN ACTIVA ANTES DE MOSTRAR EL LOGIN
        // Si ya hay una sesión guardada, redirigir directamente a MainActivity
        // ========================================================================
        if (verificarSesionActiva()) {
            Log.d(TAG, "Sesión activa detectada - Redirigiendo a MainActivity");
            navigateToMainActivity();
            return; // Detener ejecución para no mostrar el layout
        }

        // Si no hay sesión activa, mostrar el formulario de login
        setContentView(R.layout.login_activity);

        // Inicializar vistas
        editTextUsuario = findViewById(R.id.editTextUsuario);
        editTextContrasena = findViewById(R.id.editTextContrasena);
        buttonLogin = findViewById(R.id.buttonLogin);
        TextView forgotPassword = findViewById(R.id.textForgotPassword);
        MaterialButton buttonRegister = findViewById(R.id.buttonRegister);

        // Configurar listeners
        forgotPassword.setOnClickListener(v -> showForgotPasswordPopup());

        buttonRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegistroActivity.class);
            startActivity(intent);
        });

        buttonLogin.setOnClickListener(v -> realizarLogin());
    }

    /**
     * Verifica si existe una sesión activa válida en SharedPreferences.
     * Comprueba que tanto el userId como el token estén presentes.
     *
     * @return true si hay sesión activa, false en caso contrario
     */
    private boolean verificarSesionActiva() {
        int userId = prefs.getInt("user_id", -1);
        String token = prefs.getString("token", null);
        boolean sesionActiva = prefs.getBoolean("sesion_activa", false);

        // Validar que todos los datos necesarios estén presentes
        boolean hayDatosCompletos = (userId != -1 && token != null && !token.isEmpty());

        Log.d(TAG, "Verificación de sesión:");
        Log.d(TAG, "  - userId: " + userId);
        Log.d(TAG, "  - token presente: " + (token != null && !token.isEmpty()));
        Log.d(TAG, "  - sesion_activa: " + sesionActiva);
        Log.d(TAG, "  - Sesión válida: " + (hayDatosCompletos && sesionActiva));

        return hayDatosCompletos && sesionActiva;
    }

    /**
     * Obtiene los valores introducidos por el usuario, valida los campos
     * y realiza la autenticación usando la clase LogicaLogin.
     * Guarda la sesión en SharedPreferences si el login es exitoso.
     */
    private void realizarLogin() {
        String usuario = editTextUsuario.getText().toString().trim();
        String contrasena = editTextContrasena.getText().toString().trim();

        // Validación de campos vacíos
        if (usuario.isEmpty() || contrasena.isEmpty()) {
            Toast.makeText(this,
                    "Debes completar todos los campos.\n\nIngresa tu usuario y contraseña para continuar.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Iniciando proceso de login para usuario: " + usuario);

        // Crear instancia de lógica de login
        LogicaLogin logicaLogin = new LogicaLogin(usuario, contrasena);

        // Realizar petición de login
        logicaLogin.realizarLogin(new LogicaLogin.LoginCallback() {
            @Override
            public void onLoginExitoso(String respuesta) {
                runOnUiThread(() -> {
                    try {
                        // Parsear respuesta JSON del servidor
                        JSONObject jsonObject = new JSONObject(respuesta);
                        int userId = jsonObject.getInt("userId");
                        String token = jsonObject.getString("token");

                        Log.d(TAG, "Login exitoso:");
                        Log.d(TAG, "  - User ID: " + userId);
                        Log.d(TAG, "  - Token recibido: " + (token != null && !token.isEmpty()));

                        // ========================================================
                        // GUARDAR SESIÓN EN SharedPreferences
                        // ========================================================
                        guardarSesionUsuario(userId, token);

                        // Mostrar mensaje de éxito
                        Toast.makeText(LoginActivity.this,
                                "¡Bienvenido! Sesión iniciada correctamente",
                                Toast.LENGTH_SHORT).show();

                        // Navegar a MainActivity
                        navigateToMainActivity();

                    } catch (JSONException e) {
                        Log.e(TAG, "Error al procesar respuesta JSON", e);
                        Toast.makeText(LoginActivity.this,
                                "Hubo un problema al procesar la respuesta del servidor.\n\nIntenta nuevamente o contacta con soporte.",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onLoginFallido(String mensajeError) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Login fallido: " + mensajeError);

                    String mensajeAmigable = interpretarErrorLogin(mensajeError);

                    new AlertDialog.Builder(LoginActivity.this)
                            .setTitle("No pudimos iniciar sesión")
                            .setMessage(mensajeAmigable)
                            .setPositiveButton("Reintentar", null)
                            .setNeutralButton("¿Olvidaste tu contraseña?", (d, w) -> {
                                showForgotPasswordPopup();
                            })
                            .show();
                });
            }

            private String interpretarErrorLogin(String errorBackend) {
                if (errorBackend == null || errorBackend.isEmpty()) {
                    return "Ocurrió un problema inesperado.\n\nVerifica tu conexión a internet e intenta nuevamente.";
                }

                // Credenciales incorrectas
                if (errorBackend.contains("401") ||
                        errorBackend.contains("unauthorized") ||
                        errorBackend.contains("invalid") ||
                        errorBackend.contains("incorrect")) {
                    return "Usuario o contraseña incorrectos.\n\n" +
                            "Verifica tus datos e intenta nuevamente.\n" +
                            "¿Olvidaste tu contraseña? Usa la opción de recuperación.";
                }

                // Usuario no encontrado
                if (errorBackend.contains("404") ||
                        errorBackend.contains("not found") ||
                        errorBackend.contains("no existe")) {
                    return "Este usuario no está registrado.\n\n" +
                            "Verifica tu nombre de usuario o regístrate si es tu primera vez.";
                }

                // Error de conexión
                if (errorBackend.contains("timeout") ||
                        errorBackend.contains("connection") ||
                        errorBackend.contains("network")) {
                    return "No pudimos conectar con el servidor.\n\n" +
                            "Verifica tu conexión a internet e intenta nuevamente.";
                }

                // Servidor caído
                if (errorBackend.contains("500") ||
                        errorBackend.contains("server error")) {
                    return "El servidor no está disponible en este momento.\n\n" +
                            "Intenta nuevamente en unos minutos.";
                }

                // Cuenta bloqueada/suspendida
                if (errorBackend.contains("blocked") ||
                        errorBackend.contains("suspended") ||
                        errorBackend.contains("bloqueado")) {
                    return "Tu cuenta ha sido suspendida.\n\n" +
                            "Contacta con soporte para más información.";
                }

                // Error genérico
                return "No pudimos iniciar tu sesión.\n\n" +
                        "Verifica tus datos de acceso e intenta nuevamente.\n" +
                        "Si el problema persiste, contacta con soporte.";
            }
        });
    }

    /**
     * Guarda los datos de sesión del usuario en SharedPreferences.
     * Esta información se utilizará para:
     * - Mantener la sesión activa entre reinicios de la app
     * - Validar el acceso a pantallas protegidas
     * - Realizar peticiones autenticadas al backend
     *
     * @param userId ID único del usuario autenticado
     * @param token Token de autenticación JWT del servidor
     */
    private void guardarSesionUsuario(int userId, String token) {
        SharedPreferences.Editor editor = prefs.edit();

        // Guardar datos de sesión
        editor.putInt("user_id", userId);      // ID del usuario
        editor.putString("token", token);       // Token de autenticación
        editor.putBoolean("sesion_activa", true); // Flag de sesión activa

        // También guardar con nombres legacy por compatibilidad
        editor.putInt("userId", userId);        // Compatibilidad con código antiguo
        editor.putBoolean("isLoggedIn", true);  // Compatibilidad con código antiguo

        // Aplicar cambios inmediatamente
        editor.apply();

        Log.d(TAG, "Sesión guardada en SharedPreferences:");
        Log.d(TAG, "  - user_id: " + userId);
        Log.d(TAG, "  - token guardado: ✓");
        Log.d(TAG, "  - sesion_activa: true");
    }

    /**
     * Navega a la pantalla principal de la aplicación.
     * Pasa el userId y token como extras del Intent y finaliza LoginActivity.
     * Usa flags especiales para limpiar el stack de actividades previas.
     */
    private void navigateToMainActivity() {
        // Obtener datos de sesión
        int userId = prefs.getInt("user_id", -1);
        String token = prefs.getString("token", "");

        if (userId == -1 || token.isEmpty()) {
            Log.e(TAG, "Error: Datos de sesión incompletos al navegar");
            Toast.makeText(this,
                    "Ocurrió un error al cargar tu sesión.\n\nVuelve a iniciar sesión.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear intent para MainActivity
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);

        // Pasar datos de sesión como extras
        intent.putExtra("USER_ID", userId);
        intent.putExtra("TOKEN", token);

        // ⭐ IMPORTANTE: Limpiar el stack de actividades
        // Previene que el usuario use "atrás" para volver al login
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        Log.d(TAG, "Navegando a MainActivity con userId: " + userId);

        // Iniciar MainActivity
        startActivity(intent);

        // Finalizar LoginActivity
        finish();
    }

    /**
     * Muestra un diálogo emergente para recuperar la contraseña olvidada.
     * Permite al usuario introducir su email para recibir instrucciones.
     */
    private void showForgotPasswordPopup() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_forgot_password);

        // Configurar el diálogo
        dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setDimAmount(0.5f); // Fondo oscuro
        dialog.setCancelable(true);

        // Obtener vistas del diálogo
        EditText emailField = dialog.findViewById(R.id.editTextPopupEmail);
        MaterialButton recoverButton = dialog.findViewById(R.id.buttonPopupRecover);
        MaterialButton loginButton = dialog.findViewById(R.id.buttonPopupLogin);

        // Configurar botón de recuperación
        recoverButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, "Por favor, introduce tu correo electrónico",
                        Toast.LENGTH_SHORT).show();
            } else {
                // TODO: Implementar llamada al backend para recuperación de contraseña
                // Por ahora, solo simular el envío
                Log.d(TAG, "Solicitud de recuperación de contraseña para: " + email);
                Toast.makeText(this,
                        "Se han enviado instrucciones a tu correo",
                        Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        // Configurar botón de volver al login
        loginButton.setOnClickListener(v -> dialog.dismiss());

        // Mostrar el diálogo
        dialog.show();
    }

    /**
     * Se ejecuta cuando la actividad vuelve a primer plano.
     * Limpia los campos de texto para evitar mostrar datos sensibles.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Limpiar campos al volver a la pantalla
        // (útil si el usuario hizo logout y vuelve)
        if (editTextUsuario != null) {
            editTextUsuario.setText("");
        }
        if (editTextContrasena != null) {
            editTextContrasena.setText("");
        }
    }

    /**
     * Previene que el usuario pueda volver atrás desde el login
     * usando el botón físico del dispositivo.
     */
    @Override
    public void onBackPressed() {
        // Si presionan "atrás" en login, cerrar la aplicación
        // en lugar de intentar volver a una actividad anterior
        finishAffinity(); // Cierra todas las actividades y sale de la app
    }
}