package com.example.smartair;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MedicineItem {
    private String id;
    private String name;
    private String purchaseDate;
    private String expiryDate;
    private int totalDose;
    private int remainingDose;
    private String parentUid;
    private String medType;     // "Rescue" or "Controller"
    private int dosePerUse;    // For controller, rescue = always 1

    public MedicineItem() {}  // Firestore requirement
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public String getPurchaseDate() { return purchaseDate; }
    public String getExpiryDate() { return expiryDate; }
    public int getTotalDose() { return totalDose; }
    public int getRemainingDose() { return remainingDose; }
    public String getParentUid() { return parentUid; }
    public String getMedType() { return medType; }
    public int getDosePerUse() { return dosePerUse; }

    public void setName(String name) { this.name = name; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    public void setTotalDose(int totalDose) { this.totalDose = totalDose; }
    public void setRemainingDose(int remainingDose) { this.remainingDose = remainingDose; }
    public void setParentUid(String parentUid) { this.parentUid = parentUid; }
    public void setMedType(String medType) { this.medType = medType; }
    public void setDosePerUse(int dosePerUse) { this.dosePerUse = dosePerUse; }

    public double getPercentage() {
        if (totalDose == 0) return 0;
        return (remainingDose * 1.0 / totalDose) * 100;
    }

    public boolean isExpiringSoon() {
        if (expiryDate == null || expiryDate.isEmpty()) {
            return false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date dateExpiry = sdf.parse(expiryDate);
            Date dateNow = new Date();

            // 计算差值
            long diffInMillies = dateExpiry.getTime() - dateNow.getTime();
            long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

            // 如果剩余天数小于 30 天，且还没有过期（大于0），或者是已经过期了
            // 这里逻辑是：只要少于30天（包括负数即已过期）都算 expiring soon
            return diffInDays <= 30;

        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isReplacementNeeded() {
        return getPercentage() <= 20 || isExpiringSoon();
    }

}
