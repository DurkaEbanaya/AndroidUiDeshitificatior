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
- Fabricated resource overlays remove card/tile rounding and selected margins.
- Host tools and extracted firmware are excluded from git.

Installed prerequisites: NeoZygisk 2.4 (289), Vector 2.2 (3080).
No system APK modifications. Source APKs and captured UI evidence are in `research/`.

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
