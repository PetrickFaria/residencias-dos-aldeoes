"""Start a real graphical Minecraft client and require its captured inspection scenes."""
from pathlib import Path
import subprocess
import shutil
import sys
import struct

if not shutil.which('xvfb-run'):
    sys.exit('Xvfb is unavailable.')
source=Path('run-gametest/world')
if not source.exists():
    sys.exit('GameTest world unavailable for graphical inspection.')
shutil.copytree(source,Path('run/saves/visual-check'),dirs_exist_ok=True)
log=Path('client-console.log')
with log.open('w') as out:
    result=subprocess.run(['xvfb-run','-a','-s','-screen 0 1920x1080x24','bash','gradlew','--no-daemon','-Pvisual_check=true','runClient'],stdout=out,stderr=subprocess.STDOUT)
text=log.read_text(errors='replace')
print('\n'.join(text.splitlines()[-35:]))
for name in ['visual-chests.png','visual-doors.png','visual-anchor-slots.png']:
    path=Path('build/screenshots')/name
    if not path.exists():sys.exit('Minecraft did not capture '+name)
    data=path.read_bytes()
    if data[:8]!=bytes.fromhex('89504e470d0a1a0a') or struct.unpack('>II',data[16:24])!=(1600,900):
        sys.exit('Invalid Minecraft screenshot: '+name)
if result.returncode and 'Gradle build daemon disappeared unexpectedly' not in text:
    sys.exit(result.returncode)
print('Real Minecraft client captured all three scenes; image inspection is still required.')

# Reopen the same saved world in a fresh Minecraft JVM, without copying/resetting it.
restore_log=Path('client-restore-console.log')
with restore_log.open('w') as out:
    restored=subprocess.run(['xvfb-run','-a','-s','-screen 0 1920x1080x24','bash','gradlew','--no-daemon','-Pvisual_check=true','-Pvisual_restore=true','runClient'],stdout=out,stderr=subprocess.STDOUT,timeout=180)
restored_text=restore_log.read_text(errors='replace')
print('\n'.join(restored_text.splitlines()[-25:]))
if 'TEN_SLOTS_WORLD_RESTART_PASSED' not in restored_text:
    sys.exit('Ten stored output stacks did not survive a complete Minecraft process restart.')
if restored.returncode and 'Gradle build daemon disappeared unexpectedly' not in restored_text:
    sys.exit(restored.returncode)
p=Path('build/screenshots/visual-anchor-restored.png')
if not p.exists():sys.exit('Restored anchor screenshot missing.')
print('Fresh Minecraft process verified all ten stored stacks after reopening the saved world.')
