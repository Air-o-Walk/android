package com.example.air_o_walk_sprint0;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsuario, editTextContrasena;
    private Button buttonLogin;
    private SharedPreferences prefs;

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

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                realizarLogin();
            }
        });
    }

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
}