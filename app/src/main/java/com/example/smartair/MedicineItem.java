package com.example.smartair;

public class MedicineItem {
    private String id;
    private String name;
    private String purchaseDate;
    private String expiryDate;
    private int totalDose;
    private int remainingDose;

    public MedicineItem() {}  // Firestore requirement

    public MedicineItem(String id,
                        String name,
                        String purchaseDate,
                        String expiryDate,
                        int totalDose,
                        int remainingDose) {
        this.id = id;
        this.name = name;
        this.purchaseDate = purchaseDate;
        this.expiryDate = expiryDate;
        this.totalDose = totalDose;
        this.remainingDose = remainingDose;
    }


    public String getId() { return id; }

    public String getName() { return name; }

    public String getPurchaseDate() { return purchaseDate; }

    public String getExpiryDate() { return expiryDate; }

    public int getTotalDose() { return totalDose; }

    public int getRemainingDose() { return remainingDose; }

    public double getPercentage() {
        if (totalDose == 0) return 0;
        return (remainingDose * 1.0 / totalDose) * 100;
    }

    public void setId(String id) {
        this.id = id;
    }
}
