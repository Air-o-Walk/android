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
 *
 * Además, gestiona la persistencia de la sesión mediante
 * SharedPreferences y redirige al usuario a la pantalla principal
 * cuando el login es exitoso.
 *
 * @author María Algora
 * @version 1.0
 */
public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsuario, editTextContrasena;
    private Button buttonLogin;
    private SharedPreferences prefs;

    /**
     * Inicializa los elementos del layout y configura el listener del botón de login.
     * @param savedInstanceState Estado previo de la actividad, si lo hubiera
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // SharedPreferences para guardar sesión
        prefs = getSharedPreferences("UserSession", MODE_PRIVATE);

        // Verificar si ya está logueado
        if (prefs.getBoolean("isLoggedIn", false)) {
            navigateToMainActivity();
            return;
        }

        setContentView(R.layout.login_activity);

        editTextUsuario = findViewById(R.id.editTextUsuario);
        editTextContrasena = findViewById(R.id.editTextContrasena);
        buttonLogin = findViewById(R.id.buttonLogin);
        TextView forgotPassword = findViewById(R.id.textForgotPassword);

        forgotPassword.setOnClickListener(v -> showForgotPasswordPopup());
        MaterialButton buttonRegister = findViewById(R.id.buttonRegister);

        buttonRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegistroActivity.class);
            startActivity(intent);
        });


        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                realizarLogin();
            }
        });
    }

    /**
     * Obtiene los valores introducidos por el usuario, valida los campos
     * y realiza la autenticación usando la clase LogicaLogin.
     * Usa hilos secundarios para no bloquear el principal (la interfaz) mientras hace peticiones
     */
    private void realizarLogin() {
        String usuario = editTextUsuario.getText().toString().trim();
        String contrasena = editTextContrasena.getText().toString().trim();

        if (usuario.isEmpty() || contrasena.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        LogicaLogin logicaLogin = new LogicaLogin(usuario, contrasena);
        logicaLogin.realizarLogin(new LogicaLogin.LoginCallback() {
            @Override
            public void onLoginExitoso(String respuesta) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(LoginActivity.this, "Login hecho correctamente", Toast.LENGTH_LONG).show();

                        // Navegar a la siguiente actividad
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);

                        // Pasar a MainActivity
                        try {
                            JSONObject jsonObject = new JSONObject(respuesta);
                            int userId = jsonObject.getInt("userId");
                            String token = jsonObject.getString("token");

                            Log.d("Login", "User ID: " + userId);
                            Log.d("Login", "Token: " + token);

                            // GUARDAR sesión en SharedPreferences
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putInt("userId", userId);
                            editor.putString("token", token);
                            editor.putBoolean("isLoggedIn", true);
                            editor.apply();

                            navigateToMainActivity();

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(LoginActivity.this, "Error al procesar respuesta", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onLoginFallido(String mensajeError) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(LoginActivity.this, "Error: " + mensajeError, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("USER_ID", prefs.getInt("userId", -1));
        intent.putExtra("TOKEN", prefs.getString("token", ""));
        startActivity(intent);
        finish();
    }

    /*
    POP UP PARA RECUPERAR CONTRASENA
     */
    private void showForgotPasswordPopup() {

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_forgot_password);

        dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setDimAmount(0.5f); // dark background
        dialog.setCancelable(true);

        EditText emailField = dialog.findViewById(R.id.editTextPopupEmail);
        MaterialButton recoverButton = dialog.findViewById(R.id.buttonPopupRecover);
        MaterialButton loginButton = dialog.findViewById(R.id.buttonPopupLogin);

        recoverButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Introduce un correo", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Correo enviado (simulado)", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        loginButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

}