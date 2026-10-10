# Glip Architecture Documentation

## 1. Overview
Glip is architected around clean **MVVM (Model-View-ViewModel)** separation and unidirectional data flow (UDF) powered by Jetpack Compose and Kotlin Coroutines/Flow.

```
+-------------------------------------------------------------+
|                         UI Layer                            |
|  - Jetpack Compose + Material 3 Screens                     |
|  - RegistrationScreen, HomeScreen, ServicesScreen,          |
|    ProfileScreen, InteractiveDemoMap                        |
+-------------------------------------------------------------+
                              | Observes StateFlow / Dispatches Events
                              v
+-------------------------------------------------------------+
|                      ViewModel Layer                        |
|  - RegistrationViewModel, HomeViewModel,                    |
|    ServicesViewModel, ProfileViewModel                      |
+-------------------------------------------------------------+
                              | Invokes Use Cases & Repositories
                              v
+-------------------------------------------------------------+
|                       Domain Layer                          |
|  - Models: Vehicle, Route, SeatAvailability, DriverProfile, |
|            UserProfile, LocalPromotion                      |
|  - Use Cases: RouteMatcher, OtpVerificationManager,        |
|               PhoneValidator, EmailValidator                |
|  - Repository Interfaces: UserRepository, MobilityRepository|
+-------------------------------------------------------------+
                              | Implemented by
                              v
+-------------------------------------------------------------+
|                        Data Layer                           |
|  - UserRepositoryImpl (backed by SharedPreferences Flow)     |
|  - DemoMobilityRepositoryImpl (backed by DemoDataStore)     |
|  - Future: SupabaseMobilityRepositoryImpl / SupabaseUserRepo |
+-------------------------------------------------------------+
```

## 2. Directory Structure
```
app/src/main/java/com/glip/app/
├── GlipApplication.kt             # Application entry point
├── MainActivity.kt                # Single Activity host with edge-to-edge support
├── navigation/
│   ├── Screens.kt                 # Sealed routes
│   └── AppNavigation.kt           # Bottom bar scaffold and screen switcher
├── domain/
│   ├── model/
│   │   └── Models.kt              # Business entities (Vehicle, Route, SeatAvailability, etc.)
│   ├── repository/
│   │   └── Repositories.kt        # Abstract repository contracts
│   └── usecase/
│       ├── Validators.kt          # Indian mobile & email validation + phone masking
│       ├── OtpVerificationManager.kt # Demo OTP security limits, cooldown & generation
│       └── RouteMatcher.kt        # Corridor matching algorithm
├── data/
│   ├── demo/
│   │   └── DemoDataStore.kt       # Simulated routes, vehicles, stops, and promotions
│   ├── local/
│   │   └── DemoProfilePreferences.kt # Local persistent session storage
│   └── repository/
│       └── RepositoryImpls.kt     # Concrete repositories
└── ui/
    ├── theme/
    │   ├── Color.kt               # Glip Green and Saffron brand colors
    │   ├── Type.kt                # Material 3 typography
    │   └── Theme.kt               # Dark/Light MaterialTheme
    ├── components/
    │   └── CommonComponents.kt   # Brand header, badges, common pills
    ├── registration/
    │   ├── RegistrationViewModel.kt
    │   └── RegistrationScreen.kt
    ├── home/
    │   ├── HomeViewModel.kt
    │   ├── InteractiveDemoMap.kt  # Custom vector map canvas
    │   └── HomeScreen.kt
    ├── services/
    │   ├── ServicesViewModel.kt
    │   └── ServicesScreen.kt
    └── profile/
        ├── ProfileViewModel.kt
        └── ProfileScreen.kt
```

## 3. Key Design Decisions
- **Custom Vector Map**: Avoids dependence on third-party tile servers that can throttle or fail offline. Renders route polylines, stops, and moving vehicles with pinch-to-zoom, pan, and tap selection.
- **Repository Pattern**: All data operations are behind interfaces (`UserRepository`, `MobilityRepository`), making future integration of Supabase or remote APIs a drop-in replacement without touching the UI layer.
- **Privacy First**: Sensitive contact information is masked (e.g. `98******10`, `DL 01 ** 4812`), and zero permissions are requested for demo viewing.
