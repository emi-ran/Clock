"""SDK-free contract for the loader's qualified projection map (not an Android runtime test)."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1] / "app/src/main/java/com/best/deskclock/provider"
alarm = (root / "Alarm.java").read_text()
provider = (root / "ClockProvider.java").read_text()
projection = re.search(r'QUERY_ALARMS_WITH_INSTANCES_COLUMNS\s*=\s*\{(.*?)\};', alarm, re.S).group(1)
requested = re.findall(r'ClockDatabaseHelper\.(ALARMS|INSTANCES)_TABLE_NAME\s*\+\s*"\."\s*\+\s*(?:ClockContract\.(?:Alarms|Instances)Columns\.)?(\w+)', projection)
mappings = re.findall(r'sAlarmsWithInstancesProjection\.put\((ALARMS|INSTANCES)_TABLE_NAME\s*\+\s*"\."\s*\+\s*(?:Alarms|Instances)Columns\.(\w+),\s*(ALARMS|INSTANCES)_TABLE_NAME\s*\+\s*"\."\s*\+\s*(?:Alarms|Instances)Columns\.(\w+)\)', provider)
assert len(requested) > 40 and len(mappings) > 40, "could not parse projections"
assert {('ALARMS', x) for x in ('MATH_QUESTION_COUNT', 'BARCODE_FORMAT', 'BARCODE_VALUE')} <= set(requested)
actual = {(a, b): (c, d) for a, b, c, d in mappings}
missing = [f'{table}.{column}' for table, column in requested if actual.get((table, column)) != (table, column)]
assert not missing, f'Invalid column(s) in loader projection: {missing}'
print(f'loader projection contract passed: {len(requested)} columns')
