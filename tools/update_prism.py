#!/usr/bin/env python3
"""Atualiza uma instância Dolly existente sem substituir dados do jogador."""
import argparse
from datetime import datetime
import io
import json
from pathlib import Path
import tempfile
import zipfile

from package_prism import CLIENT_VERSION

FILES = ("mmc-pack.json", "patches/customjar.json", "libraries/customjar-1.jar")


def replace_file(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(dir=path.parent, delete=False) as tmp:
        temporary = Path(tmp.name)
        try:
            tmp.write(data)
            tmp.flush()
        except BaseException:
            temporary.unlink(missing_ok=True)
            raise
    try:
        temporary.replace(path)
    finally:
        temporary.unlink(missing_ok=True)


def update(instance, package):
    instance, package = Path(instance).resolve(), Path(package).resolve()
    if not (instance / "instance.cfg").is_file():
        raise ValueError("Informe a pasta da instância do Prism, que contém instance.cfg.")
    pack = json.loads((instance / "mmc-pack.json").read_text(encoding="utf-8"))
    components = pack["components"]
    custom = [c for c in components if c.get("uid") == "customjar"]
    patch_path = instance / "patches/customjar.json"
    if len(custom) != 1 or json.loads(patch_path.read_text(encoding="utf-8")).get("name") != "Dolly Client":
        raise ValueError("A instância não foi reconhecida como Dolly Client.")
    with zipfile.ZipFile(package) as archive:
        patch_data = archive.read("Dolly-Client/patches/customjar.json")
        patch = json.loads(patch_data)
        if (patch.get("uid") != "customjar" or patch.get("name") != "Dolly Client"
                or patch.get("mainJar") != {"name": "custom:customjar:1", "MMC-hint": "local"}):
            raise ValueError("Pacote Dolly incompatível.")
        jar = archive.read("Dolly-Client/libraries/customjar-1.jar")
    with zipfile.ZipFile(io.BytesIO(jar)) as client:
        if client.testzip() is not None or "ave.class" not in client.namelist() or "DollySettings.class" not in client.namelist():
            raise ValueError("JAR Dolly inválido.")
    custom[0]["version"] = patch["version"]
    custom[0]["cachedName"] = "Dolly Client"
    data = ((json.dumps(pack, indent=2) + "\n").encode(), patch_data, jar)
    originals = {name: (instance / name).read_bytes() for name in FILES}
    backups = instance / "dolly-update-backups"
    backups.mkdir(exist_ok=True)
    backup = backups / (datetime.now().strftime("%Y%m%d-%H%M%S-%f") + ".zip")
    with zipfile.ZipFile(backup, "x", compression=zipfile.ZIP_DEFLATED) as archive:
        for name, content in originals.items():
            archive.writestr(name, content)
    try:
        for name, content in zip(FILES, data):
            replace_file(instance / name, content)
    except BaseException:
        for name, content in originals.items():
            replace_file(instance / name, content)
        raise
    return patch["version"], backup


def main():
    parser = argparse.ArgumentParser(description=__doc__, epilog="Feche o jogo e a instância no Prism antes de atualizar.")
    parser.add_argument("instance", type=Path, help="Pasta da instância (contém instance.cfg), não a pasta .minecraft")
    parser.add_argument("--package", type=Path, default=Path(__file__).resolve().parents[1] / "dist" / ("Dolly-Client-" + CLIENT_VERSION + "-Prism.zip"))
    args = parser.parse_args()
    try:
        version, backup = update(args.instance, args.package)
    except (OSError, ValueError, KeyError, zipfile.BadZipFile) as error:
        parser.exit(1, "Falha ao atualizar: " + str(error) + "\n")
    print("Dolly atualizado para " + version + ". Configurações e dados preservados.")
    print("Backup: " + str(backup))
    print("O nome personalizado da instância no Prism foi preservado.")


if __name__ == "__main__":
    main()
