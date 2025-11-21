package com.example.air_o_walk_sprint0;

public class Premio {
    private int id;
    private String nombre;
    private String descripcion;
    private int puntosRequeridos;
    private int cantidadDisponible;
    private int cantidadInicial;
    private int activo;

    // Constructor vacío
    public Premio() {
    }

    // Constructor completo
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

    // Getters
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

    // Setters
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

    // Métodos auxiliares útiles
    public boolean estaActivo() {
        return activo > 0;
    }

    public boolean tieneStock() {
        return cantidadDisponible > 0;
    }

    public boolean estaDisponible() {
        return estaActivo() && tieneStock();
    }

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