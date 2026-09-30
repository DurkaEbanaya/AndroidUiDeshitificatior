"""Back up and compact the native Nothing QS layout; run with --restore to undo."""
import json, pathlib, subprocess, sys, shlex, os

serial = os.environ.get("SERIAL")
backup = pathlib.Path("research/qs-layout-backup.json")
key = "nt_sysui_qs_tiles"
def shell(command):
    adb = ["adb", "-s", serial] if serial else ["adb"]
    return subprocess.check_output(adb + ["shell", command], text=True).strip()
if "--restore" in sys.argv:
    original = backup.read_text().strip()
    shell("settings put secure " + key + " " + shlex.quote(original))
else:
    original = shell("settings get secure " + key)
    if not backup.exists():
        backup.parent.mkdir(parents=True, exist_ok=True)
        backup.write_text(original)
    data = json.loads(original)
    for i, item in enumerate(data.values()):
        item.update(cellX=i % 4, cellY=(i // 4) % 4, spanX=1, spanY=1, screenId=1 + i // 16)
    shell("settings put secure " + key + " " + shlex.quote(json.dumps(data, separators=(",", ":"))))
print(shell("settings get secure " + key))
