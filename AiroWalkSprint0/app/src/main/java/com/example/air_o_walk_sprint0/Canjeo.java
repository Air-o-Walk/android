package com.example.air_o_walk_sprint0;
/**
 * @class Canjeo
 * @brief Modelo de datos que representa un canjeo de premio.
 *
 * Esta clase encapsula la información asociada a un canjeo realizado
 * por un usuario, incluyendo el código del cupón, la fecha de redención,
 * el premio canjeado y los puntos requeridos.
 *
 * Se utiliza principalmente para transportar datos entre la capa
 * de comunicación con el backend y la interfaz de usuario.
 *
 * @author Santiago Aguirre
 * @version 1.0
 */
public class Canjeo {
    private int id;
    private String codigoCupon;
    private String fechaRedencion;
    private String nombrePremio;
    private String descripcion;
    private int puntosRequeridos;

    // Constructor vacío
    public Canjeo() {
    }

    // Constructor completo
    public Canjeo(int id, String codigoCupon, String fechaRedencion,
                     String nombrePremio, String descripcion, int puntosRequeridos) {
        this.id = id;
        this.codigoCupon = codigoCupon;
        this.fechaRedencion = fechaRedencion;
        this.nombrePremio = nombrePremio;
        this.descripcion = descripcion;
        this.puntosRequeridos = puntosRequeridos;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getCodigoCupon() {
        return codigoCupon;
    }

    public String getFechaRedencion() {
        return fechaRedencion;
    }

    public String getNombrePremio() {
        return nombrePremio;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getPuntosRequeridos() {
        return puntosRequeridos;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setCodigoCupon(String codigoCupon) {
        this.codigoCupon = codigoCupon;
    }

    public void setFechaRedencion(String fechaRedencion) {
        this.fechaRedencion = fechaRedencion;
    }

    public void setNombrePremio(String nombrePremio) {
        this.nombrePremio = nombrePremio;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setPuntosRequeridos(int puntosRequeridos) {
        this.puntosRequeridos = puntosRequeridos;
    }

    @Override
    public String toString() {
        return "Redencion{" +
                "id=" + id +
                ", codigoCupon='" + codigoCupon + '\'' +
                ", nombrePremio='" + nombrePremio + '\'' +
                ", puntosRequeridos=" + puntosRequeridos +
                '}';
    }
}