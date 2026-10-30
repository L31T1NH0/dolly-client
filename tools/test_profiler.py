import contextlib
import io
import json
import tempfile
import unittest
from pathlib import Path
from analyze_profile import read, report, union_length


class ProfileAnalysisTest(unittest.TestCase):
    def test_partial_capture_and_percentiles(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "profile.jsonl"
            rows = [{"type": "session", "mode": "NORMAL"}]
            rows += [{"type": "frame", "index": i + 1, "duration_ns": ms * 1000000,
                      "draw_calls": 2, "rendered_entities": 3, "particles": 4}
                     for i, ms in enumerate([10, 20, 30, 100])]
            rows += [{"type": "aggregate", "name": "tick", "calls": 2,
                      "total_ns": 10000000, "max_ns": 7000000}]
            path.write_text("\n".join(json.dumps(r) for r in rows) + '\n{"partial":', encoding="utf-8")
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                meta, frames, scopes, summary = read(path)
                result = report(path, 10)
            self.assertEqual(len(frames), 4)
            self.assertEqual(meta["mode"], "NORMAL")
            self.assertEqual(len(scopes), 1)
            self.assertIsNone(summary)
            self.assertEqual(result[1]["frame médio (ms)"], 40)
            self.assertEqual(result[1]["p95 (ms)"], 100)
            self.assertIn("sem resumo final", output.getvalue())
            self.assertIn("registro incompleto", output.getvalue())

    def test_gpu_gc_overlap_and_exclusive_report(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "profile.jsonl"
            rows = [
                {"type": "session", "mode": "DEEP", "schema": 2},
                {"type": "frame", "index": 7, "start_ns": 10, "duration_ns": 10,
                 "draw_calls": 1, "rendered_entities": 2, "particles": 3},
                {"type": "gpu", "frame_index": 7, "duration_ns": 6, "reason": 0},
                {"type": "gpu", "frame_index": 8, "duration_ns": -1, "reason": 4},
                {"type": "gc_event", "start_ns": 5, "duration_ns": 12, "pause_ns": 12},
                {"type": "gc_event", "start_ns": 15, "duration_ns": 10, "pause_ns": 10},
                {"type": "gc_event", "start_ns": 10, "duration_ns": 30, "pause_ns": -1},
                {"type": "aggregate", "name": "parent", "calls": 1, "total_ns": 100, "exclusive_ns": 1, "max_ns": 100},
                {"type": "aggregate", "name": "child", "calls": 1, "total_ns": 99, "exclusive_ns": 99, "max_ns": 99},
                {"type": "summary", "dropped_records": 0, "gpu_issued": 2, "gpu_collected": 1}]
            path.write_text("\n".join(json.dumps(row) for row in rows), encoding="utf-8")
            _, frames, _, _ = read(path)
            self.assertEqual(frames[0]["gpu_ns"], 6)
            self.assertEqual(frames[0]["gc_events"], 3)
            self.assertEqual(frames[0]["gc_pause_overlap_ns"], 10)
            self.assertEqual(union_length([(1, 5), (3, 8)]), 7)
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                report(path, 1)
            table = output.getvalue().split("Operações por tempo", 1)[1]
            self.assertIn("child", table)
            self.assertNotIn("parent", table)
            self.assertIn("1/2 resultados", output.getvalue())


if __name__ == "__main__":
    unittest.main()
