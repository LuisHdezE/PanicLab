# I8 Closeout — TASK-KMP-080 / TASK-KMP-081

## Baseline

- Current integrated baseline before this documentation-only reconciliation: `main@1fa8d73a7e4fdfe9c56ba8cc067ce8d596be801e`.
- I7 is DONE/CLOSED.
- I8 is capability-gated rather than globally authorized.

## TASK-KMP-080 — DONE

The I8 capability roadmap was created and merged through PR #24.

- roadmap implementation head: `4d2f25554e0dd217664f075cf5a58ec027a22ded`;
- baseline workflow run `35486995683`: SUCCESS;
- merge: `bd98d7c6bf727bc597d66972970b36ffbb773c69`;
- roadmap: `I8_CAPABILITY_ROADMAP.md`.

The roadmap defines TASK-KMP-081 through TASK-KMP-090 as independent increments and does not authorize them automatically.

## TASK-KMP-081 — DONE

TASK-KMP-081 was separately authorized by Luis, implemented through PR #25 and merged after explicit approval.

### Product result

Native iOS can import `.ips`, `.txt`, `.log` and `.json` Panic Full text files through SwiftUI `fileImporter` and send the resulting text through the same `DiagnosticViewModel -> NativeDiagnosticFacade -> shared deterministic engine` path used by typed/pasted input.

Safety boundaries:

- 5 MiB maximum;
- UTF-8 accepted directly;
- UTF-16 LE/BE accepted only with explicit BOM;
- arbitrary non-UTF-8 bytes without a valid BOM fail closed;
- empty, unsupported, oversized and unreadable inputs fail safely;
- cancellation preserves the existing input;
- import does not auto-analyze or auto-persist.

### Exact-head evidence

Implementation head: `dcae6855a9f290abc264a2978c9ff3bb3b7028ce`.

- `KMP I0 Baseline Verification` run `35489568505`: SUCCESS;
- `KMP I7 Native Equivalence` run `35489568540`: SUCCESS;
- `KMP I7 Native iOS Slice` run `35489568509`: SUCCESS;
- simulator build: SUCCESS;
- generic-device build without signing: SUCCESS;
- XCTest/XCUITest: SUCCESS;
- import-versus-paste deterministic equivalence: SUCCESS;
- `.xcresult` artifact `10599226050`, digest `sha256:17653c50580ee0855638b1711298f6ac7b692cac8e663b5672fa22d17e376f76`;
- native-equivalence artifact `10598792265`, digest `sha256:37a159af8dcc938def6c4581f08d2c8ee7116c5614102e00376cde436960801f`;
- implementation evidence: `I8_081_FILE_IMPORT.md`;
- PR #25 merge: `1fa8d73a7e4fdfe9c56ba8cc067ce8d596be801e`.

No Android behavior, Room/schema, deterministic engine semantics, canonical Rule Pack contents, camera/OCR acquisition, persistence/history, PDF/export, AI guidance or Blueprint content changed.

## Next authorization boundary

TASK-KMP-082 — iOS camera + OCR acquisition — is the next proposed capability.

**TASK-KMP-082 is NOT AUTHORIZED by this closeout.** It requires separate explicit authorization before implementation.
