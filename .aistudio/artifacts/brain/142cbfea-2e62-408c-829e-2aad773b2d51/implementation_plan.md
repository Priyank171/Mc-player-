# MC Player - Advanced Android Video Player

An all-in-one, high-performance offline video player inspired by MX Player / MC Player, featuring a comprehensive gesture control suite (volume, brightness, seek scrub, pinch-to-zoom, 2x long-press speed boost), customizable SRT/VTT subtitle rendering with sync offsets, and a local media library manager with offline playback and sample videos.

---

## 2X Speed Boost Design Refinement

- **Screen Position:** Positioned at the **top center** of the screen (`Alignment.TopCenter`), with status-bar safe insets (`statusBarsPadding().padding(top = 10.dp)`), ensuring zero obstruction of the video action.
- **Translucent / Transparent Finish:** Semi-transparent dark pill background (`Color(0x66000000)`) with a subtle `0.75dp` soft border (`Color(0x40FFFFFF)`).
- **Compact & Minimal Size:** Sleek pill size with a tiny `12dp` fast-forward icon and `11sp` monospace `"2X"` text badge.
- **Engagement & Release:** Engaging via long-press immediately shifts ExoPlayer to `2.0x` playback speed and displays the 2X indicator; releasing your finger instantly restores normal speed and dismisses the indicator.
