"""SDK-free integration source contracts; does not replace Android tests."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
java = root / "app/src/main/java/com/best/deskclock"
service = (java / "alarms/AlarmService.java").read_text()
state = (java / "alarms/AlarmStateManager.java").read_text()
scanner = (java / "alarms/BarcodeMissionScanner.java").read_text()
activity = (java / "alarms/AlarmActivity.java").read_text()
assert "import com.best.deskclock.provider.Alarm;" in service
assert service.index("Ignoring external alarm action while a mission is configured") < service.index("switch (action)")
assert "parent.barcodeFormat" in service and "parent.barcodeValue" in service
assert state.count("alarm.barcodeFormat != null") == 2
assert "cameraError(Exception error) { fallback(); }" in scanner
assert "BarcodeMission.matches(format, value" in scanner
assert "mAlarm.mathQuestionCount, mMissionRandom" in activity
for locale in ("values", "values-tr"):
    resources = ET.parse(root / f"app/src/main/res/{locale}/strings.xml").getroot()
    names = {e.get("name") for e in resources}
    assert {"barcode_mission_instruction", "barcode_mission_fallback", "barcode_mission_mismatch", "barcode_mission_enroll_prompt"} <= names
print("barcode integration source contracts passed")
