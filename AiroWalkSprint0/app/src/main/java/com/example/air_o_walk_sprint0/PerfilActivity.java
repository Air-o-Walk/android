package com.example.air_o_walk_sprint0;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.air_o_walk_sprint0.LogicaEditarPerfil;

// --------------------------------------------------------------
// PerfilActivity.java
// Hecho por Maria Algora
// Descripción: Pantalla de perfil del usuario. Permite visualizar y editar datos básicos.
//      - Muestra username y email actuales.
//      - Permite modificar username, email y contraseña.
//      - Valida formato de email y contraseñas antes de enviarlas.
// --------------------------------------------------------------
public class PerfilActivity extends AppCompatActivity {

    private String token;
    private int userId;
    private LogicaEditarPerfil logicaEditar;

    // --------------------------------------------------------------
    // onCreate()
    // Descripción: Punto de entrada de la Activity. Inicializa la interfaz de usuario,
    //              obtiene las credenciales del intent anterior y configura los botones de edición.
    // Diseño: Intent(USER_ID, TOKEN) → inicializar lógica y UI → mostrar datos.
    // --------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.perfil);

        // ---------------------------
        // OBTENER CREDENCIALES
        // ---------------------------
        userId = getIntent().getIntExtra("USER_ID", 0);
        token = getIntent().getStringExtra("TOKEN");

        if (userId == 0 || token == null) {
            Toast.makeText(this, "Error: No se recibieron credenciales válidas. Por favor, vuelve a iniciar sesión.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        logicaEditar = new LogicaEditarPerfil(token, userId);

        // ---------------------------
        // CARGAR DATOS DEL USUARIO
        // ---------------------------
        cargarDatosUsuario();

        // ---------------------------
        // CONFIGURAR BOTONES DE EDICIÓN
        // ---------------------------
        setupBotonesEdicion();
    }

    // --------------------------------------------------------------
    // cargarDatosUsuario()
    // Descripción: Obtiene la información básica del usuario (username, email)
    //              desde la lógica de negocio y la muestra en la interfaz.
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
    // Descripción: Configura los tres botones de edición de la pantalla:
    //              username, email y password. Cada botón abre un diálogo correspondiente.
    // --------------------------------------------------------------
    private void setupBotonesEdicion() {

        // ------ Botón editar username ------
        findViewById(R.id.editar_username).setOnClickListener(v -> {
                    mostrarDialogoEdicion("username", "Nuevo nombre de usuario (3-20 caracteres)",
                            nuevoValor -> {
                                if (!esUsernameValido(nuevoValor)) {
                                    Toast.makeText(this, "Por favor, ingresa un nombre de usuario válido. Debe tener entre 3 y 20 caracteres y solo puede incluir letras, números, puntos, guiones bajos o guiones.", Toast.LENGTH_LONG).show();
                                    return;
                                }

                        logicaEditar.actualizarUsername(nuevoValor, new LogicaEditarPerfil.EditarCallback() {
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

        // ------ Botón editar email ------
        findViewById(R.id.editar_email).setOnClickListener(v -> {
            mostrarDialogoEdicion("email", "Nuevo email",
                    nuevoValor -> {

                        // Validación del formato del email antes de enviar
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

        // ------ Botón editar password ------
        findViewById(R.id.editar_password).setOnClickListener(v -> {
            mostrarDialogoEdicionPassword();
        });
    }

    // --------------------------------------------------------------
    // esEmailValido()
    // Descripción: Evalúa si el formato de un email es correcto utilizando una expresión regular.
    // --------------------------------------------------------------
    private boolean esEmailValido(String email) {
        if (email == null || email.isEmpty()) return false;
        return email.trim().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    // --------------------------------------------------------------
    // esUsernameValido()
    // Descripción: Evalúa si el formato de un username es correcto utilizando una expresión regular.
    // --------------------------------------------------------------
    private boolean esUsernameValido(String username) {
        if (username == null || username.trim().isEmpty()) return false;
        return username.trim().matches("^[a-zA-Z0-9_.-]{3,20}$");
    }

    // --------------------------------------------------------------
    // mostrarDialogoEdicionPassword()
    // Descripción: Genera un cuadro de diálogo que permite al usuario cambiar la contraseña.
    //              Solicita contraseña actual, nueva y confirmación antes de enviar.
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
    // Descripción: Crea dinámicamente un formulario dentro del diálogo de contraseña
    //              con los campos: actual, nueva y confirmar.
    // --------------------------------------------------------------
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

    // --------------------------------------------------------------
    // validarPassword()
    // Descripción: Realiza verificaciones básicas de seguridad antes de enviar una nueva contraseña.
    // Validaciones:
    //      - Todos los campos completados.
    //      - Nueva contraseña y confirmación coinciden.
    //      - Longitud mínima de 6 caracteres.
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

            if (!nuevaContraseña.matches("^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+-=]).{6,}$")) {
                Toast.makeText(this, "¡Ups! Tu contraseña debe tener al menos 6 caracteres, " +
                                "una letra, un número y un carácter especial (como @, #, _, -, etc).",
                        Toast.LENGTH_LONG).show();
                return false;
            }

        return true;
    }

    // --------------------------------------------------------------
    // mostrarDialogoEdicion()
    // Descripción: Muestra un diálogo reutilizable para editar textos simples (username, email, etc.)
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
            if (!nuevoValor.isEmpty()) listener.onValorEditado(nuevoValor);
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // --------------------------------------------------------------
    // OnValorEditadoListener
    // Descripción: Interfaz funcional simple para manejar eventos al editar campos.
    // --------------------------------------------------------------
    interface OnValorEditadoListener {
        void onValorEditado(String nuevoValor);
    }

    // --------------------------------------------------------------
    // obtenerTokenDeSharedPreferences()
    // obtenerUserIdDeSharedPreferences()
    // Descripción: Métodos auxiliares para recuperar credenciales almacenadas localmente.
    //              Su implementación depende del diseño de guardado del token.
    // --------------------------------------------------------------
    private String obtenerTokenDeSharedPreferences() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        return prefs.getString("token", "");
    }

    private int obtenerUserIdDeSharedPreferences() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        return prefs.getInt("user_id", 0);
    }
}
