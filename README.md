# Period App

Android app (Kotlin) to track your period and estimate ovulation. Built with **Jetpack Compose**, **Material 3**, and **DataStore**.

## Setup

1. Open the project in **Android Studio** (Hedgehog or newer recommended).
2. Let Android Studio sync Gradle and download the wrapper if prompted.
3. Run on an emulator or device (API 26+).

## First run: onboarding wizard

- **Step 1:** When did your last period start? (one question, date picker)
- **Step 2:** When did your last period end? (one question, date picker; end ≥ start)
- **Step 3:** Summary with typical period length and “Get started”

After completion, the app opens the main screen. On later launches you go straight to the main app.

## Stack

- **Kotlin** + **Coroutines** & **Flow**
- **Jetpack Compose** + **Material 3**
- **Navigation Compose**
- **ViewModel** + **StateFlow**
- **DataStore** (Preferences) for onboarding state and period data

## Project structure

- `app/src/main/java/com/periodapp/`
  - `MainActivity.kt` – single Activity, Compose setContent
  - `ui/theme/` – Material 3 theme (rose/sage palette)
  - `ui/navigation/` – NavHost, start → wizard or main
  - `ui/wizard/` – WizardViewModel, WizardScreen (3 steps)
  - `ui/main/` – MainScreen (placeholder for calendar/ovulation)
  - `data/` – OnboardingDataStore
