#!/usr/bin/env python3
"""Validate Monarch smart-dataset exports for training/authentication use."""

from __future__ import annotations

import argparse
import csv
import json
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable


REQUIRED_PARTICIPANTS = ("aakash", "issam", "houston")
REQUIRED_MODALITIES = ("gait", "keystroke", "touchstroke", "ecg")

MIN_ROWS = {
    "gait": 100,
    "keystroke": 10,
    "touchstroke": 10,
    "ecg": 500,
}

GAIT_COLUMNS = {
    "TimeStamp",
    "Acc_x",
    "Acc_y",
    "Acc_z",
    "Gyr_x",
    "Gyr_y",
    "Gyr_z",
    "Mag_x",
    "Mag_y",
    "Mag_z",
}

ECG_COLUMNS = {"timestamp_ms", "mv"}

TYPING_COLUMNS = {
    "iki_data",
    "press_data",
    "key_events",
    "total_time_ms",
    "session_timestamp",
}

PIN_FEATURE_COLUMNS = {
    "b1_x",
    "b1_y",
    "b2_x",
    "b2_y",
    "b3_x",
    "b3_y",
    "b4_x",
    "b4_y",
    "b5_x",
    "b5_y",
    "b6_x",
    "b6_y",
    "b1_size",
    "b2_size",
    "b3_size",
    "b4_size",
    "b5_size",
    "b6_size",
    "p1_press",
    "p2_press",
    "p3_press",
    "p4_press",
    "p5_press",
    "p6_press",
    "b1_pressure",
    "b2_pressure",
    "b3_pressure",
    "b4_pressure",
    "b5_pressure",
    "b6_pressure",
    "p1_p2_flight",
    "p2_p3_flight",
    "p3_p4_flight",
    "p4_p5_flight",
    "p5_p6_flight",
    "p1_p2_digraph",
    "p2_p3_digraph",
    "p3_p4_digraph",
    "p4_p5_digraph",
    "p5_p6_digraph",
    "p1_p2_trigraph",
    "p2_p3_trigraph",
    "p3_p4_trigraph",
    "p4_p5_trigraph",
    "total_time",
}

SWIPE_COLUMNS = {
    "stroke_index",
    "touch_events",
    "duration_ms",
    "direction",
    "session_timestamp",
}

RAW_TOUCH_COLUMNS = {
    "task_type",
    "gesture_id",
    "event_type",
    "x",
    "y",
    "pressure",
    "touch_size",
    "timestamp_ms",
}


@dataclass
class CsvCheck:
    path: Path
    rows: int = 0
    schema: str = "unknown"
    errors: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)


def load_manifest(root: Path) -> dict:
    manifest_path = root / "manifest.json"
    if not manifest_path.exists():
        return {}
    with manifest_path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def csv_files(path: Path) -> list[Path]:
    if not path.exists():
        return []
    return sorted(p for p in path.rglob("*.csv") if p.is_file())


def sniff_schema(modality: str, headers: set[str]) -> str | None:
    if modality == "gait" and GAIT_COLUMNS.issubset(headers):
        return "monarch_biometrics_gait"
    if modality == "ecg" and ECG_COLUMNS.issubset(headers):
        return "galaxy_watch_ecg"
    if modality == "keystroke" and PIN_FEATURE_COLUMNS.issubset(headers):
        return "pin_keystroke_45_feature"
    if modality == "keystroke" and TYPING_COLUMNS.issubset(headers):
        return "typing_study"
    if modality == "touchstroke" and SWIPE_COLUMNS.issubset(headers):
        return "swipe_study"
    if modality == "touchstroke" and RAW_TOUCH_COLUMNS.issubset(headers):
        return "phase2_raw_touch"
    return None


def count_rows(path: Path) -> tuple[list[str], int, list[str]]:
    warnings: list[str] = []
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.reader(handle)
        try:
            headers = next(reader)
        except StopIteration:
            return [], 0, ["empty CSV"]
        row_count = 0
        expected_len = len(headers)
        for row_number, row in enumerate(reader, start=2):
            if not any(cell.strip() for cell in row):
                continue
            row_count += 1
            if len(row) != expected_len:
                warnings.append(
                    f"row {row_number} has {len(row)} columns, expected {expected_len}"
                )
    return headers, row_count, warnings


def check_csv(path: Path, modality: str) -> CsvCheck:
    check = CsvCheck(path=path)
    try:
        headers, rows, warnings = count_rows(path)
    except UnicodeDecodeError as exc:
        check.errors.append(f"not UTF-8 CSV: {exc}")
        return check
    except csv.Error as exc:
        check.errors.append(f"CSV parse error: {exc}")
        return check

    check.rows = rows
    check.warnings.extend(warnings[:5])
    schema = sniff_schema(modality, set(headers))
    if schema is None:
        check.errors.append(
            "unrecognized schema; headers were: " + ", ".join(headers[:12])
        )
    else:
        check.schema = schema
    if rows == 0:
        check.errors.append("no data rows")
    return check


def participant_ids(manifest: dict) -> tuple[str, ...]:
    participants = manifest.get("participants")
    if not participants:
        return REQUIRED_PARTICIPANTS
    ids = []
    for participant in participants:
        if isinstance(participant, dict) and participant.get("id"):
            ids.append(str(participant["id"]).lower())
        elif isinstance(participant, str):
            ids.append(participant.lower())
    return tuple(ids) or REQUIRED_PARTICIPANTS


def print_issue(prefix: str, issue: str) -> None:
    print(f"  {prefix} {issue}")


def validate(root: Path) -> int:
    manifest = load_manifest(root)
    participants = participant_ids(manifest)
    failed = False

    print(f"Dataset root: {root}")
    print(f"Participants: {', '.join(participants)}")
    print()

    for participant in participants:
        participant_root = root / "participants" / participant
        print(f"[{participant}]")
        if not participant_root.exists():
            print_issue("FAIL", "missing participant folder")
            failed = True
            print()
            continue

        for modality in REQUIRED_MODALITIES:
            modality_root = participant_root / modality
            files = csv_files(modality_root)
            total_rows = 0
            valid_files = 0

            if not files:
                print_issue("FAIL", f"{modality}: no CSV files found")
                failed = True
                continue

            for file_path in files:
                check = check_csv(file_path, modality)
                rel = file_path.relative_to(root)
                total_rows += check.rows
                if check.errors:
                    failed = True
                    print_issue("FAIL", f"{modality}: {rel}")
                    for error in check.errors:
                        print_issue("     -", error)
                else:
                    valid_files += 1
                    print_issue(
                        "OK",
                        f"{modality}: {rel} ({check.rows} rows, {check.schema})",
                    )
                for warning in check.warnings:
                    print_issue("WARN", f"{modality}: {rel}: {warning}")

            min_rows = MIN_ROWS[modality]
            if total_rows < min_rows:
                failed = True
                print_issue(
                    "FAIL",
                    f"{modality}: {total_rows} total rows, need at least {min_rows}",
                )
            elif valid_files < 2:
                print_issue(
                    "WARN",
                    f"{modality}: only {valid_files} valid file; use separate enrollment/test sessions for authentication",
                )
            else:
                print_issue(
                    "OK",
                    f"{modality}: {total_rows} total rows across {valid_files} valid files",
                )
        print()

    if failed:
        print("Result: NOT READY for full model training/authentication.")
        return 1

    print("Result: READY for initial model training/authentication experiments.")
    return 0


def parse_args(argv: Iterable[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Validate Monarch smart-dataset participant exports."
    )
    parser.add_argument(
        "root",
        nargs="?",
        default=Path(__file__).resolve().parent / "data",
        type=Path,
        help="Dataset root containing participants/<id>/<modality>/ folders.",
    )
    return parser.parse_args(argv)


def main(argv: Iterable[str]) -> int:
    args = parse_args(argv)
    return validate(args.root.resolve())


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
