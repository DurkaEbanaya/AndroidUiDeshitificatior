# AndroidUiDeshitificatior

Native Android UI simplification inspired by Android 11 / crDroid 7.
Current module: **Nothing Classic UI — experimental prototype**.

Target: Nothing Phone (3a) Pro, Nothing OS B4.1-260810-1153, Android 16.
Visual reference: crDroid 7 / Android 11. Work in progress, not a finished port.

## Implementation

- Legacy Xposed module (`dev.lain.classicui`) scoped to SystemUI and Settings.
- Native Nothing QS tiles use equal 1×1 cells, four columns; original positions
  are saved in `research/qs-layout-backup.json` before modification.
- Wi-Fi, mobile data, Bluetooth and ringer use the base native tile view.
- Tile labels are placed below icons. Secondary labels are hidden.
- Version 0.2: one dark QS surface, transparent permanent tile backgrounds;
  active icons blue, inactive icons light grey, unavailable icons muted.
  Native touch/ripple handling is retained.
- Settings preference binding reduces row padding and minimum height.
- Version 0.3: thin brightness track with a round thumb, transparent drag mirror,
  first-swipe brightness control, unfilled editor icon frames and power menu.
  First-swipe positioning is currently tuned for this phone's portrait layout.
- Version 0.4: edge-to-edge notification rows, no notification-stack scrim
  panel, and unfilled notification footer buttons.
- Version 0.4.2: flatten the actual notification drawable as well as its
  clipping outline. Open **Nothing Classic UI** from the launcher to adjust
  shade blur from 0–400 px (default 160); close and reopen the shade to apply.
- Version 0.4.3: flatten Nothing's separate notification section background.
  When native window blur is disabled by firmware, render a blurred copy of
  the static wallpaper behind the shade. This fallback blurs wallpaper,
  rather than the live contents of the underlying application.
- Version 0.4.7: blur strength is displayed as 0–100%, mapped quadratically
  to 0–80 px for finer low-strength control. The wallpaper ID is checked
  on shade opening; a changed wallpaper refreshes the copied backdrop.
- Version 0.5: separate notification-background transparency and local wallpaper
  blur sliders on the same settings screen. Text and icons are not faded or
  blurred. Local blur uses the static wallpaper fallback, not live app content.
- Fabricated resource overlays remove card/tile rounding and selected margins.
- Host tools and extracted firmware are excluded from git.

### 0.6.14 media surfaces and spacing

- Flatten the lockscreen `MediaContainerView` clipping path too, including
  after configuration changes. Reattach player styling when its host moves.
- First-swipe QS backing ends at the brightness control (313 dp); it expands
  with full QS rather than filling the area behind quick media.
- Quick media removes unused footer padding and the duplicate brightness
  height reserve. The gap before the first notification uses the native
  `notification_divider_height` (2 dp on this firmware), not zero spacing.
- Measured current session: brightness bottom 841 px, media 860–1343 px,
  notification stack begins at 1348 px (5 px gap, previously 1556 px).
- Checked lock/unlock, shade transitions and configuration changes; final
  Vector log contained no hook exceptions.

### 0.6.13 media layout

- Portrait quick-media host reserves measured space below the added brightness
  slider. Its height remains part of the native header measurement, placing
  the first notification after the player instead of overlapping QS controls.
- Player, artwork/effect outlines, button backgrounds and carousel clipping
  use square corners; playback actions and artwork colours remain native.
- Verified with the existing YouTube media session: quick brightness ends at
  841 px, media occupies 860–1343 px, first notification starts at 1556 px.
  Expanded QS transitions and light/dark switching produced no hook errors.
  Placement remains tuned for this phone's portrait layout.

### 0.6.12 heads-up fixes

- QS background and quick brightness require a nonzero shade expansion, not
  just `mQsVisible` (Nothing also sets that flag during a pinned heads-up).
- The extra brightness height is reserved only with an unlocked, opening shade.
- Wallpaper blur requires `shadeOrQsExpanded`; a notification-only window does
  not gain a wallpaper backdrop or local wallpaper blur.
- Preserve `RoundableState.maxRadius` for vendor normalisation; flatten the
  final corner radii instead. Zeroing the denominator produced infinite
  roundness requests and invalid clipping calculations.
- Tested with a temporary high-importance notification with a reply action:
  pinned card top 147 px (native status-bar inset), content/background height
  314 px, finite roundness, light/dark themes and shade opening. The test app
  was removed afterward; final Vector checks contained no hook exceptions.

### 0.6.11 regression fixes

- Settings surfaces are adjusted in the Activity's pre-draw phase, not inside
  `View.draw`. Dialog decors (including wireless-debug pairing codes) retain
  their native opaque backgrounds and redraw behaviour.
- Notification transparency uses a canvas layer around the background pass
  only, without changing callback-bearing drawable alpha on every frame.
  Section labels and notification contents retain their original opacity.
- Local notification blur follows actual card width, height and animation
  clipping, rather than painting across the entire measured background view.
- On the development phone an unrelated `com.nt.diagswitch` window from user
  10 intercepted Chrome toolbar taps at `[696,126][916,346]`. It was stopped
  and disabled **only in that work profile**. This is a device repair, not an
  APK hook or automatic package-disabling feature. Restore with
  `adb shell pm enable --user 10 com.nt.diagswitch` if needed.
- Device checks: Chrome tab switcher and overflow menu, repeated pairing-dialog
  opening, shade expansion/collapse, and light → dark theme round-trip. No
  new Vector hook exceptions were reported during the final checks.

Installed prerequisites: NeoZygisk 2.4 (289), Vector 2.2 (3080).
No system APK modifications. Source APKs and captured UI evidence are in `research/`.

### 0.7 native Windows Phone-style lockscreen

- Styles the built-in Nothing **General** clock face (`GENERAL`). Select that
  face in the native lockscreen editor; other clock styles retain their own
  rendering. The development phone already had General selected.
- Reuses the face's existing TextViews for thin, left-aligned time, weekday,
  and full month/day on separate lines. No new window, background service,
  independent timer, wallpaper replacement, or replacement unlock screen.
- Native clock events continue to supply minute updates, locale, timezone,
  12/24-hour mode, wallpaper contrast, wakefulness and doze colours.
- Keyguard's ConstraintSet positions the clock block in the lower part of the
  portrait screen and reserves a separate notification area above it. The
  native fingerprint entry, PIN screen and AOD burn-in controller remain in
  charge. The duplicate top date slice is hidden for this clock face.
- General's weather/calendar fields are used as the date block for this style.
  Other native clock faces can still be selected for their original content.
- Confirmed on the development phone: native clock bounds `[0,1435][1080,1934]`,
  separate localized weekday/date text, notification above the clock, dozing
  state, wake-up, theme round-trip and swipe dismissal back to the foreground
  app. A PIN challenge was not exercised; AOD entered dozing but its captured
  image was black, so visible always-on rendering still needs device-side
  confirmation. Full shade styling/layout code was not changed for this release.
- Typeface uses Android's built-in `sans-serif-light`, rather than bundling
  Microsoft's Segoe UI. This is a native adaptation, not a pixel-identical port.

## Build / apply

`sh tools/build.sh` uses Android build-tools 35, platform 36, JDK 17 and
Xposed API 82 from `/tmp/opencode/classic-tools`.
Override `TOOLS` and `JDK` to use another tool directory. This is the initial
development build script; SDK downloading is not automated yet.
The debug signing key is generated locally and is not included in the repository.

Overlay helper:

```sh
/tmp/opencode/classic-tools/jdk-17.0.20.1+1/bin/javac --release 8 -d build/classes tools/OverlayProbe.java
/tmp/opencode/classic-tools/android-15/d8 --output build/dex build/classes/OverlayProbe.class
sh tools/apply.sh
```

Install `build/nothing-classic-ui.apk`, enable it in Vector, and select only
`com.android.systemui` and `com.android.settings` as its scope.
ADB tools use the connected device; set `SERIAL` when multiple devices are attached.

The fabricated overlays disappeared during reboot testing on this build;
reapply them with `tools/apply.sh`. The APK and QS positions survive reboot.

## Restore

From this directory: `sh tools/restore.sh`. This disables the UI module,
restores backed-up QS positions, disables resource overlays and restarts UI.
The two framework modules can separately be disabled in KernelSU.

## Verification and limitations

- APK compiles, signature verifies, installation succeeds.
- Vector reports module loaded in Settings and SystemUI.
- Phone boots successfully with framework modules enabled; root remains available.
- Settings UI dump confirms reduced horizontal and vertical row spacing.
- Native QS layout setting contains non-overlapping 1×1 positions.
- Visual/function verification of expanded QS remains incomplete: `expand-settings`
  sometimes marks QS expanded while NotificationShade window stays invisible,
  including with the UI module disabled. `expand-notifications` can show the window.
- Large Settings heading and some hardcoded category gaps still need adaptation.
- Special tile secondary actions and custom tile behaviour need hands-on testing.
- No promise of compatibility with another firmware or OTA.
