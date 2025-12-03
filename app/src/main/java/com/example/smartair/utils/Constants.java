package com.example.smartair.utils;
/**
 * Constants.java
 * <p>
 * Utility class containing global static constants used throughout the application.
 * Centralizes key strings to prevent typos and ensure consistency across modules.
 * </p>
 * <b>Key Categories:</b>
 * <ul>
 * <li><b>Intent Keys:</b> Identifiers for passing data (UIDs, Names) between Activities.</li>
 * <li><b>User Roles:</b> Standardized strings for Parent, Child, and Provider roles (Requirement R1).</li>
 * <li><b>SOS Defaults:</b> Default guidance text for Green/Yellow/Red zones in the Action Plan (Requirement R4).</li>
 * </ul>
 *
 * @author Judy Xu, Zhan Tian
 * @version 3.0
 */
public class Constants {
    public static final String CHILD_UID = "CHILD_UID";
    public static final String CHILD_NAME = "CHILD_NAME";
    public static final String PARENT_UID = "PARENT_UID";
    public static final String MED_TYPE = "MED_TYPE";
    public static final String MEDICINE_ID = "medicineId";

    // role
    public static final String KEY_ROLE = "ROLE";
    public static final String ROLE_PARENT = "Parent";
    public static final String ROLE_DOCTOR = "Provider";
    public static final String ROLE_CHILD = "Child";

    // sos
    public static final String DEFAULT_GREEN = "Keep normal medication.";
    public static final String DEFAULT_YELLOW = "Take rescue meds 2-4 puffs. Wait 10 mins.";
    public static final String DEFAULT_RED = "Call 911 immediately.";
}
