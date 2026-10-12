# Architecture

Better Custom Audio Triggers keeps the game integration, rule evaluation, audio storage and UI in separate layers.

- `battleaudio.model` defines the JSON configuration, rule candidates, event context and stable identifiers.
- `battleaudio.core` evaluates triggers, thresholds, prerequisites, pauses and stop conditions.
- `battleaudio.audio` owns OpenAL playback, background music ducking and active playback lifecycles.
- `battleaudio.storage` imports user audio into a managed per-user library, creates local decoder caches and persists configuration. The public package contains no embedded preset resources, so a fresh library starts empty.
- `battleaudio.patches` connects game callbacks to the event model through ModTheSpire patches.
- `battleaudio.ui` renders the in-game settings overlay, selectors, filters and localized text.
- `battleaudio.localization` loads Better Custom Audio Triggers's own interface strings using the game's selected language.

The mod reads game and Workshop APIs at build time and runtime. It does not copy those APIs, game textures, game audio, screenshots or user files into the repository.
