package com.example.air_o_walk_sprint0;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * María Algora
 * Clase que implementa la interfaz del registro de usuario.
 * Gestiona los campos del formulario, valida los datos introducidos y llama a la logica.
 * Recibe nombre, apellido, correo, dni, telefono : cajas de texto
 *        registro, login : boton
 *        checkbox : checkbox
 *        selección : spinner
 */

public class RegistroActivity extends AppCompatActivity {

    private EditText campoFirstName, campoLastName, campoEmail, campoDni, campoPhone;
    private Spinner spinnerTownHall;
    private CheckBox checkBoxTerms;
    private Button buttonRegister, buttonLogin;

    /**
     * Inicializa todos los componentes del layout y configura
     * los eventos de los botones de registro e inicio de sesión.
     * @param savedInstanceState Estado previo de la actividad, si lo hubiera
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registro_activity);

        // Inicializar los componentes
        campoFirstName = findViewById(R.id.campo_first_name);
        campoLastName = findViewById(R.id.campo_last_name);
        campoEmail = findViewById(R.id.campo_email);
        campoDni = findViewById(R.id.campo_dni);
        campoPhone = findViewById(R.id.campo_phone);
        spinnerTownHall = findViewById(R.id.spinner_town_hall_id);
        checkBoxTerms = findViewById(R.id.checkBoxTerms);
        buttonRegister = findViewById(R.id.buttonRegister);
        buttonLogin = findViewById(R.id.buttonLogin);

        // Configurar el comportamiento del botón de registro
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        // Configurar el comportamiento del botón de login
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Lógica para redirigir a la pantalla de inicio de sesión
                startActivity(new Intent(RegistroActivity.this, LoginActivity.class));
            }
        });
    }

    /**
     * Valida los datos introducidos en el formulario y realiza la acción de registro llamando a la logica.
     */
    private void registerUser() {
        //Capturo los valores y trim() quita espacios accidentales
        String firstName = campoFirstName.getText().toString().trim();
        String lastName = campoLastName.getText().toString().trim();
        String email = campoEmail.getText().toString().trim();
        String dni = campoDni.getText().toString().trim();
        String phone = campoPhone.getText().toString().trim();

        // Validación de campos
        if (TextUtils.isEmpty(firstName)) {
            campoFirstName.setError("El nombre es obligatorio");
            return;
        }

        if (TextUtils.isEmpty(lastName)) {
            campoLastName.setError("El apellido es obligatorio");
            return;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            campoEmail.setError("Por favor ingrese un correo electrónico válido");
            return;
        }

        if (TextUtils.isEmpty(dni)) {
            campoDni.setError("El DNI es obligatorio");
            return;
        }

        if (TextUtils.isEmpty(phone) || !phone.matches("[0-9]+") || phone.length() < 7) {
            campoPhone.setError("Por favor ingrese un número de teléfono válido");
            return;
        }

        if (!checkBoxTerms.isChecked()) {
            Toast.makeText(this, "Debes aceptar los términos y condiciones", Toast.LENGTH_SHORT).show();
            return;
        }

        // Llamar a logica

        // Mostrar mensaje de éxito
        Toast.makeText(this, "Registro exitoso, revisa tu correo electrónico", Toast.LENGTH_SHORT).show();
    }
}