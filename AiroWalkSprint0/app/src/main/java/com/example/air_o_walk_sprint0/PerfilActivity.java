package com.example.air_o_walk_sprint0;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.air_o_walk_sprint0.LogicaEditarPerfil;

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
 *
 * La comunicación con el backend se realiza mediante la clase LogicaEditarPerfil.
 *
 * @author Maria Algora
 * @version 1.0
 */
public class PerfilActivity extends BaseActivity {
    private String token;
    private int userId;
    private LogicaEditarPerfil logicaEditar;

    // --------------------------------------------------------------
    // onCreate()
    // Descripción: Inicializa la activity, obtiene las credenciales del usuario
    //              (token y userId) y configura la interfaz de usuario.
    //              Si no se reciben credenciales válidas, cierra la actividad.
    // Parámetros: savedInstanceState : estado guardado de la actividad
    // Diseño: onCreate() -> cargarDatosUsuario() + setupBotonesEdicion()
    // --------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);
        setupHeaderAndDrawer(true);
        setupBackBehavior();

        // Obtener token y userId del Intent
        userId = getIntent().getIntExtra("USER_ID", 0);
        token = getIntent().getStringExtra("TOKEN");

        if (userId == 0 || token == null) {
            Toast.makeText(this, "Error: No se recibieron credenciales válidas. Por favor, vuelve a iniciar sesión.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        logicaEditar = new LogicaEditarPerfil(token, userId);

        // Cargar datos del usuario y configurar botones
        cargarDatosUsuario();
        setupBotonesEdicion();
    }

    // --------------------------------------------------------------
    // cargarDatosUsuario()
    // Descripción: Solicita los datos básicos del usuario (username y email)
    //              al backend mediante LogicaEditarPerfil y actualiza la interfaz
    //              con la información recibida.
    // Diseño: cargarDatosUsuario() -> LogicaEditarPerfil.obtenerDatosBasicosUsuario() -> actualizar UI
    // --------------------------------------------------------------
    private void cargarDatosUsuario() {
        logicaEditar.obtenerDatosBasicosUsuario(new LogicaEditarPerfil.UsuarioBasicoCallback() {
            @Override
            public void onUsuarioObtenido(String username, String email) {
                runOnUiThread(() -> {
                    TextView tvUsername = findViewById(R.id.campo_username);
                    TextView tvEmail = findViewById(R.id.campo_email);
                    tvUsername.setText(username);
                    tvEmail.setText(email);
                });
            }

            @Override
            public void onError(String mensajeError) {
                runOnUiThread(() ->
                        Toast.makeText(PerfilActivity.this, "No se pudieron cargar tus datos. Inténtalo nuevamente más tarde.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // --------------------------------------------------------------
    // setupBotonesEdicion()
    // Descripción: Configura los listeners de los botones de edición para:
    //              - Editar nombre de usuario
    //              - Editar email (con validación)
    //              - Cambiar contraseña (con verificación)
    //              - Acceder a quejas y política de privacidad
    // Diseño: setupBotonesEdicion() -> listeners -> diálogos de edición
    // --------------------------------------------------------------
    private void setupBotonesEdicion() {
        // Botón editar username
        findViewById(R.id.editar_username).setOnClickListener(v -> {
            mostrarDialogoEdicion("username", "Nuevo nombre de usuario",
                    nuevoValor -> logicaEditar.actualizarUsername(nuevoValor, new LogicaEditarPerfil.EditarCallback() {
                        @Override
                        public void onEdicionExitosa(String campo, String mensaje) {
                            runOnUiThread(() -> {
                                Toast.makeText(PerfilActivity.this, "Nombre de usuario actualizado exitosamente.", Toast.LENGTH_SHORT).show();
                                ((TextView) findViewById(R.id.campo_username)).setText(nuevoValor);
                            });
                        }

                        @Override
                        public void onEdicionFallida(String campo, String mensajeError) {
                            runOnUiThread(() ->
                                    Toast.makeText(PerfilActivity.this, "No se pudo actualizar tu nombre de usuario. Inténtalo nuevamente.", Toast.LENGTH_SHORT).show());
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
                                            "Formato de email inválido. Debe tener @ y dominio",
                                            Toast.LENGTH_LONG).show());
                            return;
                        }

                        logicaEditar.actualizarEmail(nuevoValor, new LogicaEditarPerfil.EditarCallback() {
                            @Override
                            public void onEdicionExitosa(String campo, String mensaje) {
                                runOnUiThread(() -> {
                                    Toast.makeText(PerfilActivity.this, "Correo electrónico actualizado correctamente.", Toast.LENGTH_SHORT).show();
                                    ((TextView) findViewById(R.id.campo_email)).setText(nuevoValor);
                                });
                            }

                            @Override
                            public void onEdicionFallida(String campo, String mensajeError) {
                                runOnUiThread(() ->
                                        Toast.makeText(PerfilActivity.this, "No se pudo actualizar el correo. Inténtalo de nuevo más tarde.", Toast.LENGTH_SHORT).show());
                            }
                        });
                    }
            );
        });

        // Botón editar password
        findViewById(R.id.editar_password).setOnClickListener(v -> {
            mostrarDialogoEdicionPassword();
        });

        // QUEJAS + PRIVACIDAD
        findViewById(R.id.chevron_quejas).setOnClickListener(v -> showQuejasPopup());
        findViewById(R.id.chevron_privacidad).setOnClickListener(v -> showPrivacidadPopup());
    }

    // --------------------------------------------------------------
    // esEmailValido()
    // Descripción: Valida el formato de un email usando expresiones regulares.
    //              Verifica que contenga '@', dominio y extensión válida.
    // Parámetros: email : dirección de correo a validar
    // Diseño: email -> esEmailValido() -> boolean
    // --------------------------------------------------------------
    private boolean esEmailValido(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        // Patrón simple para validar email
        String patron = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(patron);
    }

    // --------------------------------------------------------------
    // mostrarDialogoEdicionPassword()
    // Descripción: Muestra un diálogo para cambiar la contraseña del usuario.
    //              Solicita: contraseña actual, nueva contraseña y confirmación.
    //              Valida los campos y envía la petición al backend.
    // Diseño: mostrarDialogoEdicionPassword() -> validarPassword() -> actualizarPassword()
    // --------------------------------------------------------------
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
                                    Toast.makeText(PerfilActivity.this, "Contraseña cambiada correctamente.", Toast.LENGTH_SHORT).show();
                                    ((TextView) findViewById(R.id.campo_password)).setText("********");
                                });
                            }

                            @Override
                            public void onEdicionFallida(String campo, String mensajeError) {
                                runOnUiThread(() ->
                                        Toast.makeText(PerfilActivity.this, "Hubo un problema al cambiar la contraseña. Intenta nuevamente.", Toast.LENGTH_SHORT).show());
                            }
                        });
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // --------------------------------------------------------------
    // crearLayoutDialogoPassword()
    // Descripción: Crea el layout con los campos de entrada para el diálogo
    //              de cambio de contraseña (actual, nueva, confirmar).
    // Parámetros: modoPrueba : si es true, añade texto de ayuda para pruebas
    // Diseño: modoPrueba -> crearLayoutDialogoPassword() -> LinearLayout
    // --------------------------------------------------------------
    private LinearLayout crearLayoutDialogoPassword(boolean modoPrueba) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 0, 50, 0);

        final EditText inputCurrent = new EditText(this);
        if (modoPrueba) {
            inputCurrent.setHint("Contraseña actual (usar '123456' en pruebas)");
        } else {
            inputCurrent.setHint("Contraseña actual");
        }
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

    // --------------------------------------------------------------
    // validarPassword()
    // Descripción: Valida los campos de contraseña antes de enviar la solicitud.
    //              Verifica que: no estén vacíos, coincidan entre sí,
    //              y la nueva contraseña tenga al menos 6 caracteres.
    // Parámetros: - current : contraseña actual
    //             - newPass : nueva contraseña
    //             - confirmPass : confirmación de nueva contraseña
    // Diseño: (current, newPass, confirmPass) -> validarPassword() -> boolean
    // --------------------------------------------------------------
    private boolean validarPassword(String current, String newPass, String confirmPass) {
        if (current.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!newPass.equals(confirmPass)) {
            Toast.makeText(this, "Las nuevas contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (newPass.length() < 6) {
            Toast.makeText(this, "La nueva contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    // --------------------------------------------------------------
    // mostrarDialogoEdicion()
    // Descripción: Muestra un diálogo genérico para editar un campo del perfil.
    //              Configura el tipo de entrada según el campo (email, password, texto).
    // Parámetros: - campo : nombre del campo a editar
    //             - titulo : título del diálogo
    //             - listener : callback que recibe el nuevo valor
    // Diseño: (campo, titulo, listener) -> mostrarDialogoEdicion() -> listener.onValorEditado()
    // --------------------------------------------------------------
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

    // -----------------------------------------------------------------
    // Interface para manejar valores editados
    // -----------------------------------------------------------------
    /**
     * @interface OnValorEditadoListener
     * @brief Interfaz callback para recibir el nuevo valor editado en un diálogo.
     */
    interface OnValorEditadoListener {
        void onValorEditado(String nuevoValor);
    }

    // --------------------------------------------------------------
    // obtenerTokenDeSharedPreferences()
    // Descripción: Recupera el token de autenticación almacenado en SharedPreferences.
    // Diseño: obtenerTokenDeSharedPreferences() -> String (token)
    // --------------------------------------------------------------
    private String obtenerTokenDeSharedPreferences() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        return prefs.getString("token", "");
    }

    // --------------------------------------------------------------
    // obtenerUserIdDeSharedPreferences()
    // Descripción: Recupera el ID de usuario almacenado en SharedPreferences.
    // Diseño: obtenerUserIdDeSharedPreferences() -> int (userId)
    // --------------------------------------------------------------
    private int obtenerUserIdDeSharedPreferences() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        return prefs.getInt("user_id", 0);
    }

    // --------------------------------------------------------------
    // showQuejasPopup()
    // Descripción: Muestra un diálogo modal con el formulario de quejas.
    //              Permite al usuario enviar sugerencias o reportar problemas.
    // Diseño: showQuejasPopup() -> Dialog -> dismiss/enviar
    // --------------------------------------------------------------
    private void showQuejasPopup() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_quejas);
        Spinner spinnerTipo = dialog.findViewById(R.id.spinnerTipo);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.tipos_incidencia,
                R.layout.spinner_item
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerTipo.setAdapter(adapter);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.findViewById(R.id.btnVolverQuejas).setOnClickListener(v -> dialog.dismiss());

        dialog.findViewById(R.id.btnEnviarQueja).setOnClickListener(v -> {

            EditText editDescripcion = dialog.findViewById(R.id.editDescripcion);

            String tipo = spinnerTipo.getSelectedItem().toString();
            String descripcion = editDescripcion.getText().toString().trim();

            if (descripcion.isEmpty()) {
                Toast.makeText(this, "La descripción no puede estar vacía", Toast.LENGTH_SHORT).show();
                return;
            }

            LogicaIncidencias logicaIncidencias =
                    new LogicaIncidencias(userId, tipo, descripcion);

            logicaIncidencias.enviarIncidencia();

            Toast.makeText(this, "Incidencia enviada correctamente", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        dialog.show();
    }

    // --------------------------------------------------------------
    // showPrivacidadPopup()
    // Descripción: Muestra un diálogo modal con la política de privacidad
    //              de la aplicación.
    // Diseño: showPrivacidadPopup() -> Dialog -> dismiss
    // --------------------------------------------------------------
    private void showPrivacidadPopup() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_privacidad);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.findViewById(R.id.btnVolverPrivacidad).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}