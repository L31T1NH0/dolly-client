"""Focused checks for the offline OptiFine decoder and supplied artifact."""
from pathlib import Path
import tempfile
import unittest
import zipfile

from optifine import apply_gdiff, integrate_optifine

HEADER = b"\xd1\xff\xd1\xff\x04"
ROOT = Path(__file__).resolve().parents[1]


class OptiFineTest(unittest.TestCase):
    def test_literal_and_every_copy_width(self):
        widths = {249: (2, 1), 250: (2, 2), 251: (2, 4), 252: (4, 1),
                  253: (4, 2), 254: (4, 4), 255: (8, 4)}
        for command, (offset_width, size_width) in widths.items():
            delta = HEADER + b"\x02XY" + bytes([command]) + (2).to_bytes(offset_width, "big") + (3).to_bytes(size_width, "big") + b"\x00"
            self.assertEqual(apply_gdiff(b"abcdef", delta), b"XYcde")
        for command, width in ((247, 2), (248, 4)):
            self.assertEqual(apply_gdiff(b"", HEADER + bytes([command]) + (3).to_bytes(width, "big") + b"abc"), b"abc")

    def test_malformed_patches(self):
        for delta in (b"wrong", HEADER + b"\x03ab", HEADER + b"\xf7\x00", HEADER + b"\xf9\x00\x04\x03"):
            with self.subTest(delta=delta), self.assertRaises(ValueError):
                apply_gdiff(b"abcdef", delta)

    def test_supplied_artifact(self):
        vanilla = Path.home() / ".local/share/PrismLauncher/libraries/com/mojang/minecraft/1.8.9/minecraft-1.8.9-client.jar"
        optifine = ROOT / "references/OptiFine_1.8.9_HD_U_M5.jar"
        if not vanilla.is_file() or not optifine.is_file():
            self.skipTest("Local vanilla and OptiFine inputs required")
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "merged.jar"
            report = integrate_optifine(vanilla, optifine, output)
            self.assertEqual(report["patched_classes"], 218)
            self.assertEqual(report["patched_resources"], 832)
            with zipfile.ZipFile(output) as archive:
                self.assertIsNone(archive.testzip())
                names = archive.namelist()
                self.assertIn("Config.class", names)
                self.assertIn("assets/minecraft/mcpatcher/ctm/default/0_glass_white/0.png", names)
                self.assertIn("net/optifine/render/VboRegion.class", names)
                self.assertIn("net/minecraft/client/main/Main.class", names)
                self.assertFalse(any(name.startswith(("patch/", "META-INF/")) or name.endswith(".xdelta") for name in names))
                self.assertNotIn("optifine/OptiFineClassTransformer.class", names)
                self.assertNotIn("launchwrapper-of-2.2.jar", names)
            before = output.read_bytes()
            with self.assertRaises(ValueError):
                integrate_optifine(optifine, optifine, output)
            self.assertEqual(output.read_bytes(), before)


if __name__ == "__main__":
    unittest.main()
