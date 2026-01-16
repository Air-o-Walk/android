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
 * @class RegistroActivity
 * @brief Actividad que gestiona la interfaz de registro de nuevos usuarios.
 *
 * Esta clase implementa el formulario de registro donde los usuarios
 * pueden crear una cuenta proporcionando sus datos personales.
 * Incluye validación de campos, carga de ayuntamientos desde el servidor
 * y aceptación de términos y condiciones.
 *
 * Funcionalidades principales:
 * - Validación de datos del formulario (nombre, email, DNI, teléfono)
 * - Carga dinámica de ayuntamientos disponibles
 * - Verificación de aceptación de términos y condiciones
 * - Comunicación con el backend para registro de usuarios
 *
 * @author Maria Algora
 * @version 1.0
 */
public class RegistroActivity extends AppCompatActivity {

    private EditText campoFirstName, campoLastName, campoEmail, campoDni, campoPhone;
    private Spinner spinnerTownHall;
    private CheckBox checkBoxTerms;
    private Button buttonRegister, buttonLogin;
    private String selectedTownHallName;
    private HashMap<String, String> ayuntamientosMap = new HashMap<>();

    private LogicaRegistro logicaRegistro;

    // --------------------------------------------------------------
    // onCreate()
    // Descripción: Inicializa la actividad, configura los componentes de la interfaz
    //              y carga la lista de ayuntamientos disponibles.
    // Diseño: onCreate() -> inicializa vistas -> obtenerAyuntamientos() -> configura listeners
    // Parámetros: savedInstanceState : estado guardado de la actividad
    // --------------------------------------------------------------
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

    // --------------------------------------------------------------
    // obtenerAyuntamientos()
    // Descripción: Solicita al backend la lista de ayuntamientos disponibles
    //              y los muestra en el spinner de selección.
    // Diseño: obtenerAyuntamientos() -> LogicaRegistro -> callback -> actualiza spinner
    // --------------------------------------------------------------
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
                Toast.makeText(RegistroActivity.this, "No se pudieron cargar los ayuntamientos. Inténtalo nuevamente más tarde.", Toast.LENGTH_LONG).show();
            }
        });
    }

    // --------------------------------------------------------------
    // registerUser()
    // Descripción: Valida todos los campos del formulario y, si son correctos,
    //              envía la solicitud de registro al backend.
    // Diseño: registerUser() -> valida campos -> LogicaRegistro.solicitudUsuario() -> callback
    // --------------------------------------------------------------
    private void registerUser() {
        String firstName = campoFirstName.getText().toString().trim();
        String lastName = campoLastName.getText().toString().trim();
        String email = campoEmail.getText().toString().trim();
        String dni = campoDni.getText().toString().trim();
        String phone = campoPhone.getText().toString().trim();

        // Validación del nombre
        if (TextUtils.isEmpty(firstName)) {
            campoFirstName.setError("El nombre es obligatorio");
            return;
        }

        // Validación del apellido
        if (TextUtils.isEmpty(lastName)) {
            campoLastName.setError("El apellido es obligatorio");
            return;
        }

        // Validación del email
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            campoEmail.setError("Correo electrónico inválido");
            return;
        }

        // Validación del DNI (8 dígitos + 1 letra)
        if (!dni.matches("\\d{8}[A-Za-z]")) {
            campoDni.setError("El DNI debe tener 8 números seguidos de 1 letra");
            return;
        }

        // Validación del teléfono
        if (TextUtils.isEmpty(phone) || !phone.matches("[0-9]+") || phone.length() < 7) {
            campoPhone.setError("Teléfono no válido");
            return;
        }

        // Validación de aceptación de términos
        if (!checkBoxTerms.isChecked()) {
            Toast.makeText(this, "Por favor, acepta los términos y condiciones para continuar.", Toast.LENGTH_LONG).show();
            return;
        }

        // Validación de selección de ayuntamiento
        selectedTownHallName = (String) spinnerTownHall.getSelectedItem();
        if (selectedTownHallName == null || selectedTownHallName.isEmpty()) {
            Toast.makeText(this, "Por favor, selecciona tu ayuntamiento antes de registrarte.", Toast.LENGTH_LONG).show();
            return;
        }

        String townHallId = ayuntamientosMap.get(selectedTownHallName);

        // Envío de solicitud de registro
        logicaRegistro.solicitudUsuario(townHallId, firstName, lastName, email, dni, phone,
                new LogicaRegistro.RegistroCallback() {
                    @Override
                    public void onRegistroExitoso(String respuestaServidor) {
                        // Generar credenciales automáticas
                        String usuario = generarUsuario(firstName, lastName);
                        String contrasena = generarContrasena(dni);

                        // Mostrar popup con credenciales
                        mostrarPopupCredenciales(usuario, contrasena);
                    }

                    @Override
                    public void onRegistroFallido(String mensajeError) {
                        Toast.makeText(RegistroActivity.this, "No se pudo completar el registro. Verifica tus datos o inténtalo más tarde.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    // --------------------------------------------------------------
    // mostrarPopupTerminosYPrivacidad()
    // Descripción: Muestra un diálogo con los términos y condiciones y la
    //              política de privacidad para que el usuario los acepte.
    // Diseño: mostrarPopupTerminosYPrivacidad() -> AlertDialog -> usuario acepta/cancela
    // --------------------------------------------------------------
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


    private String generarUsuario(String nombre, String apellido) {
        String inicial = nombre.substring(0, 1).toLowerCase();
        String apellidoRecortado = apellido.length() >= 4 ?
                apellido.substring(0, 4).toLowerCase() : apellido.toLowerCase();
        return inicial + "." + apellidoRecortado;
    }

    private String generarContrasena(String dni) {
        // DNI sin la letra final
        return dni.substring(0, dni.length() - 1);
    }

    private void mostrarPopupCredenciales(String usuario, String contrasena) {
        // Preparar texto para guardar en notas
        String textoNotas = "Mis credenciales Air_o_Walk:\n\n" +
                "Usuario: " + usuario + "\n" +
                "Contraseña: " + contrasena + "\n\n" +
                "Nota: Cambia tu contraseña al iniciar sesión.";

        new AlertDialog.Builder(this)
                .setTitle("Registro completado")
                .setMessage("Guarde sus credenciales:\n\n" +
                        "Usuario: " + usuario + "\n" +
                        "Contraseña: " + contrasena + "\n\n" +
                        "Recomendamos cambiar la contraseña al iniciar sesión.")
                .setPositiveButton("Guardar en Notas", (dialog, which) -> {
                    // Abrir selector de apps priorizando Notas
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, textoNotas);
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Mis credenciales Air_o_Walk");
                    startActivity(Intent.createChooser(shareIntent, "Guardar en Notas, WhatsApp o Email"));
                })
                .setNegativeButton("Continuar", (dialog, which) -> {
                    // Redirigir al login
                    Intent intent = new Intent(RegistroActivity.this, LoginActivity.class);
                    intent.putExtra("usuario", usuario);
                    intent.putExtra("contrasena", contrasena);
                    startActivity(intent);
                    finish();
                })
                .setNeutralButton("Copiar al portapapeles", (dialog, which) -> {
                    // Copiar al portapapeles
                    android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Credenciales", textoNotas);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Credenciales copiadas al portapapeles", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
    }



}