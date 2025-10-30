package com.example.air_o_walk_sprint0;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsuario, editTextContrasena;
    private Button buttonLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

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

        com.example.air_o_walk_sprint0.LogicaLogin logicaLogin = new com.example.air_o_walk_sprint0.LogicaLogin(usuario, contrasena);
        logicaLogin.realizarLogin(new com.example.air_o_walk_sprint0.LogicaLogin.LoginCallback() {
            @Override
            public void onLoginExitoso(String respuesta) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Login exitoso", Toast.LENGTH_SHORT).show();

                        // Navegar a la siguiente actividad
                        Intent intent = new Intent(LoginActivity.this, com.example.air_o_walk_sprint0.MainActivity.class);
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