# NotificationApp - Location Tracking Service

## Overview
This application provides continuous background location tracking with 1-minute logging intervals. It includes a foreground service to ensure tracking persists even when the app is in the background or the screen is off.

## Features
- **Accurate Logging**: 60 logs per hour (every 1 minute) saved to Database and Text File.
- **Custom Notifications**: Real-time status updates in the notification tray with a custom design.
- **Battery Management**: Prompts to disable battery saver for uninterrupted tracking.
- **Map Integration**: 2D/3D map views with zoom controls and marker support.
- **Dynamic API Key**: Option to provide your own Google Maps API key.
- **Permission Handling**: Advanced permission requests for Android 13+ (POST_NOTIFICATIONS) and background location.

## Setup & SDK Information
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36 (Android 15)
- **Compile SDK**: 36
- **Required Permissions**:
    - `ACCESS_FINE_LOCATION`
    - `ACCESS_COARSE_LOCATION`
    - `ACCESS_BACKGROUND_LOCATION` (Required for tracking when screen is off)
    - `POST_NOTIFICATIONS` (Android 13+)
    - `FOREGROUND_SERVICE_LOCATION`
    - `WAKE_LOCK`

## Performance Note
- Tracking frequency is optimized for 1-minute intervals.
- If the device heats up, ensure it is not in high-performance mode or charging while in a confined space.
- **Battery Saver** must be turned off to prevent the system from killing the background service.

## Usage
1. Open the app and grant all location permissions ("Allow all the time").
2. Ensure Battery Saver is OFF.
3. Tap "Start Service" in the Background Fetch activity to begin 1-minute logging.
4. Use the "Pause/Resume/Stop" buttons to manage logging.
5. View or Export logs via the "View Overnight Logs" button.
