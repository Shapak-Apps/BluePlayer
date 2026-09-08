# Contributing to BluePlayer

Thanks for your interest in making BluePlayer better! This document explains how to report issues, propose features and submit code.

## Code of Conduct

By participating in this project you agree to follow the [Contributor Covenant Code of Conduct, version 2.1](https://www.contributor-covenant.org/version/2/1/code_of_conduct/).

## Reporting bugs

Open an issue and include:
- BluePlayer version and Android version / device model
- Steps to reproduce
- Expected vs actual behavior
- Logcat output if the app crashes (`adb logcat` filtered by the app process)

## Suggesting features

Open an issue with the `enhancement` label and describe:
- The problem you want to solve
- How the feature should behave in the UI
- Whether it affects playback, library, audio engine or UI

## Pull request flow

1. Fork the repository and create a branch from `main`:
    - `feature/short-description`
    - `fix/short-description`
    - `docs/short-description`
2. Set up the project (see README → Getting started).
3. Make your changes in small, focused commits.
4. Push your branch and open a pull request against `main`.
5. Describe what changed and why; attach screenshots for UI changes.

## Project rules

- **Multi-module discipline:** feature modules must not depend on other feature modules. Shared code belongs in `core:*` or `ui:*`.
- **UI:** Jetpack Compose + Material 3 only. Never hardcode colors — use `MaterialTheme.colorScheme` so light and dark themes stay in parity.
- **Strings:** every user-visible string goes through `Strings` in `core:domain` and must be provided in all three languages (EN, RU, TK).
- **Native code (C++):** keep the audio processing path allocation-free and lock-free; all DSP state changes go through atomics.
- **Secrets:** never commit `keystore.properties`, `*.jks` or passwords. They are git-ignored.

## Code style

- Follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Follow Compose guidelines: hoist state, keep composables side-effect free, use `remember` correctly.
- Match the formatting of the surrounding code; keep functions small and named.

## Commit messages

Use [Conventional Commits](https://www.conventionalcommits.org/):
feat(player): add cascade mode to bass DSP
fix(search): keep keyboard state after query clear
docs(readme): update screenshots

Common scopes: `player`, `audio`, `library`, `search`, `playlists`, `settings`, `ui`, `docs`, `build`.

## Before opening a PR — test checklist

- [ ] `./gradlew assembleDebug` and `./gradlew assembleRelease` succeed
- [ ] Tested on API 24 and API 34
- [ ] Light and dark themes checked on changed screens
- [ ] All three languages checked on changed screens
- [ ] Playback verified: background playback, notification controls, headset unplug pauses audio
- [ ] No new warnings in Logcat from the app process

## License

BluePlayer is licensed under the Apache License 2.0. By contributing you agree that your contributions are licensed under the same license.