package com.example.smartair;

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
