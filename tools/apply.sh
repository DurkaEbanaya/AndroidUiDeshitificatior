#!/bin/sh
set -eu
SERIAL=${SERIAL:-$(adb get-serialno)}
export SERIAL
adb -s "$SERIAL" push build/dex/classes.dex /data/local/tmp/classic-overlay.dex
adb -s "$SERIAL" shell 'su -c "CLASSPATH=/data/local/tmp/classic-overlay.dex app_process /system/bin OverlayProbe classic_settings com.android.settings homepage_preference_min_height 56 homepage_preference_corner_radius 0 nt_common_card_panel_radius 0 nt_common_preference_corner_radius 0 nt_common_preference_small_corner_radius 0 nt_preference_horizontal_margin 0 nt_common_round_preference_gap 0 search_bar_corner_radius 4 button_corner_radius 4 settingslib_preference_corner_radius 0"'
adb -s "$SERIAL" shell 'su -c "CLASSPATH=/data/local/tmp/classic-overlay.dex app_process /system/bin OverlayProbe classic_shade com.android.systemui qs_corner_radius 0 qs_large_corner_radius 0 page_tile_layout_corner_radius 0 qs_bt_page_layout_corner_radius 0 bluetooth_tile_view_large_corner_radius 0 notification_corner_radius 0 cell_padding 4"'
python3 tools/grid.py
