"""
Adds the user's requested exercise list to the library.

Usage:  python -I tools/add_requested.py
Writes: app/src/main/assets/exercises_requested.json  (new exercises, ids "r_*")
        app/src/main/assets/exercise_renames.json     (id -> requested display name; old name kept as alias)
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_variants import SETUP, APP_EQUIPMENT  # noqa: E402
from requested_map import NEW, RENAME  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, 'app', 'src', 'main', 'assets')


def requested():
    names = []
    for line in open(os.path.join(ROOT, 'tools', 'requested_exercises.txt'), encoding='utf-8'):
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        if '|' in line:
            base, eqs = [x.strip() for x in line.split('|')]
            names += [f"{base} - {e.strip()}" for e in eqs.split(',')]
        else:
            names.append(line)
    return names


def norm(s):
    return re.sub(r'[^a-z0-9]', '', s.lower().replace('biceps', 'bicep'))


def slug(s):
    return re.sub(r'[^A-Za-z0-9]+', '_', s).strip('_')


def main():
    lib = json.load(open(os.path.join(ASSETS, 'exercises.json'), encoding='utf-8'))
    lib += json.load(open(os.path.join(ASSETS, 'exercises_variants.json'), encoding='utf-8'))
    by_id = {x['id']: x for x in lib}
    extras = re.findall(r'x\("(x_[^"]+)",\s*"([^"]+)"',
                        open(os.path.join(ROOT, 'app/src/main/java/com/gymtrainer/app/data/SeedData.kt'), encoding='utf-8').read())
    by_norm = {norm(x['name']): x['id'] for x in lib}
    by_norm.update({norm(n): i for i, n in extras})

    renames, new, errors = {}, [], []
    taken = {}  # existing id -> requested name using it

    def claim(rid, name):
        if rid in taken and taken[rid] != name:
            errors.append(f"{name}: id {rid} already used by {taken[rid]}")
            return False
        taken[rid] = name
        return True

    def image_of(src):
        if not src:
            return '', 0
        if src not in by_id:
            errors.append(f"unknown src id {src}")
            return '', 0
        x = by_id[src]
        base = x.get('imageOf') or x['id']
        return base, by_id[base]['images'] if base in by_id else 0

    for name in requested():
        if name in RENAME:
            rid = RENAME[name]
            if rid not in by_id and rid not in dict(extras):
                errors.append(f"RENAME {name}: unknown id {rid}")
            elif claim(rid, name) and (by_id[rid]['name'] if rid in by_id else dict(extras)[rid]) != name:
                renames[rid] = name
            continue
        hit = by_norm.get(norm(name))
        if hit:
            if claim(hit, name) and (by_id[hit]['name'] if hit in by_id else dict(extras)[hit]) != name:
                renames[hit] = name
            continue
        base, eq = (name.rsplit(' - ', 1) + ['Bodyweight'])[:2] if ' - ' in name else (name, 'Bodyweight')
        spec = NEW.get(base)
        if spec is None:
            errors.append(f"no NEW spec for base '{base}' ({name})")
            continue
        img, count = image_of(spec['src'])
        src = by_id.get(spec['src']) if spec['src'] else None
        steps = spec['steps'] or (src['steps'] if src else None)
        if not steps:
            errors.append(f"{name}: no steps")
            continue
        new.append({
            'id': 'r_' + slug(name), 'name': name, 'group': spec['group'], 'primary': spec['primary'],
            'secondary': spec['secondary'], 'equipment': APP_EQUIPMENT.get(eq, eq), 'level': spec['level'],
            'pattern': spec['pattern'], 'compound': spec['compound'], 'category': 'strength',
            'setup': SETUP[eq], 'steps': steps,
            'mistakes': spec['mistakes'] or (src['mistakes'] if src else 'Rushing | Poor posture | Holding your breath'),
            'repMin': spec['reps'][0], 'repMax': spec['reps'][1], 'rest': spec['reps'][2],
            'timed': spec['secs'] > 0, 'seconds': spec['secs'], 'images': count, 'imageOf': img, 'featured': False,
        })

    # Final library must have unique names.
    final = {x['id']: renames.get(x['id'], x['name']) for x in lib}
    final.update({i: renames.get(i, n) for i, n in extras})
    final.update({x['id']: x['name'] for x in new})
    seen = {}
    for i, n in final.items():
        k = n.lower()
        if k in seen:
            errors.append(f"duplicate name '{n}': {seen[k]} and {i}")
        seen[k] = i

    if errors:
        print("ERRORS:")
        for e in errors:
            print("  ", e)
        sys.exit(1)
    json.dump(new, open(os.path.join(ASSETS, 'exercises_requested.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=0)
    json.dump(renames, open(os.path.join(ASSETS, 'exercise_renames.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=0)
    names = requested()
    print(f"requested {len(names)}: new {len(new)}, renamed {len(renames)}, "
          f"without photo {sum(1 for x in new if not x['images'])}, library total {len(final)}")
    missing = [n for n in names if n.lower() not in seen]
    print("requested names not in final library:", missing)


if __name__ == '__main__':
    main()
