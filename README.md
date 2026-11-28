# Dawarich

An unofficial [Dawarich](https://dawarich.app/) client for the Light Phone. It is not affiliated with Dawarich or The Light Phone.

The tool id is `com.jhellerstedt.dawarich`. The current version is in [`tool/lighttool.toml`](tool/lighttool.toml).

## What it does

You point the tool at your self-hosted Dawarich base URL (HTTP or HTTPS) and sign in from **Settings** (gear, upper right) by scanning the Account → API access QR code, or by pasting an API key. From the home screen you can:

- See the current GPS fix as a nearby street address when Dawarich reverse-geocoding works (coordinates as fallback)
- Home nests the **two nearest** cached places (tap through for sync / tag filter / star; filter blank shows at most two recent tags, typing shows autocomplete)
- Star / save a place at the current fix
- Manually upload the current fix as a timeline point, with an optional note and tag (tag suggestions come from place tags on the server)
- Auto-upload a point every 5 minutes while the tool is open (on by default; toggle in Settings)

Places are cached on the phone so the nearest list still opens offline. On the home screen the location fix and nearest preview refresh every **30 seconds** (`LocationHelper.HOME_POLL_INTERVAL`) to save battery and reverse-geocode traffic; Places and Upload still poll about every 2 seconds. There is no deep link into Light Directions; the tool shows address/coordinates and distance so you can open Directions yourself. The unofficial-client notice lives under **Settings → About**.

## What leaves the phone

The tool talks only to the Dawarich URL you enter. Requests send your API key as the `api_key` query parameter. Places you create and timeline points you upload go to that server.

The API key is stored on the phone, encrypted with the Android keystore. There is no analytics.

## HTTP vs HTTPS

Both schemes are allowed. The tool declares the `cleartext-http` capability so a LAN Dawarich on plain HTTP works. Prefer HTTPS when you have a certificate.

## Building

This checkout vendors [lightphone/light-sdk](https://github.com/lightphone/light-sdk) the same way [`light-zulip-tool`](https://github.com/jhellerstedt/light-zulip-tool) does. `sdkVersion` in [`gradle.properties`](gradle.properties) is the SDK a portal build will use.

```bash
scripts/sideload.sh              # debug APK, shared SDK dev key
scripts/sideload.sh release      # release APK
scripts/store-build-check.sh     # portal-shaped build, before you submit
```

`scripts/sideload.sh` builds, installs on a connected phone, and opens Dawarich. A release install uses `keystore.properties` when that file names a keystore, and the shared dev key otherwise:

```bash
keytool -genkeypair -v \
  -keystore "$HOME/keys/dawarich-release.jks" \
  -alias dawarich -keyalg RSA -keysize 2048 -validity 10000
cp keystore.properties.example keystore.properties
```

## Releases

A `v*` tag builds the release APK and attaches it to a GitHub Release. Bump `versionCode` and `versionName` in [`tool/lighttool.toml`](tool/lighttool.toml) first. The tag is `v` plus `versionName` (`versionName = "0.1.0"` becomes tag `v0.1.0`).

## Scaffold status

Still open:

- Confirm real GPS on a retail Light Phone (likely LightOS v592+)
- Dedicated Dawarich tags list API (today tags are collected from places)
- Background tracking (Light location lease is foreground-only)
- Richer timeline batching while walking
- Portal submission polish

## Light SDK

SDK notes live under [docs/](docs/). Upstream merges land on `main` when this tool needs a newer SDK.
