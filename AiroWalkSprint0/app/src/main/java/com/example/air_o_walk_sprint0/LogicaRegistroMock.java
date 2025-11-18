package com.example.air_o_walk_sprint0;

import android.os.Handler;
import android.os.Looper;
import java.util.HashMap;

public class LogicaRegistroMock {

    /**
     * Simula la obtención de los ayuntamientos (GET).
     * @param callback Callback para manejar la respuesta.
     */
    public void obtenerAyuntamientosMock(AyuntamientosCallback callback) {
        // Simulamos un retraso en la obtención de los ayuntamientos, como si fuera una petición GET
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Simulamos la respuesta con un mapa de ayuntamientos
            HashMap<String, String> ayuntamientosMap = new HashMap<>();
            ayuntamientosMap.put("Madrid", "1");
            ayuntamientosMap.put("Barcelona", "2");
            ayuntamientosMap.put("Valencia", "3");

            // Llamamos al callback de éxito
            callback.onAyuntamientosObtenidos(ayuntamientosMap);
        }, 2000); // Simula un retraso de 2 segundos
    }

    /**
     * Simula el registro de un usuario (POST).
     * @param firstName Nombre del usuario
     * @param lastName Apellido del usuario
     * @param email Correo electrónico del usuario
     * @param dni DNI del usuario
     * @param phone Teléfono del usuario
     * @param townHallId ID del ayuntamiento
     * @param callback Callback para manejar la respuesta.
     */
    public void registrarUsuario(String firstName, String lastName, String email, String dni, String phone, String townHallId, RegistroCallback callback) {
        // Validación de campos
        if (firstName.isEmpty()) {
            callback.onRegistroFallido("✗ El nombre no puede estar vacío");
            return;
        }

        if (lastName.isEmpty()) {
            callback.onRegistroFallido("✗ El apellido no puede estar vacío");
            return;
        }

        if (email.isEmpty() || !email.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
            callback.onRegistroFallido("✗ El correo electrónico no es válido");
            return;
        }

        if (dni.length() != 9) { // Asumimos que el DNI tiene que tener 9 caracteres
            callback.onRegistroFallido("✗ El DNI debe tener 9 caracteres");
            return;
        }

        if (phone.isEmpty() || phone.length() < 9 || !phone.matches("[0-9]+")) { // Asumimos que el teléfono debe ser numérico y tener al menos 9 dígitos
            callback.onRegistroFallido("✗ El teléfono no es válido");
            return;
        }

        if (townHallId == null || townHallId.isEmpty()) {
            callback.onRegistroFallido("✗ Debes seleccionar un ayuntamiento");
            return;
        }

        // Simulamos un retraso en la "petición POST", como si estuviera esperando una respuesta del servidor
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Si todo es válido, simulamos un registro exitoso
            String jsonResponse = "{"
                    + "\"firstName\": \"" + firstName + "\","
                    + "\"lastName\": \"" + lastName + "\","
                    + "\"email\": \"" + email + "\","
                    + "\"dni\": \"" + dni + "\","
                    + "\"phone\": \"" + phone + "\","
                    + "\"townHallId\": \"" + townHallId + "\""
                    + "}";
            callback.onRegistroExitoso(jsonResponse);
        }, 2000); // Retraso simulado de 2 segundos
    }

    // Interfaz callback para manejar los resultados de la obtención de los ayuntamientos
    public interface AyuntamientosCallback {
        void onAyuntamientosObtenidos(HashMap<String, String> ayuntamientosMap);
        void onError(String mensajeError);
    }

    // Interfaz callback para manejar los resultados del registro
    public interface RegistroCallback {
        void onRegistroExitoso(String jsonResponse);
        void onRegistroFallido(String mensajeError);
    }
}
