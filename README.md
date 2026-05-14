# Location Tracking Application with OpenStreetMap

An Android application that tracks the device's GPS coordinates in real time, sends them to a PHP/MySQL backend, and displays all recorded positions as map markers using the OSMDroid library.

---

## Overview

This project was built as part of a mobile development lab. The application:

- Requests location permissions at runtime
- Listens for GPS updates every 60 seconds or every 150 meters of movement
- Sends latitude, longitude, timestamp, and a device identifier to a local PHP server via HTTP POST
- Displays all previously recorded coordinates as pins on an interactive OpenStreetMap

---

## Tech Stack

| Layer | Technology |
|---|---|
| Mobile | Java, Android SDK 24+ |
| Map rendering | OSMDroid 6.1.17 (OpenStreetMap) |
| HTTP client | Volley 1.2.1 |
| Backend | PHP 8, PDO |
| Database | MySQL (via XAMPP) |
| Build system | Gradle with Kotlin DSL + Version Catalog |

---

## Project Structure

```
MapApplication/
├── app/src/main/
│   ├── java/com/example/mapapplication/
│   │   ├── MainActivity.java          # Permission handling, GPS tracking, HTTP POST
│   │   └── GoogleMapActivity.java     # OSMDroid map, fetches and plots pins
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml      # Launch screen with map button
│   │   │   └── activity_google_map.xml# Full-screen map view
│   │   ├── values/strings.xml         # App strings including GPS toast format
│   │   └── xml/
│   │       └── network_security_config.xml  # Allows HTTP to 10.0.2.2 (local dev)
│   └── AndroidManifest.xml
├── gradle/libs.versions.toml          # Centralized dependency versions
└── map_project/                       # PHP backend (deploy to htdocs/)
    ├── createPosition.php             # POST endpoint — saves a coordinate
    ├── getPosition.php                # POST endpoint — returns all coordinates
    └── setup.sql                      # Database and table creation script
```

---

## Setup

### Prerequisites

- Android Studio Hedgehog or newer
- XAMPP (Apache + MySQL) running on the host machine
- Android emulator or physical device running Android 7.0 (API 24)+
- PHP 8.0+

### Database

1. Start XAMPP and make sure Apache and MySQL are both running
2. Open phpMyAdmin at `http://localhost/phpmyadmin`
3. Go to the SQL tab and paste the contents of `map_project/setup.sql`
4. Click Go — this creates the `map_project` database and the `positions` table

```sql
CREATE TABLE `positions` (
  `id`        INT(11)     NOT NULL AUTO_INCREMENT,
  `latitude`  DOUBLE      NOT NULL,
  `longitude` DOUBLE      NOT NULL,
  `date`      DATETIME    NOT NULL,
  `imei`      VARCHAR(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

### Backend

1. Copy the `map_project/` folder into your XAMPP `htdocs/` directory

```
C:/xampp/htdocs/map_project/
```

2. Verify the two endpoints are reachable from your browser:

```
http://localhost/map_project/getPosition.php
http://localhost/map_project/createPosition.php
```

### Android App

1. Clone this repository and open it in Android Studio
2. Make sure `gradle.properties` contains:

```properties
android.useAndroidX=true
android.enableJetifier=true
```

3. Sync Gradle — all dependencies are defined in `gradle/libs.versions.toml`
4. Run the app on an emulator (Pixel 4a recommended)
5. Accept the location permission prompt
6. To simulate a GPS fix in the emulator: go to the three-dot menu > Extended Controls > Location, enter coordinates, and click Send

The app will POST the coordinates to `http://10.0.2.2/map_project/createPosition.php`.  
`10.0.2.2` is the emulator's alias for `localhost` on the host machine.

---

## How It Works

```
Android Device
     |
     | GPS fix (every 60s or 150m)
     v
MainActivity.java
     |
     | HTTP POST (Volley)
     | latitude, longitude, date, android_id
     v
createPosition.php  -->  MySQL: positions table
     
     
User taps "Open Live Map"
     |
     v
GoogleMapActivity.java
     |
     | HTTP POST (Volley)
     v
getPosition.php  -->  returns JSON array of all coordinates
     |
     v
OSMDroid MapView  -->  drops a pin for each coordinate
```

---

## Demo

<!-- Add your demo video link or embed below -->
<img width="1240" height="980" alt="image" src="https://github.com/user-attachments/assets/83c4460a-2342-49a8-8560-36ac87309cfc" />



https://github.com/user-attachments/assets/01bf3244-d54f-4b3c-b926-0aa3f574bfca






---

## Database Screenshot

<!-- Add your phpMyAdmin screenshot below -->
<img width="1059" height="160" alt="image" src="https://github.com/user-attachments/assets/1631d585-6e3a-494b-b7c9-7b513f9f7f6d" />

---

## Security Notes

- `ANDROID_ID` is used instead of IMEI — it requires no special permission and resets only on factory reset
- The network security config permits HTTP traffic only to `10.0.2.2` (local development server)
- PDO prepared statements are used on all SQL queries to prevent injection attacks
- For production: switch to HTTPS, move credentials to environment variables, and add API authentication
