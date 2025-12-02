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

* **Location:** `app/src/test/java/com/example/smartair/`
* **Frameworks:** JUnit 4, Mockito.
* **Coverage:** Tests validate success/failure scenarios in the `LoginPresenter`.
* **How to run:** Right-click the `test` folder in Android Project View and select **"Run Tests in 'test'"**.

---

---

## License & Acknowledgements

* Developed for CSCB07 at UTSC.
