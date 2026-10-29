"""Expand Falley schema-1 input batches from GA4 BigQuery JSON exports. No network access."""
import argparse
import csv
import json
import math
from pathlib import Path
import re

FIELDS = ["run_id", "build_channel", "stream", "time_ms", "x", "y", "z", "rotation", "phase", "width", "height"]

def params(event):
    return {p["key"]: next((v for v in p.get("value", {}).values() if v is not None), None)
            for p in event.get("event_params", [])}

def decode(event):
    if event.get("event_name") != "falley_input_batch":
        return
    values = params(event)
    if int(values.get("schema", 0)) != 1:
        raise ValueError("Unsupported Falley input schema")
    run_id = str(values.get("run_id", ""))
    stream = values.get("stream")
    channel = values.get("build_channel", "")
    count = int(values.get("sample_count", 0))
    if not re.fullmatch(r"[0-9a-f-]{1,36}", run_id) or stream not in ("accel", "gyro", "touch") or channel not in ("debug", "release", "") or not 1 <= count <= 10:
        raise ValueError("Invalid batch metadata")
    for index in range(count):
        fields = str(values.get("s" + str(index), "")).split(",")
        if len(fields) != (6 if stream == "touch" else 5):
            raise ValueError("Invalid sample width")
        row = dict.fromkeys(FIELDS, "")
        row.update(run_id=run_id, build_channel=channel, stream=stream, time_ms=int(fields[0]))
        if row["time_ms"] < 0:
            raise ValueError("Negative sample time")
        if stream == "touch":
            row.update(phase=int(fields[1]), x=float(fields[2]), y=float(fields[3]), width=int(fields[4]), height=int(fields[5]))
            if row["phase"] not in range(4) or row["width"] <= 0 or row["height"] <= 0:
                raise ValueError("Invalid touch dimensions/phase")
        else:
            row.update(x=float(fields[1]), y=float(fields[2]), z=float(fields[3]), rotation=int(fields[4]))
            if row["rotation"] not in range(4):
                raise ValueError("Invalid device rotation")
        for axis in ("x", "y", "z"):
            if row[axis] != "" and (not math.isfinite(row[axis]) or abs(row[axis]) > 100000):
                raise ValueError("Invalid input coordinate")
        yield row

def events(path):
    with path.open(encoding="utf-8-sig") as source:
        leading = source.read(1024).lstrip()
        source.seek(0)
        if leading.startswith("["):
            yield from json.load(source)
        else:
            for line in source:
                if line.strip():
                    yield json.loads(line)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    count = 0
    # Refuse to overwrite earlier analysis. No user identifiers are copied to CSV.
    with args.output.open("x", encoding="utf-8", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=FIELDS)
        writer.writeheader()
        for event in events(args.input):
            for row in decode(event):
                writer.writerow(row)
                count += 1
    print(f"Decoded {count} input samples. Keep the CSV private.")

if __name__ == "__main__":
    main()
