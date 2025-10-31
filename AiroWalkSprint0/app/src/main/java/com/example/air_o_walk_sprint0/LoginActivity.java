package com.example.air_o_walk_sprint0;
import android.content.Intent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        // Mostrar progreso
        // progressBar.setVisibility(View.VISIBLE);

        LogicaLogin logicaLogin = new LogicaLogin(usuario, contrasena);
        logicaLogin.realizarLogin(new LogicaLogin.LoginCallback() {
            @Override
            public void onLoginExitoso(String respuesta) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Login hecho correctamente", Toast.LENGTH_LONG).show();

                        // Navegar a la siguiente actividad
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);


                        // Pasar a MainActivity
                        try {
                            // Convertir String a JSONObject
                            JSONObject jsonObject = new JSONObject(respuesta);

                            // Extraer valores
                            int userId = jsonObject.getInt("userId");
                            String token = jsonObject.getString("token");

                            Log.d("Login", "User ID: " + userId);
                            Log.d("Login", "Token: " + token);

                            // Usar los datos
                            intent.putExtra("USER_ID", userId);
                            intent.putExtra("TOKEN", token);
                            startActivity(intent);
                            finish();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        startActivity(intent);
                        finish();
                    }
                });
            }

            @Override
            public void onLoginFallido(String mensajeError) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Error: " + mensajeError, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}