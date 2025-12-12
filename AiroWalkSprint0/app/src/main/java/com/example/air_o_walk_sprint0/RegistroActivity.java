package com.example.air_o_walk_sprint0;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Hecho por Maria Algora
 * Clase que implementa la interfaz del registro de usuario.
 * Gestiona los campos del formulario, valida los datos introducidos y llama a la lógica correspondiente.
 */
public class RegistroActivity extends AppCompatActivity {

    private EditText campoFirstName, campoLastName, campoEmail, campoDni, campoPhone;
    private Spinner spinnerTownHall;
    private CheckBox checkBoxTerms;
    private Button buttonRegister, buttonLogin;
    private String selectedTownHallName;
    private HashMap<String, String> ayuntamientosMap = new HashMap<>();

     private LogicaRegistro logicaRegistro;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registro_activity);

        campoFirstName = findViewById(R.id.campo_first_name);
        campoLastName = findViewById(R.id.campo_last_name);
        campoEmail = findViewById(R.id.campo_email);
        campoDni = findViewById(R.id.campo_dni);
        campoPhone = findViewById(R.id.campo_phone);
        spinnerTownHall = findViewById(R.id.spinner_town_hall_id);
        checkBoxTerms = findViewById(R.id.checkBoxTerms);
        checkBoxTerms.setOnClickListener(v -> mostrarPopupTerminosYPrivacidad());
        buttonRegister = findViewById(R.id.buttonRegister);
        buttonLogin = findViewById(R.id.buttonLogin);

        logicaRegistro = new LogicaRegistro();
        obtenerAyuntamientos();


        buttonRegister.setOnClickListener(v -> registerUser());
        buttonLogin.setOnClickListener(v -> startActivity(new Intent(RegistroActivity.this, LoginActivity.class)));
    }

    private void obtenerAyuntamientos() {
        logicaRegistro.obtenerListaAyuntamientos(new LogicaRegistro.AyuntamientosCallback() {
            @Override
            public void onAyuntamientosObtenidos(HashMap<String, String> ayuntamientosMap) {
                RegistroActivity.this.ayuntamientosMap = ayuntamientosMap;
                ArrayList<String> townHallNames = new ArrayList<>(ayuntamientosMap.keySet());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(RegistroActivity.this,
                        android.R.layout.simple_spinner_item, townHallNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerTownHall.setAdapter(adapter);
            }

            @Override
            public void onError(String mensajeError) {
                Toast.makeText(RegistroActivity.this, mensajeError, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void registerUser() {
        String firstName = campoFirstName.getText().toString().trim();
        String lastName = campoLastName.getText().toString().trim();
        String email = campoEmail.getText().toString().trim();
        String dni = campoDni.getText().toString().trim();
        String phone = campoPhone.getText().toString().trim();

        if (TextUtils.isEmpty(firstName)) {
            campoFirstName.setError("El nombre es obligatorio");
            return;
        }
        if (TextUtils.isEmpty(lastName)) {
            campoLastName.setError("El apellido es obligatorio");
            return;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            campoEmail.setError("Correo electrónico inválido");
            return;
        }
        if (!dni.matches("\\d{8}[A-Za-z]")) {
            campoDni.setError("El DNI debe tener 8 números seguidos de 1 letra");
            return;
        }
        if (TextUtils.isEmpty(phone) || !phone.matches("[0-9]+") || phone.length() < 7) {
            campoPhone.setError("Teléfono no válido");
            return;
        }
        if (!checkBoxTerms.isChecked()) {
            Toast.makeText(this, "Debes aceptar los términos y condiciones", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedTownHallName = (String) spinnerTownHall.getSelectedItem();
        if (selectedTownHallName == null || selectedTownHallName.isEmpty()) {
            Toast.makeText(this, "Selecciona un ayuntamiento", Toast.LENGTH_SHORT).show();
            return;
        }

        String townHallId = ayuntamientosMap.get(selectedTownHallName);

        logicaRegistro.solicitudUsuario(townHallId, firstName,lastName, email, dni, phone,
                new LogicaRegistro.RegistroCallback() {
                    @Override
                    public void onRegistroExitoso(String respuestaServidor) {
                        Toast.makeText(RegistroActivity.this, respuestaServidor, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onRegistroFallido(String mensajeError) {
                        Toast.makeText(RegistroActivity.this, mensajeError, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void mostrarPopupTerminosYPrivacidad() {
        // Contenido de los términos y condiciones y la política de privacidad
        String contenido =
                "1. Aceptas proporcionar información verídica.\n" +
                "2. No usarás el servicio para fines ilegales.\n" +
                "\n" +
                "Política de Privacidad:\n\n" +
                "1. Protegemos tus datos personales según la ley de protección de datos.\n" +
                "2. No compartiremos tu información con terceros sin tu consentimiento.";

        // Crear el popup con los términos y condiciones y la política de privacidad
        new AlertDialog.Builder(this)
                .setTitle("Términos y Condiciones y Política de Privacidad")
                .setMessage(contenido)
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    // Marcar el checkbox si el usuario acepta
                    checkBoxTerms.setChecked(true);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    // Desmarcar el checkbox si el usuario no acepta
                    checkBoxTerms.setChecked(false);
                })
                .show();
    }
}
