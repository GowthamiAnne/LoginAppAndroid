# Android Mobile Login Functionality - Project Explanation

## Overview

This Android application demonstrates a simple but robust login feature using modern Android development practices. The app features a clean user interface built with Jetpack Compose and follows the MVVM (Model-View-ViewModel) architectural pattern for clear separation of concerns.

The project also includes network connectivity monitoring to handle offline scenarios gracefully and comprehensive unit and UI tests.

---


## Architecture

The app follows the **MVVM architecture**, consisting of:

- **Model:** Data layer including repositories that manage data sources.
- **ViewModel:** Holds UI-related data and business logic, exposing state as `StateFlow` for Compose to observe.
- **View (UI):** Composable functions built with Jetpack Compose that observe the ViewModel state and render UI accordingly.

---

## Key Components

### 1. Login Screen (`LoginScreen.kt`)

- Uses Jetpack Compose to build the login UI.
- Displays username, password fields, a "Remember me" checkbox, and a login button.
- Shows error messages and loading states dynamically based on ViewModel state.

### 2. Login ViewModel (`LoginViewModel.kt`)

- Manages UI state (`uiState`) including username, password, loading, errors, and login enablement.
- Handles login logic, including offline checks using `NetworkMonitor`.
- Emits navigation events upon successful login.

### 3. Network Monitoring (`NetworkMonitor.kt`)

- Checks network connectivity using Android’s `ConnectivityManager`.
- Provides an online/offline status that the ViewModel observes to decide whether to allow login attempts.

### 4. Repositories

- Responsible for handling actual authentication calls (stubbed or real backend calls).
- The example uses `AuthRepository` to encapsulate login requests.

---

## Flow of the Login Feature

1. **User Input:**  
   User enters username and password, optionally checks "Remember me".

2. **Login Button Click:**  
   ViewModel triggers `login()` method.

3. **Network Check:**  
   Before attempting login, ViewModel consults `NetworkMonitor` to verify online status.

4. **Offline Handling:**  
   If offline, ViewModel updates state with an error message "No internet connection" and prevents login call.

5. **Authentication:**  
   If online, ViewModel calls the repository to authenticate.

6. **Result Handling:**  
   - On success: ViewModel emits navigation event; UI responds by navigating away.  
   - On failure: ViewModel updates state with error message.

---

## Testing

- **Unit Tests:**  
  Tests for ViewModel logic, especially for error and offline handling.

- **UI Tests:**  
  Compose UI tests simulate user input and verify UI state changes.

---

Feel free to explore the source code for detailed implementation, and use this as a foundation for building more complex, network-aware Android applications with robust user authentication flows.
