# Universal IR Blaster Remote for Android

This project is now a broader **universal IR remote app** for Android phones that still include a hardware **IR blaster**. Instead of only targeting one VW TV preset, the app now includes a preset library for multiple appliance categories such as:

- TVs
- AC units
- Set-top boxes
- Streaming boxes
- Ceiling fans
- Projectors
- Audio devices / soundbars

## What the app does

- Lets you choose a **category**, **brand**, and **preset/model** from a built-in library.
- Supports both **NEC 16-bit** style frames and **Sony SIRC12** style frames.
- Exposes **carrier frequency**, **device address**, and **custom command** fields so you can tweak codes for brands or models not covered exactly by the bundled presets.
- Reuses one dynamic control grid that changes labels depending on whether you select a TV, AC, fan, projector, set-top box, or audio preset.

## Included preset examples

The preset library includes examples for:

- Samsung TVs
- LG TVs
- Sony Bravia TVs
- VW Linux frameless TVs
- Daikin ACs
- Voltas ACs
- Tata Play set-top boxes
- Mi streaming boxes
- Generic IR ceiling fans
- Epson projectors
- Sony soundbars / AV devices

## Build

```bash
./gradlew assembleDebug
```

Expected APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Important notes

- Your phone **must** have a real IR blaster for transmission to work.
- Appliance IR compatibility is highly brand/model specific. Many TVs use short NEC/SIRC-style commands, but some devices — especially AC remotes — often use longer stateful frames. This app focuses on practical preset testing and manual tuning rather than claiming perfect support for every device ever made.
- If a preset partly works, keep the same protocol/frequency and experiment with nearby device addresses or custom command hex values.
