# VW Smart TV IR Remote

A lightweight Android app that turns compatible phones with an infrared blaster into a remote control for supported VW Smart TV models.

## Features

- Power, Home, Menu, Source and navigation controls
- Volume, mute and channel controls
- Configurable carrier frequency
- Configurable device address
- NEC-style infrared transmission
- Simple remote-focused Android UI

## Tech stack

- Kotlin
- Android SDK
- Android `ConsumerIrManager`
- View Binding
- Material Components
- Gradle

## Architecture

```text
User action
    ↓
Android UI
    ↓
ConsumerIrManager
    ↓
IR carrier + command
    ↓
VW Smart TV
```

## Build

```bash
./gradlew assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Compatibility

The phone must have a **hardware IR blaster**. The app was designed around supported VW Smart TV profiles such as VW32C3, VW24C3 and VW43S1.

## Why I built it

A small hardware-integrated project exploring how Android applications can interact with physical devices through infrared communication.

## Future improvements

- Multiple TV profiles
- Saved remote presets
- Automatic device/profile detection
- Haptic feedback
- UI customization
