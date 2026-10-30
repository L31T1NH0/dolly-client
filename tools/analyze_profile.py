#!/usr/bin/env python3
"""Summarize one Dolly JSONL capture or compare two captures (before, after)."""
import argparse
import json
from pathlib import Path


def read(path):
    frames, aggregates, meta, summary = [], [], {}, None
    gpu, gc_events, network, gpu_reasons = {}, [], None, {}
    with Path(path).open(encoding="utf-8") as source:
        for number, line in enumerate(source, 1):
            try:
                row = json.loads(line)
            except json.JSONDecodeError:
                print(f"{path}:{number}: registro incompleto ignorado")
                continue
            kind = row.get("type")
            if kind == "frame":
                frames.append(row)
            elif kind == "aggregate":
                aggregates.append(row)
            elif kind == "session":
                meta = row
            elif kind == "summary":
                summary = row
            elif kind == "gpu":
                if row["reason"] == 0:
                    gpu[row["frame_index"]] = row["duration_ns"]
                else:
                    gpu_reasons[row["reason"]] = gpu_reasons.get(row["reason"], 0) + 1
            elif kind == "gc_event":
                gc_events.append(row)
            elif kind == "network_summary":
                network = row
    events = sorted(gc_events, key=lambda e: e["start_ns"])
    active, index = [], 0
    for frame in sorted(frames, key=lambda f: f.get("start_ns", 0)):
        start = frame.get("start_ns", 0)
        end = start + frame["duration_ns"]
        while index < len(events) and events[index]["start_ns"] < end:
            active.append(events[index])
            index += 1
        active = [e for e in active if e["start_ns"] + e["duration_ns"] > start]
        frame["gpu_ns"] = gpu.get(frame["index"])
        frame["gc_events"] = len(active)
        intervals = [(max(start, e["start_ns"]), min(end, e["start_ns"] + e["duration_ns"]))
                     for e in active if e.get("pause_ns", -1) >= 0]
        frame["gc_pause_overlap_ns"] = union_length(intervals)
    meta["_network"] = network
    meta["_gpu_reasons"] = gpu_reasons
    meta["_gc_events"] = len(events)
    return meta, frames, aggregates, summary


def union_length(intervals):
    total, last_end = 0, None
    for start, end in sorted(intervals):
        effective = start if last_end is None else max(start, last_end)
        total += max(0, end - effective)
        last_end = end if last_end is None else max(last_end, end)
    return total


def report(path, top, sort="exclusive"):

    meta, frames, aggregates, summary = read(path)
    print(f"\n{path} — {meta.get('mode', '?')}")
    if summary is None:
        print("Captura sem resumo final: parada abrupta ou gravação em andamento.")
    elif summary["dropped_records"]:
        print(f"Registros descartados: {summary['dropped_records']} (agregados preservados).")
    print(f"Eventos de GC: {meta['_gc_events']} | Resultados GPU indisponíveis: {meta['_gpu_reasons']}")
    if summary and summary.get("gpu_issued", 0) > summary.get("gpu_collected", 0):
        print(f"GPU: {summary['gpu_collected']}/{summary['gpu_issued']} resultados coletados.")
    if summary:
        print(f"Frames DEEP amostrados: {summary.get('sampled_frames', '?')} | "
              f"Escopos omitidos por orçamento: {summary.get('deep_scopes_skipped', 0)} | "
              f"Overflow de pilha: {summary.get('stack_overflows', 0)} | "
              f"Chamadas sem thread registrada: {summary.get('untracked_calls', 0)}")
    network = meta["_network"]
    if network:
        calls = network['handled_packets']
        print(f"Rede: {calls} handlers, espera média {network['queue_total_ns']/max(1,calls)/1e6:.3f} ms, "
              f"máxima {network['queue_max_ns']/1e6:.3f} ms, backlog máximo {network['pending_max']}")
        print(f"Bytes de protocolo: entrada {network['bytes_in']}, saída {network['bytes_out']}")
    if not frames:
        print("Sem frames exportados.")
        return None
    times = sorted(f["duration_ns"] / 1e6 for f in frames)
    mean = sum(times) / len(times)
    from math import ceil
    quantile = lambda p: times[max(0, ceil(len(times) * p) - 1)]
    result = {"frame médio (ms)": mean, "p95 (ms)": quantile(.95), "p99 (ms)": quantile(.99)}
    print(f"Frames exportados: {len(times)} | FPS médio: {1000 / mean:.2f}")
    print(" | ".join(f"{k}: {v:.3f}" for k, v in result.items()))
    print(f"Operações por tempo {sort} (exclusivo desconta apenas filhos instrumentados):")
    print(f"{'operação':70} {'chamadas':>10} {'inclusivo ms':>13} {'exclusivo ms':>13} {'máx ms':>12}")
    key = "exclusive_ns" if sort == "exclusive" else "total_ns"
    for row in sorted(aggregates, key=lambda r: r.get(key, r["total_ns"]), reverse=True)[:top]:
        count = row["calls"]
        print(f"{row['name']:70} {count:10d} {row['total_ns']/1e6:12.3f} "
              f"{row['exclusive_ns']/1e6:13.3f} {row['max_ns']/1e6:12.3f}" if "exclusive_ns" in row else
              f"{row['name']:70} {count:10d} {row['total_ns']/1e6:12.3f} {'indisponível':>13} {row['max_ns']/1e6:12.3f}")
    print("Frames mais lentos (índice, ms, draw calls, entidades renderizadas, partículas):")
    for f in sorted(frames, key=lambda r: r["duration_ns"], reverse=True)[:5]:
        print(f"  {f['index']}: {f['duration_ns']/1e6:.3f} ms, {f['draw_calls']} draws, "
              f"{f['rendered_entities']} entidades, {f['particles']} partículas, "
              f"GPU {f['gpu_ns']/1e6 if f['gpu_ns'] is not None else '?'} ms, "
              f"{f['gc_events']} eventos GC sobrepostos, pausas conhecidas {f['gc_pause_overlap_ns']/1e6:.3f} ms")
    return meta, result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("captures", nargs="+", type=Path)
    parser.add_argument("--top", type=int, default=20)
    parser.add_argument("--sort", choices=["exclusive", "inclusive"], default="exclusive")
    args = parser.parse_args()
    if not 1 <= len(args.captures) <= 2:
        parser.error("informe uma captura ou duas (antes, depois)")
    results = [report(path, args.top, args.sort) for path in args.captures]
    if len(results) == 2 and all(results):
        if any(results[0][0].get(k) != results[1][0].get(k) for k in ("mode", "schema", "deep_categories", "deep_every_frame", "max_scopes_per_thread_frame")):
            print("\nConfigurações de coleta diferentes: overhead diferente; comparação não equivalente.")
        print("\nDepois / antes:")
        for key, before in results[0][1].items():
            after = results[1][1][key]
            delta = (after / before - 1) * 100 if before else 0
            print(f"  {key}: {before:.3f} → {after:.3f} ({delta:+.1f}%)")


if __name__ == "__main__":
    main()
