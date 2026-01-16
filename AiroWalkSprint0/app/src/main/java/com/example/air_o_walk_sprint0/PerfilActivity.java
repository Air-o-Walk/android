package com.example.air_o_walk_sprint0;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

/**
 * @class PerfilActivity
 * @brief Activity que gestiona la visualización y edición del perfil de usuario.
 *
 * Esta clase permite al usuario:
 * - Visualizar sus datos básicos (username, email, password)
 * - Editar su nombre de usuario
 * - Cambiar su correo electrónico con validación de formato
 * - Actualizar su contraseña con verificación de contraseña actual
 * - Acceder a secciones de quejas y política de privacidad
 * - Cerrar sesión de forma segura
 * - Eliminar su cuenta (próximamente)
 *
 * La comunicación con el backend se realiza mediante la clase LogicaEditarPerfil.
 *
 * @author Maria Algora
 * @version 2.0
 */
public class PerfilActivity extends BaseActivity {

    private static final String TAG = "PerfilActivity";
    private static final String PREFS_NAME = "app_prefs";

    private String token;
    private int userId;
    private LogicaEditarPerfil logicaEditar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ⭐ VERIFICAR SESIÓN ANTES DE CARGAR LA ACTIVIDAD
        if (!verificarSesionActivaBase()) {
            return; // Si no hay sesión, BaseActivity redirige a Login
        }

        setContentView(R.layout.activity_perfil);
        setupHeaderAndDrawer(true);
        setupBackBehavior();

        // Obtener token y userId del Intent o SharedPreferences
        userId = getIntent().getIntExtra("USER_ID", 0);
        token = getIntent().getStringExtra("TOKEN");

        // Si no vienen del Intent, cargar de SharedPreferences
        if (userId == 0 || token == null) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            userId = prefs.getInt("user_id", 0);
            token = prefs.getString("token", null);
        }

        // Validar que tenemos los datos necesarios
        if (userId == 0 || token == null) {
            Log.e(TAG, "Error: No se recibieron credenciales válidas");
            Toast.makeText(this,
                    "No pudimos cargar tus datos de sesión.\n\nCierra sesión y vuelve a iniciar sesión.",
                    Toast.LENGTH_LONG).show();
            cerrarSesionYRedirigir(); // Cerrar sesión y volver a Login
            return;
        }

        Log.d(TAG, "PerfilActivity iniciado - UserID: " + userId);

        // Inicializar lógica de edición de perfil
        logicaEditar = new LogicaEditarPerfil(token, userId);

        // Cargar datos del usuario y configurar botones
        cargarDatosUsuario();
        setupBotonesEdicion();
        setupLogoutButton(); // ⭐ NUEVO: Configurar botón de logout
    }

    /**
     * Carga los datos básicos del usuario desde el backend y actualiza la UI.
     */
    private void cargarDatosUsuario() {
        logicaEditar.obtenerDatosBasicosUsuario(new LogicaEditarPerfil.UsuarioBasicoCallback() {
            @Override
            public void onUsuarioObtenido(String username, String email) {
                runOnUiThread(() -> {
                    TextView tvUsername = findViewById(R.id.campo_username);
                    TextView tvEmail = findViewById(R.id.campo_email);
                    tvUsername.setText(username);
                    tvEmail.setText(email);

                    Log.d(TAG, "Datos de usuario cargados - Username: " + username);
                });
            }

            @Override
            public void onError(String mensajeError) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error al cargar datos: " + mensajeError);
                    Toast.makeText(PerfilActivity.this,
                            "No pudimos obtener tus datos de perfil.\n\nVerifica tu conexión a internet e intenta deslizar para actualizar.",
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Configura los listeners de los botones de edición de perfil.
     */
    private void setupBotonesEdicion() {
        // Botón editar username
        findViewById(R.id.editar_username).setOnClickListener(v -> {
            mostrarDialogoEdicion("username", "Nuevo nombre de usuario",
                    nuevoValor -> logicaEditar.actualizarUsername(nuevoValor,
                            new LogicaEditarPerfil.EditarCallback() {
                                @Override
                                public void onEdicionExitosa(String campo, String mensaje) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(PerfilActivity.this,
                                                "Nombre de usuario actualizado.\n\nTu nuevo nombre es: " + nuevoValor,
                                                Toast.LENGTH_SHORT).show();
                                        ((TextView) findViewById(R.id.campo_username)).setText(nuevoValor);
                                    });
                                }

                                @Override
                                public void onEdicionFallida(String campo, String mensajeError) {
                                    runOnUiThread(() ->
                                            Toast.makeText(PerfilActivity.this,
                                                    "No pudimos guardar tu nuevo nombre de usuario.\n\nVerifica tu conexión e intenta nuevamente.",
                                                    Toast.LENGTH_LONG).show());
                                }
                            })
            );
        });

        // Botón editar email
        findViewById(R.id.editar_email).setOnClickListener(v -> {
            mostrarDialogoEdicion("email", "Nuevo email",
                    nuevoValor -> {
                        if (!esEmailValido(nuevoValor)) {
                            runOnUiThread(() ->
                                    Toast.makeText(PerfilActivity.this,
                                            "El formato del correo no es válido.\n\nDebe tener formato: usuario@dominio.com",
                                            Toast.LENGTH_LONG).show());
                            return;
                        }

                        logicaEditar.actualizarEmail(nuevoValor, new LogicaEditarPerfil.EditarCallback() {
                            @Override
                            public void onEdicionExitosa(String campo, String mensaje) {
                                runOnUiThread(() -> {
                                    Toast.makeText(PerfilActivity.this,
                                            "Correo actualizado exitosamente.\n\nTu nuevo correo es: " + nuevoValor,
                                            Toast.LENGTH_SHORT).show();
                                    ((TextView) findViewById(R.id.campo_email)).setText(nuevoValor);
                                });
                            }

                            @Override
                            public void onEdicionFallida(String campo, String mensajeError) {
                                runOnUiThread(() ->
                                        Toast.makeText(PerfilActivity.this,
                                                "No pudimos guardar tu nuevo correo.\n\nVerifica tu conexión e intenta nuevamente.",
                                                Toast.LENGTH_LONG).show());
                            }
                        });
                    }
            );
        });

        // Botón editar password
        findViewById(R.id.editar_password).setOnClickListener(v -> {
            mostrarDialogoEdicionPassword();
        });

        // Botones de quejas y privacidad
        findViewById(R.id.chevron_quejas).setOnClickListener(v -> showQuejasPopup());
        findViewById(R.id.chevron_privacidad).setOnClickListener(v -> showPrivacidadPopup());
    }

    // ========================================================================
    // MÉTODOS DE LOGOUT Y ELIMINACIÓN DE CUENTA
    // ========================================================================

    /**
     * Configura los botones de logout y eliminación de cuenta.
     */
    private void setupLogoutButton() {
        // Botón de Logout
        findViewById(R.id.buttonLogout).setOnClickListener(v -> {
            mostrarDialogoConfirmacionLogout();
        });

        // Botón de Eliminar Cuenta
        findViewById(R.id.deleteAccount).setOnClickListener(v -> {
            mostrarDialogoEliminarCuenta();
        });
    }

    /**
     * Muestra un diálogo de confirmación antes de cerrar sesión.
     */
    private void mostrarDialogoConfirmacionLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro que deseas cerrar sesión?\n\n" +
                        "Podrás volver a iniciar sesión en cualquier momento.")
                .setPositiveButton("Sí, cerrar sesión", (dialog, which) -> {
                    realizarLogout();
                })
                .setNegativeButton("Cancelar", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Realiza el proceso completo de cierre de sesión:
     * 1. Limpia SharedPreferences (sesión local)
     * 2. Invalida el token en el backend (opcional)
     * 3. Redirige a LoginActivity
     * 4. Limpia el stack de actividades
     */
    private void realizarLogout() {
        Log.d(TAG, "Iniciando proceso de logout para userId: " + userId);

        // 1. Limpiar SharedPreferences (sesión local)
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear(); // Elimina TODOS los datos guardados
        editor.apply();

        Log.d(TAG, "SharedPreferences limpiado - Sesión local eliminada");

        // 2. Opcional: Invalidar token en el backend
        invalidarTokenEnBackend();

        // 3. Redirigir a LoginActivity
        Intent intent = new Intent(PerfilActivity.this, LoginActivity.class);

        // ⭐ FLAGS IMPORTANTES:
        // FLAG_ACTIVITY_NEW_TASK: Inicia una nueva tarea
        // FLAG_ACTIVITY_CLEAR_TASK: Limpia la pila de actividades
        // Esto previene que el usuario use el botón "atrás" para volver
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);

        // 4. Finalizar esta actividad
        finish();

        // 5. Mensaje de confirmación
        Toast.makeText(this,
                "Has cerrado sesión correctamente.\n\n¡Hasta pronto!",
                Toast.LENGTH_SHORT).show();

        Log.d(TAG, "Logout completado - Redirigido a LoginActivity");
    }

    /**
     * Invalida el token de sesión en el backend (opcional).
     * Esto previene que el token pueda ser usado nuevamente.
     */
    private void invalidarTokenEnBackend() {
        // Solo si tu backend tiene un endpoint de logout
        if (token == null || token.isEmpty()) {
            Log.w(TAG, "No hay token para invalidar");
            return;
        }

        String url = "http://api.sagucre.upv.edu.es/auth/logout";

        PeticionarioREST peticion = new PeticionarioREST();
        peticion.hacerPeticionREST("POST", url, null, new PeticionarioREST.RespuestaREST() {
            @Override
            public void callback(int codigo, String cuerpo) {
                // El logout ya se completó en el frontend
                // Esto es solo para invalidar el token en el backend
                if (codigo == 200) {
                    Log.d(TAG, "Token invalidado en backend correctamente");
                } else {
                    Log.w(TAG, "No se pudo invalidar token en backend - código: " + codigo);
                }
            }
        });
    }

    /**
     * Cierra la sesión sin confirmación y redirige a Login.
     * Se usa cuando hay errores de autenticación.
     */
    private void cerrarSesionYRedirigir() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent intent = new Intent(PerfilActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Muestra un diálogo de confirmación para eliminar la cuenta.
     * Advierte al usuario de que la acción es irreversible.
     */
    private void mostrarDialogoEliminarCuenta() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Eliminar Cuenta")
                .setMessage("ADVERTENCIA: Esta acción es irreversible.\n\n" +
                        "Se eliminarán permanentemente:\n" +
                        "• Todos tus datos personales\n" +
                        "• Historial de recorridos\n" +
                        "• Puntos y recompensas acumulados\n" +
                        "• Vinculación de dispositivos\n\n" +
                        "¿Estás completamente seguro de que deseas continuar?")
                .setPositiveButton("Sí, eliminar permanentemente", (dialog, which) -> {
                    // Mostrar segundo diálogo de confirmación
                    mostrarSegundaConfirmacionEliminarCuenta();
                })
                .setNegativeButton("Cancelar", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Segundo diálogo de confirmación para eliminar cuenta (doble verificación).
     */
    private void mostrarSegundaConfirmacionEliminarCuenta() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmación Final")
                .setMessage("Esta es tu última oportunidad para cancelar.\n\n" +
                        "¿Confirmas que deseas eliminar tu cuenta de forma PERMANENTE?")
                .setPositiveButton("Confirmar eliminación", (dialog, which) -> {
                    eliminarCuenta();
                })
                .setNegativeButton("No, cancelar", null)
                .show();
    }

    /**
     * Elimina la cuenta del usuario llamando al backend.
     * TODO: Implementar endpoint en el backend.
     */
    private void eliminarCuenta() {
        // TODO: Implementar llamada al backend para eliminar cuenta
        // String url = "http://api.sagucre.upv.edu.es/users/" + userId;
        // DELETE request

        Toast.makeText(this,
                "La eliminación de cuenta estará disponible próximamente.\n\nContacta con soporte si necesitas eliminar tu cuenta ahora.",
                Toast.LENGTH_LONG).show();

        Log.d(TAG, "Solicitud de eliminación de cuenta para userId: " + userId);

        // Cuando se implemente:
        // 1. Hacer DELETE al backend
        // 2. Si es exitoso, hacer logout automáticamente
        // 3. Mostrar mensaje de confirmación
    }

    // ========================================================================
    // MÉTODOS DE EDICIÓN DE PERFIL (sin cambios significativos)
    // ========================================================================

    private boolean esEmailValido(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        String patron = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(patron);
    }

    private void mostrarDialogoEdicionPassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cambiar contraseña");

        LinearLayout layout = crearLayoutDialogoPassword(false);
        builder.setView(layout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String currentPassword = ((EditText) layout.getChildAt(0)).getText().toString().trim();
            String newPassword = ((EditText) layout.getChildAt(1)).getText().toString().trim();
            String confirmPassword = ((EditText) layout.getChildAt(2)).getText().toString().trim();

            if (validarPassword(currentPassword, newPassword, confirmPassword)) {
                logicaEditar.actualizarPassword(currentPassword, newPassword,
                        new LogicaEditarPerfil.EditarCallback() {
                            @Override
                            public void onEdicionExitosa(String campo, String mensaje) {
                                runOnUiThread(() -> {
                                    Toast.makeText(PerfilActivity.this,
                                            "Contraseña actualizada correctamente.",
                                            Toast.LENGTH_SHORT).show();
                                    ((TextView) findViewById(R.id.campo_password)).setText("********");
                                });
                            }

                            @Override
                            public void onEdicionFallida(String campo, String mensajeError) {
                                runOnUiThread(() -> {
                                    String mensaje = interpretarErrorPassword(mensajeError);
                                    Toast.makeText(PerfilActivity.this, mensaje, Toast.LENGTH_LONG).show();
                                });

                            }
                            private String interpretarErrorPassword(String error) {
                                if (error.contains("401") || error.contains("incorrect") || error.contains("incorrecta")) {
                                    return "La contraseña actual es incorrecta.\n\nVerifica e intenta nuevamente.";
                                }

                                if (error.contains("weak") || error.contains("débil")) {
                                    return "La nueva contraseña es muy débil.\n\nUsa una combinación de letras, números y símbolos.";
                                }

                                return "No pudimos cambiar tu contraseña.\n\nVerifica tu conexión e intenta nuevamente.";
                            }
                        });
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private LinearLayout crearLayoutDialogoPassword(boolean modoPrueba) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 0, 50, 0);

        final EditText inputCurrent = new EditText(this);
        inputCurrent.setHint(modoPrueba ? "Contraseña actual (usar '123456' en pruebas)" : "Contraseña actual");
        inputCurrent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputCurrent);

        final EditText inputNew = new EditText(this);
        inputNew.setHint("Nueva contraseña");
        inputNew.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputNew);

        final EditText inputConfirm = new EditText(this);
        inputConfirm.setHint("Confirmar nueva contraseña");
        inputConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputConfirm);

        return layout;
    }

    private boolean validarPassword(String current, String newPass, String confirmPass) {
        if (current.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this,
                    "Debes completar todos los campos.\n\nIngresa tu contraseña actual y la nueva contraseña dos veces.",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!newPass.equals(confirmPass)) {
            Toast.makeText(this,
                    "Las contraseñas nuevas no coinciden.\n\nVerifica que hayas escrito la misma contraseña en ambos campos.",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (newPass.length() < 6) {
            Toast.makeText(this, "La nueva contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void mostrarDialogoEdicion(String campo, String titulo, OnValorEditadoListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(titulo);

        final EditText input = new EditText(this);
        if (campo.equals("password")) {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else if (campo.equals("email")) {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            input.setHint("ejemplo@dominio.com");
        }
        builder.setView(input);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nuevoValor = input.getText().toString().trim();
            if (!nuevoValor.isEmpty()) {
                listener.onValorEditado(nuevoValor);
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    interface OnValorEditadoListener {
        void onValorEditado(String nuevoValor);
    }

    private void showQuejasPopup() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_quejas);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.findViewById(R.id.btnVolverQuejas).setOnClickListener(v -> dialog.dismiss());

        dialog.findViewById(R.id.btnEnviarQueja).setOnClickListener(v -> {
            Toast.makeText(this,
                    "Tu queja fue enviada correctamente.\n\nTe responderemos en un plazo de 24-48 horas.",
                    Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showPrivacidadPopup() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_privacidad);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.findViewById(R.id.btnVolverPrivacidad).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}