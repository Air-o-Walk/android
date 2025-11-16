// LogicaEditarPerfilMock.java
package com.example.air_o_walk_sprint0;

import android.os.Handler;
import android.os.Looper;

public class LogicaEditarPerfilMock {
    private int userId;

    public LogicaEditarPerfilMock(int userId) {
        this.userId = userId;
    }

    public void actualizarUsername(String nuevoUsername, EditarCallback callback) {
        simularRespuesta("username", nuevoUsername, callback, true);
    }

    public void actualizarEmail(String nuevoEmail, EditarCallback callback) {
        simularRespuesta("email", nuevoEmail, callback, true);
    }

    public void actualizarPassword(String passwordActual, String nuevaPassword, EditarCallback callback) {
        // Simular validación de contraseña actual
        if ("123456".equals(passwordActual)) { // Contraseña "correcta" para pruebas
            simularRespuesta("password", "********", callback, true);
        } else {
            simularRespuesta("password", "", callback, false);
        }
    }

    private void simularRespuesta(String campo, String valor, EditarCallback callback, boolean exito) {
        // Simular delay de red (1 segundo)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (exito) {
                callback.onEdicionExitosa(campo, "✓ " + campo + " actualizado correctamente (MOCK)");
            } else {
                callback.onEdicionFallida(campo, "✗ Contraseña actual incorrecta (usar '123456')");
            }
        }, 1000);
    }

    // Misma interfaz que la clase real
    public interface EditarCallback {
        void onEdicionExitosa(String campo, String mensaje);
        void onEdicionFallida(String campo, String mensajeError);
    }
}