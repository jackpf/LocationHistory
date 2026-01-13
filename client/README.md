# LocationHistory Client (Android)

A lightweight, battery-efficient Android application that 
syncs your device's location to your self-hosted server.

## Features
* **Background Tracking:** robust service that survives reboots.
* **Battery Efficient:** uses the WorkManager and Fused Location Provider to minimize wake-ups.
* **Failure tolerant:** uses gRPC for communication.

## Tech Stack
* **Platform:** Android (Native)
* **Language:** Java
* **Build System:** Gradle

## Setup & Installation

### Prerequisites
* Android Studio
* Android SDK
* Gradle

## Build & Run

### Local build

```bash
export ANDROID_HOME="${HOME}/Library/Android/sdk" # Example for MacOS, please adjust for your OS
make lint build-debug
```

### Running Locally

The easiest way to run locally is via Android Studio + Emulator.
