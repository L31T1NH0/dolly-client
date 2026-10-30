#!/usr/bin/env python3
"""Build a local Dolly version from an unmodified Minecraft 1.8.9 installation."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import zipfile

from package_prism import package_prism
from optifine import integrate_optifine, OPTIFINE_SHA256

ROOT = Path(__file__).resolve().parents[1]
VANILLA_SHA1 = "3870888a6c3d349d3771a3e9d16c9bf5e076b908"
VERSION = "dolly-1.8.9"


def cache_roots():
    home = Path.home()
    return [home / ".local/share/PrismLauncher/libraries", home / ".minecraft/libraries",
            home / ".var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/libraries"]


def discover_jar():
    for cache in cache_roots():
        p = cache / "com/mojang/minecraft/1.8.9/minecraft-1.8.9-client.jar"
        if p.is_file():
            return p
    p = Path.home() / ".minecraft/versions/1.8.9/1.8.9.jar"
    return p if p.is_file() else None


def libraries(roots):
    resolved = []
    runtime = json.loads((ROOT / "tools/version-template.json").read_text())["libraries"]
    coordinates = [lib["name"] for lib in runtime if "natives" not in lib]
    coordinates += json.loads((ROOT / "tools/libraries.json").read_text())
    for coordinate in dict.fromkeys(coordinates):
        group, artifact, version = coordinate.split(":")
        relative = Path(group.replace(".", "/")) / artifact / version / (artifact + "-" + version + ".jar")
        found = next((root / relative for root in roots if (root / relative).is_file()), None)
        if found is None:
            raise FileNotFoundError("Missing cached dependency: " + coordinate + "; install Minecraft 1.8.9 or pass --libraries")
        resolved.append(found.resolve())
    return resolved


def run(command):
    subprocess.run([str(c) for c in command], cwd=ROOT, check=True)


def javac(java, paths, destination, sources):
    destination.mkdir(parents=True, exist_ok=True)
    java8_rt = Path("/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar")
    target = ["-source", "8", "-target", "8", "-bootclasspath", str(java8_rt)] if java8_rt.is_file() else ["--release", "8"]
    args = target + ["-proc:none", "-Xlint:-options", "-encoding", "UTF-8", "-cp", os.pathsep.join(map(str, paths)),
            "-d", str(destination)] + [str(p) for p in sources]
    argument_file = destination.parent / (destination.name + ".args")
    argument_file.write_text("\n".join('"' + a.replace("\\", "\\\\").replace('"', '\\"') + '"' for a in args) + "\n")
    run([java, "-m", "jdk.compiler/com.sun.tools.javac.Main", "@" + str(argument_file)])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--minecraft-jar", type=Path, default=discover_jar())
    parser.add_argument("--libraries", type=Path, action="append", help="Local Maven cache; repeatable")
    parser.add_argument("--optifine", type=Path, default=ROOT / "references/OptiFine_1.8.9_HD_U_M5.jar", help="Local OptiFine 1.8.9 HD U M5 installer JAR")
    parser.add_argument("--java", default=shutil.which("java") or "java", help="JDK 9+ for compilation; output targets Java 8")
    args = parser.parse_args()
    if args.minecraft_jar is None or not args.minecraft_jar.is_file():
        parser.error("Install vanilla 1.8.9 with your launcher first, or pass --minecraft-jar")
    vanilla = args.minecraft_jar.resolve()
    if hashlib.sha1(vanilla.read_bytes()).hexdigest() != VANILLA_SHA1:
        parser.error("Minecraft JAR is not the original 1.8.9 client; refusing to patch a different version")
    if not args.optifine.is_file() or hashlib.sha256(args.optifine.read_bytes()).hexdigest() != OPTIFINE_SHA256:
        parser.error("Expected the original OptiFine 1.8.9 HD U M5 JAR; refusing a different version")
    deps = libraries(args.libraries or cache_roots())
    required = ["org/objectweb/asm/ClassReader.class", "org/objectweb/asm/tree/ClassNode.class"]
    for member in required:
        found = False
        for jar in deps:
            try:
                with zipfile.ZipFile(jar) as archive:
                    if member in archive.namelist():
                        found = True
                        break
            except zipfile.BadZipFile:
                continue
        if not found:
            parser.error("Missing local ASM/ASM Tree build library: " + member + "; pass --libraries")
    build = ROOT / "build"
    if build.exists():
        shutil.rmtree(build)
    build.mkdir()
    classes, patcher, tests = build / "classes", build / "patcher", build / "tests"
    integrated = build / "minecraft-optifine.jar"
    optifine_report = integrate_optifine(vanilla, args.optifine, integrated)
    (build / "optifine.json").write_text(json.dumps(optifine_report, indent=2) + "\n")
    cp = [integrated] + deps
    verify_java = Path("/usr/lib/jvm/java-8-openjdk-amd64/bin/java")
    verifier = str(verify_java) if verify_java.is_file() else args.java
    javac(args.java, cp, classes, sorted((ROOT / "src/main/java").rglob("*.java")))
    shutil.copytree(ROOT / "src/main/resources", classes, dirs_exist_ok=True)
    javac(args.java, cp + [classes], patcher, sorted((ROOT / "tools/java").glob("*.java")))
    devices = build / "test-devices"
    javac(args.java, [], devices, sorted((ROOT / "src/test/stubs").rglob("*.java")))
    javac(args.java, [devices] + cp + [classes], tests, sorted((ROOT / "src/test/java").rglob("*.java")))
    run([verifier, "-ea", "-cp", os.pathsep.join(map(str, [tests, classes])), "DollyCoreTest"])
    output = ROOT / "dist" / VERSION
    output.mkdir(parents=True, exist_ok=True)
    staged = build / (VERSION + ".jar")
    run([args.java, "-cp", os.pathsep.join(map(str, [patcher, classes, integrated] + deps)),
         "PatchClient", integrated, staged, classes])
    own_classes = build / "own-classes.txt"
    own_classes.write_text("\n".join(str(p.relative_to(classes))[:-6].replace(os.sep, "/") for p in sorted(classes.rglob("*.class"))) + "\n")
    run([verifier, "-Xverify:all", "-cp", os.pathsep.join(map(str, [tests, staged] + deps)),
         "DollyLinkTest", str(build / "patches.txt"), str(own_classes)])
    run([verifier, "-ea", "-cp", os.pathsep.join(map(str, [tests, staged] + deps)), "DollySettingsTest"])
    test_cp = os.pathsep.join(map(str, [tests, staged] + deps))
    run([verifier, "-ea", "-Xverify:all", "-Xbootclasspath/a:" + str(devices), "-cp", test_cp, "DollyRuntimeTest"])
    run([verifier, "-ea", "-cp", test_cp, "DollyPatchTest", str(staged)])
    run([verifier, "-ea", "-Xverify:all", "-cp", test_cp, "DollyProfilerTest"])
    run([verifier, "-ea", "-Xverify:all", "-cp", test_cp, "DollyPerformanceTest"])
    technical_cp = os.pathsep.join(map(str, [tests, staged] + deps))
    run([verifier, "-ea", "-Xverify:all", "-Xbootclasspath/a:" + str(devices), "-cp", technical_cp,
         "DollyTechnicalInputTest", "--integration"])
    for test in ["DollyTechnicalRenderTest", "DollyTechnicalLoadingTest", "DollyTechnicalRuntimeTest", "DollyHudCacheTest", "DollyGroupOneTest", "DollyGroupTwoTest", "DollyGroupThreeTest", "DollyGroupFourTest", "DollyGroupFiveTest", "DollyGroupSixTest", "DollyGroupSevenEightTest", "DollyGroupNineTest", "DollyTactileTest"]:
        run([verifier, "-ea", "-Xverify:all", "-Xbootclasspath/a:" + str(devices), "-cp", technical_cp, test, str(staged)])
    jar = output / (VERSION + ".jar")
    shutil.copyfile(staged, jar)
    profile = json.loads((ROOT / "tools/version-template.json").read_text())
    profile.update({"id": VERSION, "jar": VERSION})
    (output / (VERSION + ".json")).write_text(json.dumps(profile, indent=2) + "\n")
    with zipfile.ZipFile(ROOT / "dist" / (VERSION + ".zip"), "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for file in [jar, output / (VERSION + ".json")]:
            archive.write(file, str(Path(VERSION) / file.name))
    (build / "environment.json").write_text(json.dumps({"vanilla": str(vanilla), "libraries": list(map(str, deps))}, indent=2))
    with zipfile.ZipFile(jar) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)) or any(n.startswith(("org/objectweb/asm/", "net/minecraftforge/fml/common/Loader")) for n in names):
            raise RuntimeError("Unexpected packaged runtime content")
        if any(n.startswith(("DollyRuntimeTest", "DollyPatchTest", "org/lwjgl/")) for n in names):
            raise RuntimeError("Test code or device doubles packaged in client")
        if not {"Config.class", "net/optifine/render/VboRegion.class", "net/optifine/shaders/Shaders.class"}.issubset(names):
            raise RuntimeError("Missing integrated OptiFine runtime")
    (build / "verification.json").write_text(json.dumps({
        "verifier": str(verifier), "core_tests": "passed", "minecraft_settings_integration": "passed",
        "jvm_verification": "passed", "runtime_regressions": "passed", "patch_regressions": "passed", "profiler": "passed", "graphics": "not exercised by automated build",
        "jar_sha256": hashlib.sha256(jar.read_bytes()).hexdigest(),
        "optifine": "1.8.9 HD U M5 (integrated at build time)",
        "performance_regressions": "passed",
        "chat_scoreboard_regressions": "passed",
        "slot_profile_regressions": "passed",
        "technical_features": "passed",
        "tactile_physics_and_profiles": "passed",
        "tactile_runtime": "passed",
        "windows_input": "simulated Win32 API; hardware not exercised",
        "patched_classes": (build / "patches.txt").read_text().splitlines()
    }, indent=2) + "\n")
    print("Built and verified:", jar.relative_to(ROOT))
    print("Launcher version ZIP:", (ROOT / "dist" / (VERSION + ".zip")).relative_to(ROOT))
    print("Prism instance ZIP:", package_prism(jar).relative_to(ROOT))


if __name__ == "__main__":
    try:
        main()
    except (OSError, subprocess.CalledProcessError) as error:
        print("Build failed:", error, file=sys.stderr)
        sys.exit(1)
