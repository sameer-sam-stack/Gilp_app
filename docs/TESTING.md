# Glip Testing Documentation

## 1. Unit Test Coverage
Unit tests located at `app/src/test/java/com/glip/app/` verify core business rules without requiring an Android device or emulator:

### `ValidationAndOtpTest.kt`
- Indian mobile number validation (+91, 0, 91, 10-digit formats).
- Rejection of invalid phone numbers (letters, wrong prefixes, incorrect lengths).
- Number sanitization and masking (`98******10`).
- Optional email validation.
- OTP generation randomness and 6-digit integrity.
- Verification success on matching code.
- Verification failure and attempt countdown on invalid code.
- Exhaustion after 3 failed attempts.
- Cooldown timer enforcement.

### `RouteMatcherAndDataTest.kt`
- `SeatAvailability` capacity calculation and overflow handling.
- Corridor matching for origin and destination queries.
- Intermediate stop matching along active vehicle routes.
- Empty result handling for unmatched routes.
- Simulated dataset validation (ratings, vehicle IDs, non-empty routes, promotions).

## 2. Manual Verification Checklist
1. **First Launch**: Shows Glip Registration screen.
2. **Form Validation**: Entering invalid phone numbers highlights error without proceeding.
3. **Demo OTP**: Clicking "Get Demo OTP" reveals simulated code and countdown.
4. **Verification**: Entering correct code navigates to Home screen.
5. **Session Persistence**: Closing and relaunching app opens directly to Home screen.
6. **Bottom Navigation**: Switching between Home, Services, and Profile maintains state.
7. **Interactive Map**: Panning, zooming, and tapping vehicle chips selects active vehicle cards.
8. **List Fallback**: Clicking "Show List Fallback" displays a clean scrollable list of vehicles.
9. **Services Screen**: Displays Ride Pooling action, Coming Soon cards, and local merchant offers.
10. **Profile Screen**: Displays user name, masked phone number, and support options.
11. **Logout & Clear Data**: Clicking "Log Out" or "Clear Demo Data" clears the profile and returns to Registration.
