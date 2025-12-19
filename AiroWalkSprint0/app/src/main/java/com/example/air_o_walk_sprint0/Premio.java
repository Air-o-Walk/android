package com.example.air_o_walk_sprint0;

/**
 * @class Premio
 * @brief Modelo de datos que representa un premio canjeable en la aplicación.
 *
 * Esta clase encapsula toda la información de un premio que los usuarios
 * pueden canjear con sus puntos acumulados:
 * - Información básica (id, nombre, descripción)
 * - Requisitos de canje (puntos necesarios)
 * - Control de inventario (cantidad disponible e inicial)
 * - Estado de activación
 *
 * Incluye métodos auxiliares para verificar la disponibilidad del premio.
 *
 * @author Santiago Aguirre
 * @version 1.0
 */
public class Premio {
    private int id;
    private String nombre;
    private String descripcion;
    private int puntosRequeridos;
    private int cantidadDisponible;
    private int cantidadInicial;
    private int activo;

    // --------------------------------------------------------------
    // Constructor vacío
    // Descripción: Crea un objeto Premio sin inicializar sus atributos.
    //              Útil para frameworks de mapeo JSON.
    // Diseño: new Premio()
    // --------------------------------------------------------------
    public Premio() {
    }

    // --------------------------------------------------------------
    // Constructor completo
    // Descripción: Crea un objeto Premio inicializando todos sus atributos.
    // Parámetros: - id : identificador único del premio
    //             - nombre : nombre del premio
    //             - descripcion : descripción detallada del premio
    //             - puntosRequeridos : puntos necesarios para canjear
    //             - cantidadDisponible : unidades disponibles actualmente
    //             - cantidadInicial : unidades totales del premio
    //             - activo : estado de activación (1=activo, 0=inactivo)
    // Diseño: (id, nombre, ..., activo) -> new Premio()
    // --------------------------------------------------------------
    public Premio(int id, String nombre, String descripcion, int puntosRequeridos,
                  int cantidadDisponible, int cantidadInicial, int activo) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.puntosRequeridos = puntosRequeridos;
        this.cantidadDisponible = cantidadDisponible;
        this.cantidadInicial = cantidadInicial;
        this.activo = activo;
    }

    // -----------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getPuntosRequeridos() {
        return puntosRequeridos;
    }

    public int getCantidadDisponible() {
        return cantidadDisponible;
    }

    public int getCantidadInicial() {
        return cantidadInicial;
    }

    public int getActivo() {
        return activo;
    }

    // -----------------------------------------------------------------
    // Setters
    // -----------------------------------------------------------------

    public void setId(int id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setPuntosRequeridos(int puntosRequeridos) {
        this.puntosRequeridos = puntosRequeridos;
    }

    public void setCantidadDisponible(int cantidadDisponible) {
        this.cantidadDisponible = cantidadDisponible;
    }

    public void setCantidadInicial(int cantidadInicial) {
        this.cantidadInicial = cantidadInicial;
    }

    public void setActivo(int activo) {
        this.activo = activo;
    }

    // -----------------------------------------------------------------
    // Métodos auxiliares
    // -----------------------------------------------------------------

    // --------------------------------------------------------------
    // estaActivo()
    // Descripción: Verifica si el premio está activo en el sistema.
    // Diseño: estaActivo() -> boolean
    // --------------------------------------------------------------
    public boolean estaActivo() {
        return activo > 0;
    }

    // --------------------------------------------------------------
    // tieneStock()
    // Descripción: Verifica si quedan unidades disponibles del premio.
    // Diseño: tieneStock() -> boolean
    // --------------------------------------------------------------
    public boolean tieneStock() {
        return cantidadDisponible > 0;
    }

    // --------------------------------------------------------------
    // estaDisponible()
    // Descripción: Verifica si el premio está disponible para canjear.
    //              Un premio está disponible si está activo Y tiene stock.
    // Diseño: estaDisponible() -> estaActivo() AND tieneStock() -> boolean
    // --------------------------------------------------------------
    public boolean estaDisponible() {
        return estaActivo() && tieneStock();
    }

    // --------------------------------------------------------------
    // toString()
    // Descripción: Genera una representación en String del objeto Premio
    //              con sus atributos principales.
    // Diseño: toString() -> String
    // --------------------------------------------------------------
    @Override
    public String toString() {
        return "Premio{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", puntosRequeridos=" + puntosRequeridos +
                ", cantidadDisponible=" + cantidadDisponible +
                ", activo=" + activo +
                '}';
    }
}