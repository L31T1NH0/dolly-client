"""Package a Dolly build as an importable PrismLauncher instance."""
import configparser
import hashlib
import json
from pathlib import Path
import zipfile

CLIENT_VERSION = "0.1.16"


def package_prism(jar):
    jar = Path(jar)
    components = [
        {"uid": "org.lwjgl", "version": "2.9.4-nightly-20150209", "dependencyOnly": True},
        {"uid": "net.minecraft", "version": "1.8.9", "important": True},
        {"uid": "customjar", "version": CLIENT_VERSION, "cachedName": "Dolly Client"},
    ]
    # Same local mainJar mechanism as Prism's installCustomJar_internal.
    patch = {
        "formatVersion": 1, "name": "Dolly Client", "uid": "customjar", "version": CLIENT_VERSION,
        "mainJar": {"name": "custom:customjar:1", "MMC-hint": "local"},
    }
    config = "[General]\nInstanceType=OneSix\nConfigVersion=1.3\nname=Dolly Client " + CLIENT_VERSION + "\niconKey=default\n"
    config += "OverrideMemory=true\nMinMemAlloc=256\nMaxMemAlloc=1024\n"
    # Linux Java 8 may reference an AtkWrapper absent from its classpath.
    config += "OverrideJavaArgs=true\nJvmArgs=-Djavax.accessibility.assistive_technologies=\n"
    java8 = Path("/usr/lib/jvm/java-8-openjdk-amd64/bin/java")
    if java8.is_file():
        config += "OverrideJavaLocation=true\nAutomaticJava=false\nJavaPath=" + str(java8) + "\n"
    else:
        config += "OverrideJavaLocation=false\nAutomaticJava=true\n"
    output = jar.parent.parent / ("Dolly-Client-" + CLIENT_VERSION + "-Prism.zip")
    prefix = "Dolly-Client/"
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        archive.writestr(prefix + "instance.cfg", config)
        archive.writestr(prefix + "mmc-pack.json", json.dumps({"formatVersion": 1, "components": components}, indent=2) + "\n")
        archive.writestr(prefix + "patches/customjar.json", json.dumps(patch, indent=2) + "\n")
        archive.write(jar, prefix + "libraries/customjar-1.jar")
    # Check the actual archive, including Prism's flat local-library lookup.
    with zipfile.ZipFile(output) as archive:
        assert archive.testzip() is None, "Corrupt Prism archive"
        parsed_config = configparser.ConfigParser()
        parsed_config.read_string(archive.read(prefix + "instance.cfg").decode())
        assert parsed_config["General"]["InstanceType"] == "OneSix"
        settings = parsed_config["General"]
        assert settings.getboolean("OverrideMemory")
        assert settings.getint("MinMemAlloc") == 256 and settings.getint("MaxMemAlloc") == 1024
        assert settings.getboolean("OverrideJavaArgs")
        assert settings["JvmArgs"] == "-Djavax.accessibility.assistive_technologies="
        parsed_patch = json.loads(archive.read(prefix + "patches/customjar.json"))
        main_jar = parsed_patch["mainJar"]
        assert main_jar["MMC-hint"] == "local" and "downloads" not in main_jar
        _, artifact, version = main_jar["name"].split(":")
        bundled = archive.read(prefix + "libraries/" + artifact + "-" + version + ".jar")
        assert hashlib.sha256(bundled).digest() == hashlib.sha256(jar.read_bytes()).digest()
        pack = json.loads(archive.read(prefix + "mmc-pack.json"))
        assert pack["components"][-1]["uid"] == parsed_patch["uid"]
    return output
