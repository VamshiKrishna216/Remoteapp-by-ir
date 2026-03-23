# VW Smart TV IR Remote APK

A minimal Android app for phones with an IR blaster. It targets VW Smart TV models such as **VW32C3**, **VW24C3**, and **VW43S1** with a preset button layout inspired by common non-voice K2-style remotes.

## Features

- IR remote UI with power, home, menu, source, navigation, volume, mute, and channel buttons.
- Uses Android's `ConsumerIrManager` to send NEC-style infrared frames.
- Lets you edit the carrier frequency and device address if your TV needs a different preset.

## Build

```bash
./gradlew assembleDebug
```

The generated APK will be at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Notes

- Your Android phone **must** have a hardware IR blaster.
- The default profile uses a common NEC-style TV address (`0x20DF`) and common TV command values. If your specific VW TV responds to a different address, edit the field inside the app and test again.
