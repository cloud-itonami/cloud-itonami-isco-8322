# cloud-itonami-isco-8322

Open Occupation Blueprint for **ISCO-08 8322**: Car, Taxi and Van Drivers.

This repository designs a forkable OSS business for an independent car/taxi/van driver: a vehicle-telemetry robot performs pre-trip inspection checks under a governor-gated actor, so the operator keeps their own trip and duty-hours records instead of renting a closed fleet-dispatch SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a vehicle-telemetry robot performs pre-trip inspection checks and cargo/passenger-area sensing under an actor that proposes
actions and an independent **Car Van Driving Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
driving beyond a duty-hours limit, or accepting a trip flagged as high-risk) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
trip request + duty-hours log + vehicle-inspection record
        |
        v
Driving Advisor -> Car Van Driving Governor -> drive/drop-off, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `8322`). Required capabilities:

- :robotics
- :telemetry
- :identity
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
