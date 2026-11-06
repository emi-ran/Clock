"""Lightweight guard for legacy migrations; does not run Android code."""
from pathlib import Path

source = (Path(__file__).resolve().parents[1] / 'app/src/main/java/com/best/deskclock/provider/ClockDatabaseHelper.java').read_text()
block = source.split('if (oldVersion < 21) {', 1)[1].split('if (oldVersion < 22) {', 1)[0]
assert 'new Alarm(cursor)' not in block, 'legacy projection cannot be read by the modern Alarm cursor constructor'
assert 'createAlarmsTable(db, TEMP_ALARMS_TABLE_NAME)' not in block, 'current schema causes duplicate later ALTER TABLE migrations'
print('legacy migration source contract passed')

# Exercise the actual static CREATE/ALTER/UPDATE SQL against SQLite. The Java
# control flow and Android framework are not executed by this harness.
import re
import sqlite3
import json

contract = (Path(__file__).resolve().parents[1] / 'app/src/main/java/com/best/deskclock/provider/ClockContract.java').read_text()
constants = dict(re.findall(r'String\s+(\w+)\s*=\s*"([^"]*)"\s*;', contract))
constants['_ID'] = '_id'
tables = {'ALARMS_TABLE_NAME': 'alarm_templates', 'INSTANCES_TABLE_NAME': 'alarm_instances', 'CATEGORIES_TABLE_NAME': 'alarm_categories'}
def sql(expr, **local):
    parts = re.findall(r'"(?:[^"\\]|\\.)*"|ClockContract\.\w+\.\w+|\b(?:alarmsTableName|instanceTableName|ALARMS_TABLE_NAME|INSTANCES_TABLE_NAME|CATEGORIES_TABLE_NAME)\b', expr)
    return ''.join(json.loads(p) if p.startswith('"') else constants[p.rsplit('.', 1)[-1]] if p.startswith('ClockContract.') else (tables | local)[p] for p in parts)
def method(name):
    return source.split('void ' + name + '(', 1)[1].split('\n    }', 1)[0]
def create(db, name, **local):
    expr = re.search(r'db\.execSQL\((.*?)\);(?=\s*(?:\n|$))', method(name), re.S).group(1)
    db.execute(sql(expr, **local))
def upgrade_sql(db, version):
    for n in range(22, 33):
        m = re.search(r'if \(oldVersion < ' + str(n) + r'\) \{(.*?)\n        \}', source, re.S)
        if version < n and m:
            for expr in re.findall(r'db\.execSQL\((.*?)\);(?=\s*(?:\n|$))', m.group(1), re.S):
                db.execute(sql(expr))
            if 'createCategoriesTable(db)' in m.group(1): create(db, 'createCategoriesTable')
def columns(db, table):
    return [r[1] for r in db.execute('PRAGMA table_info(' + table + ')')]
for version in range(15, 21):
    for populated in (False, True):
        db = sqlite3.connect(':memory:')
        create(db, 'createVersion21AlarmsTable', alarmsTableName='alarm_templates')
        create(db, 'createVersion21InstanceTable', instanceTableName='alarm_instances')
        # Derive pre-v21 schemas by removing the columns introduced after
        # each version, then apply the production ALTER statements.
        additions = [(16, ['year', 'month', 'day'], []),
                     (17, ['crescendoDuration'], ['crescendoDuration']),
                     (18, ['snoozeDuration'], ['snoozeDuration']),
                     (19, ['autoSilenceDuration'], ['autoSilenceDuration']),
                     (20, ['alarmVolume'], ['alarmVolume'])]
        for threshold, alarms, instances in additions:
            if version < threshold:
                for table, names in [('alarm_templates', alarms), ('alarm_instances', instances)]:
                    for name in names:
                        db.execute('ALTER TABLE ' + table + ' DROP COLUMN ' + name)
        if populated:
            for table in ('alarm_templates', 'alarm_instances'):
                info = list(db.execute('PRAGMA table_info(' + table + ')'))
                vals = [7 if r[1] == '_id' else 7 if r[1] == 'alarm_id' else 'sentinel' if r[2] == 'TEXT' else 1 for r in info]
                db.execute('INSERT INTO ' + table + ' VALUES (' + ','.join('?' for _ in vals) + ')', vals)
        for threshold, _, _ in additions:
            if version < threshold:
                m = re.search(r'if \(oldVersion < ' + str(threshold) + r'\) \{(.*?)\n        \}', source, re.S)
                for expr in re.findall(r'db\.execSQL\((.*?)\);(?=\s*(?:\n|$))', m.group(1), re.S):
                    if 'UPDATE ' not in expr:
                        db.execute(sql(expr))
                if threshold == 16:
                    db.execute('UPDATE alarm_templates SET year=2026, month=10, day=6')
        original_columns = {t: columns(db, t) for t in ('alarm_templates','alarm_instances')}
        before = {t: db.execute('SELECT * FROM ' + t).fetchall() for t in ('alarm_templates','alarm_instances')}
        for table, meth, arg in [('alarm_templates','createVersion21AlarmsTable','alarmsTableName'), ('alarm_instances','createVersion21InstanceTable','instanceTableName')]:
            create(db, meth, **{arg:table + '_temp'})
            common = [c for c in columns(db, table + '_temp') if c in columns(db, table)]
            names = ','.join('"' + c + '"' for c in common)
            db.execute('INSERT INTO ' + table + '_temp (' + names + ') SELECT ' + names + ' FROM ' + table)
        db.execute('DROP TABLE alarm_instances'); db.execute('DROP TABLE alarm_templates')
        db.execute('ALTER TABLE alarm_templates_temp RENAME TO alarm_templates')
        db.execute('ALTER TABLE alarm_instances_temp RENAME TO alarm_instances')
        upgrade_sql(db, 21)
        for table in before:
            oldcols = original_columns[table] if populated else []
            # schema order is unchanged for the v21 columns
            if populated:
                row = db.execute('SELECT ' + ','.join('"' + c + '"' for c in oldcols) + ' FROM ' + table).fetchone()
                expected = list(before[table][0]); expected[oldcols.index(constants['AUTO_SILENCE_DURATION'])] *= 60
                assert row == tuple(expected), (version, table, row, expected)
            else: assert db.execute('SELECT count(*) FROM ' + table).fetchone()[0] == 0
        fresh = sqlite3.connect(':memory:')
        create(fresh, 'createAlarmsTable', alarmsTableName='alarm_templates')
        create(fresh, 'createInstanceTable', instanceTableName='alarm_instances')
        assert set(columns(db, 'alarm_templates')) == set(columns(fresh, 'alarm_templates'))
        assert set(columns(db, 'alarm_instances')) == set(columns(fresh, 'alarm_instances'))
print('SQLite v21 rebuild and v22–32 SQL passed: 6 versions × empty/populated (derived historical fixtures)')
