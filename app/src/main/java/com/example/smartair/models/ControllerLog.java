package com.example.smartair.models;
/**
 * ControllerLog.java
 * <p>
 * Data model representing a single record of Controller medication usage (Requirement R3).
 * Used primarily to track daily adherence and calculate gamification streaks.
 * </p>
 * <b>Key Fields:</b>
 * <ul>
 * <li><b>parentUid/childUid:</b> Identifies the user associated with this log.</li>
 * <li><b>date:</b> The timestamp of the medication intake, used for chronological sorting and streak logic.</li>
 * </ul>
 *
 * @author Tan Ngo
 * @version 1.0
 */
public class ControllerLog {
    private String parentUid;
    private String childUid;
    private long date;

    public ControllerLog() {}

    public ControllerLog(String parentUid, String childUid, long date) {
        this.parentUid = parentUid;
        this.childUid = childUid;
        this.date = date;
    }

    public String getParentUid() {
        return parentUid;
    }

    public String getChildUid() {
        return childUid;
    }

    public long getDate() {
        return date;
    }
}
