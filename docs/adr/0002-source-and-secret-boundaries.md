# ADR 0002: Source and Secret Boundaries

## Status

Accepted

## Context

The app must support JSON playlist directories, direct M3U URLs, and Xtream-compatible accounts. Xtream is not treated as a single official standard, and credentials must not leak through logs, analytics, crash reports, or exports.

## Decision

Represent every source with a stable `PlaylistSource` domain model and keep source-specific parsing in `data:playlist`. Store credential references on source records rather than raw passwords. The current in-memory prototype emits `keystore://xtream/{sourceId}` references; the next persistence slice will back these references with Android Keystore-based encrypted storage.

All network logging surfaces must pass through `SecretRedactor` before text leaves the network layer.

## Consequences

UI and domain code can list and route sources without knowing how credentials are stored. Parser tests and redaction tests protect the earliest high-risk behavior before Room/DataStore are added.
