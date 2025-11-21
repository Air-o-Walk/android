package com.example.air_o_walk_sprint0;
public class Premios {
    private int id;
    private String name;
    private String description;
    private int pointsRequired;
    private int quantityAvailable;
    private int initialQuantity;
    private int active;

    // Constructor vacío
    public Premios() {
    }

    // Constructor completo
    public Premios(int id, String name, String description, int pointsRequired,
                 int quantityAvailable, int initialQuantity, int active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.pointsRequired = pointsRequired;
        this.quantityAvailable = quantityAvailable;
        this.initialQuantity = initialQuantity;
        this.active = active;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getPointsRequired() {
        return pointsRequired;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }

    public int getInitialQuantity() {
        return initialQuantity;
    }

    public int getActive() {
        return active;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPointsRequired(int pointsRequired) {
        this.pointsRequired = pointsRequired;
    }

    public void setQuantityAvailable(int quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }

    public void setInitialQuantity(int initialQuantity) {
        this.initialQuantity = initialQuantity;
    }

    public void setActive(int active) {
        this.active = active;
    }

    // Métodos auxiliares útiles
    public boolean isActive() {
        return active > 0;
    }

    public boolean hasStock() {
        return quantityAvailable > 0;
    }

    public boolean isAvailable() {
        return isActive() && hasStock();
    }

    @Override
    public String toString() {
        return "Prize{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", pointsRequired=" + pointsRequired +
                ", quantityAvailable=" + quantityAvailable +
                ", active=" + active +
                '}';
    }
}