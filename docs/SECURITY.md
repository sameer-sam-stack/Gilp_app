# Haze Security and Privacy Guidelines

## 1. Zero Sensitive Permissions
The Haze MVP operates strictly without invasive permissions:
- **No Background Location**: Demo map relies on simulated corridor coordinates.
- **No SMS Permissions**: The app does not read or send SMS messages.
- **No Contact / Camera / Microphone Access**: Privacy-by-design.

## 2. Authentication & Demo OTP Safeguards
- **Random Client-Generated OTP**: Utilizes `java.security.SecureRandom` to generate a 6-digit code.
- **Attempt Limits**: Maximum 3 verification attempts permitted per OTP cycle.
- **Rate-Limiting Cooldown**: 30-second cooldown required before a new OTP can be requested.
- **Explicit Demo Labeling**: OTP is labeled as a local simulation. The app explicitly states that no SMS was sent.
- **No Hardcoded Values**: Never uses `123456` or fixed passwords.
- **One-Time Use**: The OTP is invalidated in memory immediately upon verification or exhaustion.

## 3. Data Storage & Privacy Protection
- **Local Persistence**: Only the minimal profile (Full Name, Phone Number, optional Email) is saved in app-private `SharedPreferences`.
- **Masking**: Phone numbers and license plates are masked on the UI (`98******10`, `DL 01 ** 4812`).
- **Data Erasure**: A "Clear Demo Data" button in the Profile screen completely deletes locally stored preferences.
- **No Secret Leakage**: No API keys, credentials, or secrets are committed to the codebase.
