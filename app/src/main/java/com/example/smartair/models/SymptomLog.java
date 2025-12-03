package com.example.smartair.models;

import java.util.List;
/**
 * SymptomLog.java
 * <p>
 * Data model representing a single record of a symptom event (Requirement R5).
 * Encapsulates specific asthma control indicators and environmental triggers.
 * </p>
 * <b>Key Fields:</b>
 * <ul>
 * <li><b>Core Symptoms:</b> Boolean flags for Night Waking, Activity Limitations, and Cough/Wheeze.</li>
 * <li><b>Triggers:</b> A list of strings identifying environmental factors (e.g., Cold Air, Smoke).</li>
 * <li><b>Author:</b> Tracks whether the log was created by the "Child" or "Parent".</li>
 * </ul>
 *
 * @author Rphael Wang
 * @version 1.0
 */
public class SymptomLog {

    private String childUid;
    private long timestamp;
    private String author;

    private boolean nightWaking;
    private boolean activityLimit;
    private boolean coughWheeze;

    private List<String> triggers;

    public SymptomLog() {}

    public SymptomLog(String childUid,
                      long timestamp,
                      String author,
                      boolean nightWaking,
                      boolean activityLimit,
                      boolean coughWheeze,
                      List<String> triggers) {
        this.childUid = childUid;
        this.timestamp = timestamp;
        this.author = author;
        this.nightWaking = nightWaking;
        this.activityLimit = activityLimit;
        this.coughWheeze = coughWheeze;
        this.triggers = triggers;
    }

    // Getters and Setters
    public String getChildUid() {
        return childUid;
    }

    public void setChildUid(String childUid) {
        this.childUid = childUid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public boolean isNightWaking() {
        return nightWaking;
    }

    public void setNightWaking(boolean nightWaking) {
        this.nightWaking = nightWaking;
    }

    public boolean isActivityLimit() {
        return activityLimit;
    }

    public void setActivityLimit(boolean activityLimit) {
        this.activityLimit = activityLimit;
    }

    public boolean isCoughWheeze() {
        return coughWheeze;
    }

    public void setCoughWheeze(boolean coughWheeze) {
        this.coughWheeze = coughWheeze;
    }

    public List<String> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<String> triggers) {
        this.triggers = triggers;
    }
}
