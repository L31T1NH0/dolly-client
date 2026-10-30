#!/usr/bin/env python3
"""Run the optional headless OFF/NORMAL/DEEP calibration after tools/build.py."""
import json
import os
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def main():
    environment = ROOT / "build/environment.json"
    if not environment.is_file() or not (ROOT / "build/tests/DollyProfilerBenchmark.class").is_file():
        raise SystemExit("Execute python3 tools/build.py antes do benchmark.")
    libraries = json.loads(environment.read_text())["libraries"]
    java8 = Path("/usr/lib/jvm/java-8-openjdk-amd64/bin/java")
    java = str(java8) if java8.is_file() else shutil.which("java") or "java"
    cp = [ROOT / "build/tests", ROOT / "dist/dolly-1.8.9/dolly-1.8.9.jar"] + libraries
    print("Calibração sintética sem GPU/rede reais; não estima perda de FPS numa partida.", flush=True)
    subprocess.run([java, "-cp", os.pathsep.join(map(str, cp)), "DollyProfilerBenchmark"], check=True)


if __name__ == "__main__":
    main()
