#!/usr/bin/env python3
"""Diff Relics API surface between versions against what an addon actually references.

Usage: apidiff.py <javap> <relics.jar> <addon.jar>
Builds: class -> {super, ifaces, members:(name,desc)} from javap -p -s,
and addon Methodref/Fieldref set from javap -c -p output.
Inheritance-aware existence check.
"""
import re, subprocess, sys, zipfile, collections

JAVAP, RELICS, ADDON = sys.argv[1], sys.argv[2], sys.argv[3]
BATCH = 60

def classes_in(jar):
    with zipfile.ZipFile(jar) as z:
        return [n[:-6].replace("/", ".") for n in z.namelist() if n.endswith(".class")]

def javap(args, names):
    out = []
    for i in range(0, len(names), BATCH):
        chunk = names[i:i + BATCH]
        p = subprocess.run([JAVAP] + args + chunk, capture_output=True, text=True, errors="replace")
        out.append(p.stdout)
    return "\n".join(out)

DECL = re.compile(r'^(?:public |final |abstract |sealed |non-sealed )*(?:class|interface|record) ([\w.$]+)(?: extends ([\w.$]+))?(?: implements ([\w.,\s$]+?))?(?: <.*>)? \{$')
MEM = re.compile(r'^  (?:\S+ )*(\S+)\(([^)]*)\);\s*$')
FIELD = re.compile(r'^  (?:\S+ )*(\S+);\s*$')

def parse_surface(text):
    surface = {}
    cur = None
    pending_name = None
    for line in text.splitlines():
        line = line.rstrip()
        if line.startswith("Compiled from"):
            cur = None; pending_name = None; continue
        m = DECL.match(line.strip())
        if m:
            cur = m.group(1)
            sup = m.group(2)
            ifs = [x.strip() for x in (m.group(3) or "").split(",") if x.strip()]
            surface.setdefault(cur, {"super": sup, "if": ifs, "m": set(), "f": set()})
            continue
        if cur is None:
            continue
        s = line.strip()
        if s.startswith("descriptor:"):
            desc = s.split(":", 1)[1].strip()
            if pending_name:
                kind, name = pending_name
                (surface[cur]["m"] if kind == "m" else surface[cur]["f"]).add((name, desc))
            pending_name = None
            continue
        mm = MEM.match(line)
        if mm:
            pending_name = ("m", mm.group(1)); continue
        ff = FIELD.match(line)
        if ff:
            pending_name = ("f", ff.group(1)); continue
    return surface

def parse_refs(text):
    """owner -> set of (kind, name, desc) referenced"""
    refs = collections.defaultdict(set)
    pat = re.compile(r'// (?:Method|InterfaceMethod|Field) ([\w.$/]+)\.([\w$<>]+):([^ \n]+)')
    for line in text.splitlines():
        for m in pat.finditer(line):
            owner = m.group(1).replace("/", ".")
            refs[owner].add((m.group(2), m.group(3)))
    return refs

print("indexing %s ..." % RELICS, file=sys.stderr)
rel_names = classes_in(RELICS)
surface = parse_surface(javap(["-p", "-s", "-classpath", RELICS], rel_names))
print("  classes indexed:", len(surface), file=sys.stderr)

def exists(cls, name, desc, depth=0, seen=None):
    seen = seen or set()
    if cls in seen or depth > 8:
        return False
    seen.add(cls)
    e = surface.get(cls)
    if not e:
        return None  # unknown class (not in relics jar)
    if (name, desc) in e["m"]:
        return True
    if e["super"] and exists(e["super"], name, desc, depth + 1, seen):
        return True
    return any(exists(i, name, desc, depth + 1, seen) for i in e["if"])

print("indexing %s ..." % ADDON, file=sys.stderr)
addon_names = classes_in(ADDON)
reftext = javap(["-p", "-c", "-classpath", ADDON], addon_names)
refs = parse_refs(reftext)
print("  owners referenced:", len(refs), file=sys.stderr)

missing = []
for owner, members in refs.items():
    if not owner.startswith("it.hurts.sskirillss.relics"):
        continue
    for name, desc in members:
        r = exists(owner, name, desc)
        if r is False:
            missing.append((owner, name, desc))

print("MISSING_SYMBOLS=%d" % len(missing))
for owner, name, desc in sorted(set(missing)):
    print("%s.%s%s" % (owner, name, desc))
