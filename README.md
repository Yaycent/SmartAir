# SMART AIR - Asthma Management App for Children

**SMART AIR** is a kid-friendly Android application designed to help children (ages 6-16) manage asthma, log symptoms, practice good inhaler technique, and allow parents to monitor their health status and share reports with healthcare providers.

> **Course:** CSCB07
> **Term:** Fall 2025
> **Group:** 66

---

## Team Members

| Student Name | GitHub User | Primary Role / Key Contributions |
| :--- | :--- | :--- |
| **Judy Xu** | **Yaycent** |**Scrum Master** - Auth, Parent/Child Management, MVP Architecture, Parent Dashboard |
| **Jiayi Qi** | **qijiayi28-glitch** |-Data Visualization (Charts), Inventory, Notifications |
| **Rapheal Wang** | **Anony13531** |- Symptom Tracking, Daily Check-in|
| **Tan Ngo** | **TanDatNgo28** |-  Safety/PEF Logic, Zone Calculation, Technique Helper, Gamification |
| **Zhan Tian** | **zhantian0201** |- Triggers, Medication Logs, Onboarding, Triage System|

---

## Tech Stack & Architecture

* **Language:** Java
* **IDE:** Android Studio
* **Database & Backend:** Firebase Firestore & Firebase Authentication
* **Architecture:**
    * Standard MVC for general activities.
    * **Model-View-Presenter (MVP)** pattern applied to the **Login Module** for decoupling and testability.
* **Testing:** JUnit 4 & Mockito (Unit tests for Login Presenter).

---

## Key Features (Requirements Implemented)

### R1. Accounts & Roles
* **Multi-Role Support:** Distinct login flows and home screens for **Parents**, **Children**, and **Providers**.
* **Child Accounts:** Parents can create and manage multiple child profiles under a single parent account.

### R2. Linking & Sharing
* **Provider Connection:** Parents can generate a 7-day invite code to link with a healthcare provider.
* **Granular Privacy Control:** Parents can toggle sharing permissions (e.g., share meds but hide symptoms) in real-time.

### R3. Medicines & Motivation
* **Medication Logging:** Tracks Rescue vs. Controller meds with timestamp and dosage.
* **Technique Helper:** Provides guidance for proper inhaler use.
* **Inventory Tracking:** Automatically decrements inventory and alerts parents when medication is low (≤20%).

### R4. Safety & Triage (Critical)
* **Zone Calculation:** Automatically calculates Green/Yellow/Red zones based on the child's Personal Best (PB) PEF value.
* **One-Tap Triage:** An emergency flow that assesses danger signs (e.g., blue lips, inability to speak) and directs users to call 911 or start home care.
* **Alerts:** Detects "Rapid Rescue Repeats" (≥3 uses in 3 hours) and triggers alerts.

### R5. Symptoms & History
* **Daily Check-in:** Logs night waking, activity limits, and cough/wheeze.
* **Trigger Tracking:** Records environmental triggers (e.g., Cold Air, Pets, Smoke).

### R6. Dashboard & Reports
* **Parent Dashboard:** Visualizes "Today's Zone", medication usage trends, and active alerts.
* **Data Visualization:** Interactive charts showing PEF trends over 7/30 days.
* **Report Export:** Generates PDF summaries for healthcare providers.

---

## Setup & Installation Instructions (For TA)

**IMPORTANT:** This application requires the `google-services.json` file to connect to the Firebase backend.
    * The `google-services.json` file is **included** in the repository inside the `app/` folder.
    * *Note: If for some reason the file is missing, please contact us or place the provided json file into the `app/` directory.*

---

## Testing

We have implemented Unit Tests for the **Login Module** following the MVP architecture requirements.

---

# Code Documentation & Architecture

This section provides a high-level overview of the key classes and modules implemented in the SMART AIR application.

## Architecture: MVP (Model-View-Presenter)
We refactored the **Login Module** to follow the MVP pattern to improve testability and separation of concerns.

### Login Module
* **`LoginActivity.java` (View)**
    * **Description:** Handles the user authentication process for Parents and Providers.
    * **Responsibilities:** Captures user input, updates UI states (loading/error), and handles navigation routing based on the Presenter's decision.
* **`LoginPresenter.java` (Presenter)**
    * **Description:** The "brain" of the login process. Decouples business logic from the UI.
    * **Responsibilities:** Validates input, communicates with Firebase Auth/Firestore models, validates User Roles (Security), and commands the View to update.
* **`LoginContract.java`**
    * **Description:** Interface definition that enforces the contract between View and Presenter.

---

## Core Activities (Screens)

### Dashboards
* **`ParentDashboardActivity.java`**
    * **Description:** The central control hub for Parents (Requirement R6).
    * **Key Features:**
        * **Visualizations:** Displays PEF trends using MPAndroidChart (7/30 days).
        * **Inventory:** Tracks medication stock and displays "Low Stock" alerts.
        * **Alerts:** Shows real-time "Red Zone" or "Rapid Rescue" warnings via `ParentAlertHelper`.
* **`ChildDashboardActivity.java`**
    * **Description:** A kid-friendly interface with large buttons for logging health data (Requirement R3, R4).
    * **Key Features:** One-tap logging for Rescue/Controller meds, PEF recording entry point, and Gamification (Streaks/Badges) display.
* **`ProviderDashboardActivity.java`**
    * **Description:** The dashboard for Healthcare Providers (Requirement R2, R6).
    * **Key Features:** Supports patient linking via invite codes and displays shared patient data based on granular permission settings.

### Data Logging & History
* **`RecordPEFFeature.java`**
    * **Description:** Handles the recording and analysis of Peak Flow values (Requirement R4).
    * **Logic:** Automatically calculates the "Asthma Zone" (Green/Yellow/Red) based on the child's Personal Best (PB) and triggers alerts if in the Red Zone.
* **`SymptomHistoryActivity.java`**
    * **Description:** A history browser for symptoms and logs (Requirement R5).
    * **Features:** Supports filtering by date/symptom type and exporting data to PDF/CSV.
* **`EmergencyMedicationActivity.java`**
    * **Description:** Logs rescue medication usage with context (Pre/Post feeling). Checks for "Worse after dose" conditions.

### Safety & Triage (SOS)
* **`SOSTriageActivity.java`**
    * **Description:** Phase 1 of the "One-Tap Triage" system. Assesses critical "Red Flag" symptoms (e.g., blue lips) and directs to 911 if necessary.
* **`SOSSecondaryActivity.java`**
    * **Description:** Phase 2 of Triage. Handles Action Plan guidance and the 10-minute safety countdown timer.

---

### Managers & Helpers
* **`ParentAlertHelper.java`**
    * **Description:** Centralized logic for generating safety alerts (Requirement R4 & R6).
    * **Logic:** Monitors data for specific conditions: Red Zone PEF, Rapid Rescue Repeats (≥3 in 3 hours), and Inventory Low/Expired.
* **`RescueUsageManager.java`**
    * **Description:** Background manager for Rescue Medication.
    * **Logic:** Listens for new logs to automatically deduct inventory count and triggers the "Rapid Rescue" check.
* **`MyFirebaseMessagingService.java`**
    * **Description:** Handles incoming FCM (Firebase Cloud Messaging) notifications to ensure parents receive alerts even when the app is in the background.
 

## License & Acknowledgements

* Developed for CSCB07 at UTSC.
