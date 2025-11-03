# GPS Navigation for Agriculture

Android app that helps farmers manage and navigate their fields using **Mapbox** maps.  
Users can register, view their fields, and track agricultural activities easily.

## 🌾 Features
- User login & registration (Firebase Authentication)
- Mapbox map view with colored field boundaries
- Add, edit, delete, and search fields
- Automatic area calculation (in hectares)
- Track activities (sowing, spraying, fertilizing)
- Navigation drawer with user info and quick actions

## 🧩 Tech Stack
- Android Studio (Java)
- Firebase Authentication & Realtime Database
- Mapbox Maps SDK

## 🚀 Setup
1. Clone this repository
2. Open in Android Studio
3. Add:
    google-services.json → inside /app
    Mapbox token in local.properties and in settings.gradle
    Sync Gradle and run the app