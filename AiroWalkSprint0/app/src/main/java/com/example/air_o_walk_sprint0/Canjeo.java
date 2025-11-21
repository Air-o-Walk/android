package com.example.air_o_walk_sprint0;
public class Canjeo {
    private int id;
    private String couponCode;
    private String redemptionDate;
    private String prizeName;
    private String description;
    private int pointsRequired;

    // Constructor vacío
    public Canjeo() {
    }

    // Constructor completo
    public Canjeo(int id, String couponCode, String redemptionDate,
                      String prizeName, String description, int pointsRequired) {
        this.id = id;
        this.couponCode = couponCode;
        this.redemptionDate = redemptionDate;
        this.prizeName = prizeName;
        this.description = description;
        this.pointsRequired = pointsRequired;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public String getRedemptionDate() {
        return redemptionDate;
    }

    public String getPrizeName() {
        return prizeName;
    }

    public String getDescription() {
        return description;
    }

    public int getPointsRequired() {
        return pointsRequired;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public void setRedemptionDate(String redemptionDate) {
        this.redemptionDate = redemptionDate;
    }

    public void setPrizeName(String prizeName) {
        this.prizeName = prizeName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPointsRequired(int pointsRequired) {
        this.pointsRequired = pointsRequired;
    }

    @Override
    public String toString() {
        return "Redemption{" +
                "id=" + id +
                ", couponCode='" + couponCode + '\'' +
                ", prizeName='" + prizeName + '\'' +
                ", pointsRequired=" + pointsRequired +
                '}';
    }
}