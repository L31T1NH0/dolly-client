"""Reconstruct the supplied OptiFine M5 at build time, without running its installer."""
import hashlib
import io
from pathlib import Path, PurePosixPath
import re
import tempfile
import zipfile

OPTIFINE_SHA256 = "2739760a70fca6ae5c7dc817bcc6283f18f55e38fa23778da7f387a6b724b2c4"
VANILLA_SHA256 = "14f0d96d1a56fb4f5c3b2233d00699525893fe5ce3dcf181e7de59120595d298"
_COPY_WIDTHS = {249: (2, 1), 250: (2, 2), 251: (2, 4), 252: (4, 1),
                253: (4, 2), 254: (4, 4), 255: (8, 4)}


def apply_gdiff(base, delta):
    """Decode the GDIFF v4 commands used by this artifact, checking all bounds."""
    stream = io.BytesIO(delta)

    def read(size):
        data = stream.read(size)
        if len(data) != size:
            raise ValueError("Truncated OptiFine GDIFF patch")
        return data

    def number(size):
        return int.from_bytes(read(size), "big")

    if read(5) != b"\xd1\xff\xd1\xff\x04":
        raise ValueError("Unsupported OptiFine GDIFF header")
    output = bytearray()
    while stream.tell() < len(delta):
        command = number(1)
        if command == 0:
            continue
        if command <= 248:
            size = command if command <= 246 else number(2 if command == 247 else 4)
            output.extend(read(size))
        else:
            offset_width, size_width = _COPY_WIDTHS[command]
            offset, size = number(offset_width), number(size_width)
            if offset + size > len(base):
                raise ValueError("OptiFine GDIFF copy exceeds base resource")
            output.extend(base[offset:offset + size])
    return bytes(output)


def _members(archive):
    members = {}
    for info in archive.infolist():
        name = info.filename
        if info.is_dir():
            continue
        if name in members or name.startswith("/") or "\\" in name or ".." in PurePosixPath(name).parts:
            raise ValueError("Duplicate or unsafe JAR entry: " + name)
        members[name] = archive.read(info)
    return members


def integrate_optifine(vanilla, optifine, output):
    """Merge a pinned vanilla JAR and M5, returning a reproducible build report.

    The returned JAR has complete resources, so bna's classpath fallback supplies
    them without OptiFine's runtime transformer or resource-locator registration.
    """
    vanilla, optifine, output = map(Path, (vanilla, optifine, output))
    if output.resolve() in (vanilla.resolve(), optifine.resolve()):
        raise ValueError("OptiFine output must not replace an input JAR")
    for file, expected in ((vanilla, VANILLA_SHA256), (optifine, OPTIFINE_SHA256)):
        if hashlib.sha256(file.read_bytes()).hexdigest() != expected:
            raise ValueError("Unsupported input JAR: " + str(file))
    with zipfile.ZipFile(vanilla) as archive:
        base = _members(archive)
    with zipfile.ZipFile(optifine) as archive:
        supplied = _members(archive)
    rules = []
    for line in supplied["patch.cfg"].decode("ascii").splitlines():
        if line.strip() and not line.lstrip().startswith("#"):
            pattern, source = line.split("=", 1)
            rules.append((re.compile(pattern.strip()), source.strip()))
    merged = {name: data for name, data in base.items() if not name.startswith("META-INF/")}
    patched = []
    for name, delta in supplied.items():
        if not name.startswith("patch/") or not name.endswith(".xdelta"):
            continue
        target = name[len("patch/"):-len(".xdelta")]
        source = next((target if value == "*" else value for pattern, value in rules if pattern.fullmatch(target)), None)
        if source is None or source not in base:
            raise ValueError("Missing OptiFine patch base for " + target)
        decoded = apply_gdiff(base[source], delta)
        checksum = supplied.get("patch/" + target + ".md5", b"").decode("ascii").strip()
        if not re.fullmatch("[0-9a-f]{32}", checksum) or hashlib.md5(decoded).hexdigest() != checksum:
            raise ValueError("OptiFine patch checksum mismatch: " + target)
        merged[target] = decoded
        patched.append({"member": target, "base": source, "md5": checksum,
                        "sha256": hashlib.sha256(decoded).hexdigest()})
    # Keep OptiFine's Forge compatibility types; these are not a Forge loader.
    # optifine/ contains the installer/transformer tools, with no direct runtime
    # class references from Config, patched game classes, or net/optifine/.
    runtime = [name for name in supplied if name == "Config.class" or name.startswith(("net/", "javax/", "assets/", "doc/"))
               or name in ("buildof.txt", "changelog.txt", "optifine/xdelta/licence.txt")]
    patched_names = {record["member"] for record in patched}
    for name in runtime:
        if name in patched_names:
            raise ValueError("OptiFine contains both complete and delta resource: " + name)
        merged[name] = supplied[name]
    expected_deltas = {name[:-len(".md5")] for name in supplied if name.startswith("patch/") and name.endswith(".md5")}
    actual_deltas = {"patch/" + record["member"] for record in patched}
    if expected_deltas != actual_deltas or len(patched) != 1050:
        raise ValueError("Unexpected OptiFine patch inventory")
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(dir=output.parent, prefix="optifine-", suffix=".jar", delete=False) as file:
            temporary = Path(file.name)
        with zipfile.ZipFile(temporary, "w", compression=zipfile.ZIP_DEFLATED) as archive:
            for name, data in sorted(merged.items()):
                entry = zipfile.ZipInfo(name)
                entry.compress_type = zipfile.ZIP_DEFLATED
                archive.writestr(entry, data)
        temporary.replace(output)
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)
    return {"version": "1.8.9_HD_U_M5", "optifine_sha256": OPTIFINE_SHA256,
            "vanilla_sha256": VANILLA_SHA256, "jar_sha256": hashlib.sha256(output.read_bytes()).hexdigest(),
            "patched_classes": sum(record["member"].endswith(".class") for record in patched),
            "patched_resources": sum(not record["member"].endswith(".class") for record in patched),
            "compatibility_stubs": sorted(name for name in runtime if name.startswith("net/minecraftforge/")),
            "runtime_files": len(runtime), "patches": patched}
