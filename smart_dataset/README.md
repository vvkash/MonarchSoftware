# Monarch Smart Dataset

This folder defines the working dataset contract for the Monarch authentication
experiments across gait, keystroke, touchstroke, and ECG.

The raw participant data is intentionally not committed. Place exports under
`data/participants/<participant>/<modality>/`, then run the validator:

```sh
python3 smart_dataset/validate_dataset.py smart_dataset/data
```

Expected participant IDs for the current study batch:

- `aakash`
- `issam`
- `houston`

## Folder Layout

```text
smart_dataset/
  manifest.example.json
  validate_dataset.py
  data/
    participants/
      aakash/
        gait/
        keystroke/
        touchstroke/
        ecg/
      issam/
        gait/
        keystroke/
        touchstroke/
        ecg/
      houston/
        gait/
        keystroke/
        touchstroke/
        ecg/
```

## Source Exports

| Modality | Source app | Accepted files | Key schema |
| --- | --- | --- | --- |
| `gait` | Monarch Biometrics | `*.csv` | `TimeStamp, Acc_x, Acc_y, Acc_z, Gyr_x, Gyr_y, Gyr_z, Mag_x, Mag_y, Mag_z, Application Scenario, Subject` |
| `keystroke` | Monarch PIN or typing study | `*.csv` | PIN 45-feature table, or typing study exports with `iki_data`, `press_data`, `key_events` |
| `touchstroke` | Monarch swipe/social-media tasks | `*.csv` | Swipe exports with `touch_events`, or raw phase-2 touch exports with `gesture_id`, `event_type`, `pressure`, `touch_size` |
| `ecg` | GalaxyWatchHeart ECG | `*.csv` | `timestamp_ms,mv` |

## Training Readiness Rules

The validator checks that all three participants have each modality and that the
files can be parsed as CSV with the expected fields. It also reports whether
there are enough samples for initial model training and authentication testing.

Default minimums are conservative placeholders:

- gait: `100` rows per participant
- keystroke: `10` rows per participant
- touchstroke: `10` rows per participant
- ECG: `500` rows per participant

For authentication experiments, keep enrollment and test sessions separate by
recording at least two files per participant per modality whenever possible.

## On-Device Training Starting Point

The existing PIN keystroke project already trains on the Android phone:

- 45 input features from a 6-digit PIN entry
- 8 hidden units
- 1 sigmoid output
- Backpropagation implemented in Java
- Weights persisted into SQLite

Relevant classes:

- `app/src/main/java/com/monarch/software/keystroke/KeystrokeMainActivity.java`
- `app/src/main/java/com/monarch/software/keystroke/trainingActivity.java`
- `app/src/main/java/com/monarch/software/keystroke/trainingData.java`
- `app/src/main/java/com/monarch/software/keystroke/errorBackPropagation.java`

The existing model is now routed through `OnDeviceTrainer`. The next on-device
step is to add a second implementation for TensorFlow Lite transfer learning or
another Android-compatible runtime for HMAE fine-tuning.
