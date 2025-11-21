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

public class PerfilActivity extends AppCompatActivity {
    private String token;
    private int userId;
    private LogicaEditarPerfil logicaEditar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.perfil);

        // Obtener token y userId del Intent
        userId = getIntent().getIntExtra("USER_ID", 0);
        token = getIntent().getStringExtra("TOKEN");

        if (userId == 0 || token == null) {
            Toast.makeText(this, "Error: No se recibieron credenciales válidas", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        logicaEditar = new LogicaEditarPerfil(token, userId);

        // Cargar datos del usuario y configurar botones
        cargarDatosUsuario();
        setupBotonesEdicion();
    }

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
                runOnUiThread(() -> {
                    Toast.makeText(PerfilActivity.this, "Error: " + mensajeError, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }


    private void setupBotonesEdicion() {
        // Botón editar username
        findViewById(R.id.editar_username).setOnClickListener(v -> {
            mostrarDialogoEdicion("username", "Nuevo nombre de usuario",
                    nuevoValor -> logicaEditar.actualizarUsername(nuevoValor, new LogicaEditarPerfil.EditarCallback() {
                        @Override
                        public void onEdicionExitosa(String campo, String mensaje) {
                            runOnUiThread(() -> {
                                Toast.makeText(PerfilActivity.this, mensaje, Toast.LENGTH_SHORT).show();
                                // Actualizar TextView
                                ((TextView) findViewById(R.id.campo_username)).setText(nuevoValor);
                            });
                        }

                        @Override
                        public void onEdicionFallida(String campo, String mensajeError) {
                            runOnUiThread(() ->
                                    Toast.makeText(PerfilActivity.this, mensajeError, Toast.LENGTH_SHORT).show());
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
                                    Toast.makeText(PerfilActivity.this, mensaje, Toast.LENGTH_SHORT).show();
                                    ((TextView) findViewById(R.id.campo_email)).setText(nuevoValor);
                                });
                            }

                            @Override
                            public void onEdicionFallida(String campo, String mensajeError) {
                                runOnUiThread(() ->
                                        Toast.makeText(PerfilActivity.this, mensajeError, Toast.LENGTH_SHORT).show());
                            }
                        });
                    }
            );
        });

        // Botón editar password
        findViewById(R.id.editar_password).setOnClickListener(v -> {
            mostrarDialogoEdicionPassword();
        });
    }

    /**
     * Valida el formato del email.
     */
    private boolean esEmailValido(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        // Patrón simple para validar email
        String patron = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(patron);
    }

    /**
     * Muestra un diálogo para editar la contraseña con verificación de contraseña actual.
     */
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
                                    Toast.makeText(PerfilActivity.this, mensaje, Toast.LENGTH_SHORT).show();
                                    ((TextView) findViewById(R.id.campo_password)).setText("********");
                                });
                            }

                            @Override
                            public void onEdicionFallida(String campo, String mensajeError) {
                                runOnUiThread(() ->
                                        Toast.makeText(PerfilActivity.this, mensajeError, Toast.LENGTH_SHORT).show());
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
    /**
     * Valida los campos de contraseña antes de enviar la solicitud.
     */
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

    /**
     * Muestra un diálogo para editar un campo.
     */
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

    private String obtenerTokenDeSharedPreferences() {
        // Implementar según cómo guardes el token
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        return prefs.getString("token", "");
    }

    private int obtenerUserIdDeSharedPreferences() {
        // Implementar según cómo guardes el user ID
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        return prefs.getInt("user_id", 0);
    }

}