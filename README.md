# myq: garage door button for Wear OS

A one-button Wear OS app that opens and closes a garage door through
[Home Assistant](https://www.home-assistant.io/).

- Door closed: the button says **Open**. Door open: it says **Close**.
- Tap once to arm it (it turns red and says "Tap again"), then tap again within
  3 seconds to send the command. This stops a bumped wrist from moving the door.
- While the door is moving, the button says **Stop**.

## Why Home Assistant and not myQ directly

Chamberlain blocked unofficial access to the myQ cloud API in late 2023, so no
app can talk to myQ reliably anymore. The working setup is:

```
Watch  →  Home Assistant  →  ratgdo (wired to the opener)  →  door
```

[ratgdo](https://paulwieland.github.io/ratgdo/) is a small Wi-Fi board that
connects to the opener's wall-button terminals and appears in Home Assistant as
a `cover` entity. Any other controller that gives you a Home Assistant `cover`
entity works too.

## Setup

### 1. Home Assistant

1. Install ratgdo (or another controller) and confirm the door appears in Home
   Assistant, for example as `cover.garage_door`. Find the exact ID under
   Settings → Devices & services → Entities.
2. Create a token: click your user profile (bottom left) → Security →
   **Long-lived access tokens** → Create token. Copy it; it is only shown once.

### 2. Build the APK

Push to GitHub and the **CI** workflow builds it. Download `myq-garage-debug` from
the run's Artifacts. To build locally with Android Studio instead, open this
folder and run the `app` configuration.

### 3. Install on the watch

1. On the watch: Settings → System → About → tap **Build number** 7 times, then
   Developer options → turn on **ADB debugging** and **Debug over Wi-Fi**.
2. From your PC (the watch shows its IP and port):

```bash
adb connect WATCH_IP:PORT
```

```bash
adb install app-debug.apk
```

### 4. Point the app at Home Assistant

Use your Home Assistant address. A local IP is the most reliable choice, because
`homeassistant.local` often doesn't resolve on watches.

```bash
adb shell am start -n io.github.userxx09afk.myq/.SetupActivity --es url "http://192.168.1.50:8123" --es token "PASTE_TOKEN_HERE" --es entity "cover.garage_door"
```

The watch shows "Garage configured" and opens the app. Re-run the command any
time to change the settings. All three values are required every time.

## Notes

- **Network:** the watch must reach Home Assistant. On home Wi-Fi a local URL
  works. Away from home, or when the watch is connected only through the phone's
  Bluetooth, use a remote URL such as Home Assistant Cloud (Nabu Casa) or your
  own HTTPS address.
- **Token safety:** the token gives full control of Home Assistant. It is stored
  only in the app's private storage on the watch, never in this repo. For
  tighter control, create a separate Home Assistant user for the watch.
- The app checks the door state every 10 seconds while open, and every 2 seconds
  while the door is moving.
