# Security Policy

The Blue Player team and the Shapak-Apps organization take the security of our
software seriously. This document describes how to report vulnerabilities,
which versions we support, and the security principles built into the project.

## Supported Versions

We only support the latest released version of Blue Player. Security fixes are
landed on the `main` branch first and shipped in the next tagged release.

| Version            | Supported          |
| ------------------ | ------------------ |
| Latest release     | :white_check_mark: |
| `main` branch      | :white_check_mark: (best effort) |
| Older than latest  | :x:                |

If you are running an older build, please update before reporting — the issue
may already be fixed.

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues,
discussions, or pull requests.** Public disclosure before a fix is available
puts all users at risk.

Instead, use one of the following private channels:

1. **GitHub Private Vulnerability Reporting (preferred):**
   open the **Security** tab of this repository and click
   **"Report a vulnerability"**. This keeps the report confidential and gives
   you a private fork to develop a proof of concept.
2. **Email:** [shapak.apps@gmail.com](mailto:shapak.apps@gmail.com) with the
   subject line `[SECURITY] Blue Player — <short summary>`.

### What to include in your report

* A clear description of the vulnerability and its potential impact
* The affected component, version tag, or commit SHA
* Step-by-step reproduction instructions, including proof-of-concept code,
  logs, or screenshots where possible
* The Android version(s) and device(s) where the issue was reproduced
* Whether exploitation requires physical access, user interaction, or network
  position

### What happens next

| Step                        | Target time            |
| --------------------------- | ---------------------- |
| Acknowledgement of receipt  | within 48 hours        |
| Initial assessment & severity | within 5 business days |
| Fix plan & coordinated disclosure date | agreed with you |
| Patch released & advisory published | as soon as ready |

We follow a **90-day coordinated disclosure** window. We will not publish
details of an unresolved vulnerability without your agreement, and we ask that
you do not disclose publicly until a fix is available.

### Recognition

Researchers who report valid, previously unknown vulnerabilities will be
credited in our release notes and in the **Security acknowledgments** section
below, unless they prefer to remain anonymous.

## Scope

### In scope

* Blue Player application code (Kotlin, Jetpack Compose UI)
* The native C++ audio engine and its JNI layer
* Media3 / ExoPlayer integration as used by the app (playback session,
  media notification, media browsing)
* MediaStore interactions: library scanning, track deletion, queue and
  playlist persistence
* Network clients used for cover-art lookup (iTunes Search, Deezer,
  MusicBrainz / Cover Art Archive)
* Local storage of user data (settings, favorites, playlists, bookmarks)

### Out of scope

* Vulnerabilities in the Android operating system or device firmware
* Upstream vulnerabilities in androidx.media3, ExoPlayer, Glide, Jetpack
  Compose, or other third-party libraries — please report those to the
  respective projects; we will update our dependencies promptly
* The third-party web services we query (itunes.apple.com, api.deezer.com,
  musicbrainz.org, coverartarchive.org)
* Attacks that require physical access to an unlocked device
* Social engineering attacks
* Findings from automated scanners without a working proof of concept
* Rate-limiting or denial-of-service concerns against third-party APIs
* Issues that only affect already-rooted or heavily modified devices

## Security Design Notes

Blue Player is designed with privacy and safety in mind:

* **No backend server.** All user data — library, playlists, favorites,
  settings, playback position — is stored locally on the device.
* **Minimal network usage.** The only network calls are HTTPS cover-art
  metadata lookups. No personal data, library contents, or listening history
  ever leave the device.
* **No analytics, no tracking, no advertising SDKs.**
* **Scoped Storage compliance.** File access goes through MediaStore; track
  deletion uses the system consent dialogs required on Android 11+.
* **Hardened release builds.** Release artifacts are minified and
  resource-shrunk with R8/ProGuard and distributed only as signed builds via
  GitHub Releases.

## Safe Harbor

We consider security research conducted in good faith on Blue Player to be
authorized. If you follow this policy while researching, we will not pursue
legal action against you, and we will work with you to understand and resolve
any issue you find. If a third party initiates legal action against you for
research performed in accordance with this policy, we will make it known that
your actions were authorized.

## Acknowledgments

We thank the following researchers for responsibly disclosing security issues:

| Researcher | Issue | Disclosed |
| ---------- | ----- | --------- |
| *(none yet — be the first!)* | | |

## Changes to This Policy

This policy may be updated from time to time. Material changes will be
announced in release notes. The current version always lives at the root of
this repository.

## Contact

* **Security reports:** [shapak.apps@gmail.com](mailto:shapak.apps@gmail.com)
* **General questions:** open a regular GitHub issue
* **Organization:** [Shapak-Apps](https://github.com/Shapak-Apps)