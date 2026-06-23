# Notification SDK

This SDK allows you to easily show custom notifications in your Android app.

## Setup

### 1. Add JitPack repository to your root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add the dependency to your app `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.YOUR_GITHUB_USERNAME:NotificationApp:1.0.0")
}
```

## Usage

### Initialize the SDK

In your `MainActivity` or `Application` class:

```java
NotificationSDK.init(this);
```

### Show a Notification

```java
NotificationSDK.showNotification(
    context, 
    "Title", 
    "Message", 
    MainActivity.class, 
    R.drawable.your_icon
);
```
