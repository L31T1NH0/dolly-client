"""Updater regressions run entirely against temporary instances."""
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile

from package_prism import package_prism, CLIENT_VERSION
import update_prism


class UpdateTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        jar = self.root / 'dist/client/client.jar'
        jar.parent.mkdir(parents=True)
        with zipfile.ZipFile(jar, 'w') as archive:
            archive.writestr('ave.class', b'fixture')
            archive.writestr('DollySettings.class', b'fixture')
        self.package = package_prism(jar)
        self.instance = self.root / 'instance'
        with zipfile.ZipFile(self.package) as archive:
            archive.extractall(self.root / 'unpacked')
        (self.root / 'unpacked/Dolly-Client').rename(self.instance)
        pack = json.loads((self.instance / 'mmc-pack.json').read_text())
        pack['components'][-1]['version'] = '0.1.6'
        pack['components'].append({'uid': 'user.component', 'version': '1'})
        (self.instance / 'mmc-pack.json').write_text(json.dumps(pack))
        for name in ('instance.cfg', '.minecraft/options.txt', '.minecraft/dolly/config.properties', '.minecraft/saves/world/level.dat'):
            path = self.instance / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(b'user settings')
        self.before = {p.relative_to(self.instance).as_posix(): p.read_bytes() for p in self.instance.rglob('*') if p.is_file()}

    def test_update_preserves_user_data_and_backup(self):
        version, backup = update_prism.update(self.instance, self.package)
        self.assertEqual(version, CLIENT_VERSION)
        pack = json.loads((self.instance / 'mmc-pack.json').read_text())
        self.assertEqual(pack['components'][-2]['version'], CLIENT_VERSION)
        self.assertEqual(pack['components'][-1]['uid'], 'user.component')
        for name, content in self.before.items():
            if name not in update_prism.FILES:
                self.assertEqual((self.instance / name).read_bytes(), content)
        with zipfile.ZipFile(backup) as archive:
            for name in update_prism.FILES:
                self.assertEqual(archive.read(name), self.before[name])

    def test_bad_package_leaves_instance_unchanged(self):
        self.package.write_bytes(b'not a zip')
        with self.assertRaises(zipfile.BadZipFile):
            update_prism.update(self.instance, self.package)
        for name, content in self.before.items():
            self.assertEqual((self.instance / name).read_bytes(), content)

    def test_write_failure_rolls_back(self):
        original = update_prism.replace_file
        calls = 0
        def failing(path, data):
            nonlocal calls
            calls += 1
            if calls == 2:
                raise OSError('simulated write failure')
            original(path, data)
        with patch.object(update_prism, 'replace_file', failing):
            with self.assertRaises(OSError):
                update_prism.update(self.instance, self.package)
        for name, content in self.before.items():
            self.assertEqual((self.instance / name).read_bytes(), content)


if __name__ == '__main__':
    unittest.main()
