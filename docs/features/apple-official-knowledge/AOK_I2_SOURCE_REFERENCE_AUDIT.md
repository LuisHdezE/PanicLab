# AOK-I2 Source Reference Audit

## Scope

Read-only audit of governed `Apple Oficial` and `Apple Fichas` source references for AOK-I2.
No source identity is inferred from model knowledge. Exact URL equality is the only automatic mapping rule.

## Frozen counts

- `Apple Oficial`: 91 source rows, 82 unique official URLs.
- `Apple Fichas`: 241 cards.
- Card source references: 776 URL occurrences, 117 unique URLs.
- Unique URLs with exactly one exact catalog match: 68.
- Unique URLs with exact but ambiguous catalog match: 4.
- Unique URLs with no exact catalog match: 45.
- Of the 45 unmatched URLs, 9 have a locale/path-equivalent catalog candidate; these remain candidates only, not identity.
- 36 unmatched URLs have no locale/path-equivalent source row in the 91-source catalog.

## Normalization policy

1. Exact URL + exactly one `AOK-*` row: deterministic mapping is allowed.
2. Exact URL + multiple `AOK-*` rows: preserve raw URL and candidate IDs; do not auto-select an ID.
3. No exact URL match, but same Apple support path under another locale: preserve raw URL and record candidate IDs only.
4. No catalog match: preserve the Apple URL as an unmapped governed reference.
5. Never create a synthetic `AOK-*` ID to satisfy the normalized contract.

## Exact URL ambiguities

| URL | Occurrences in cards | Catalog IDs |
| --- | ---: | --- |
| `https://support.apple.com/en-us/101944` | 37 | `AOK-024`, `AOK-025` |
| `https://support.apple.com/es-es/102658` | 22 | `AOK-023`, `AOK-088` |
| `https://support.apple.com/es-es/108044` | 31 | `AOK-069`, `AOK-072`, `AOK-078`, `AOK-081`, `AOK-085`, `AOK-093` |
| `https://support.apple.com/es-es/120579` | 21 | `AOK-026`, `AOK-051`, `AOK-089` |

## Unmatched URLs

| Occurrences | URL | Locale/path candidate IDs |
| ---: | --- | --- |
| 14 | `https://support.apple.com/es-es/120652` | `AOK-011` |
| 12 | `https://support.apple.com/es-es/100464` | `AOK-007` |
| 10 | `https://support.apple.com/es-es/116940` | NONE |
| 10 | `https://support.apple.com/es-es/120555` | `AOK-057` |
| 7 | `https://support.apple.com/en-us/121720` | NONE |
| 7 | `https://support.apple.com/es-es/101970` | NONE |
| 6 | `https://support.apple.com/en-ie/116940` | NONE |
| 6 | `https://support.apple.com/es-es/101969` | NONE |
| 5 | `https://support.apple.com/es-es/126470` | NONE |
| 4 | `https://support.apple.com/es-es/101966` | NONE |
| 4 | `https://support.apple.com/es-es/125089` | NONE |
| 4 | `https://support.apple.com/es-es/125090` | NONE |
| 4 | `https://support.apple.com/es-es/125091` | NONE |
| 4 | `https://support.apple.com/es-es/125092` | NONE |
| 3 | `https://support.apple.com/en-ae/102327` | NONE |
| 3 | `https://support.apple.com/en-gb/102474` | NONE |
| 3 | `https://support.apple.com/en-gb/120803` | NONE |
| 3 | `https://support.apple.com/en-ie/120819` | NONE |
| 2 | `https://support.apple.com/en-ie/122208` | NONE |
| 2 | `https://support.apple.com/en-us/120662` | NONE |
| 2 | `https://support.apple.com/es-es/101967` | NONE |
| 2 | `https://support.apple.com/es-es/103256` | NONE |
| 2 | `https://support.apple.com/es-es/108039` | NONE |
| 2 | `https://support.apple.com/es-es/118106` | `AOK-022` |
| 2 | `https://support.apple.com/es-es/guide/deployment/dep36c581d6x/web` | NONE |
| 2 | `https://support.apple.com/es-us/guide/iphone/iph8903c3ee6/27/ios/27` | NONE |
| 1 | `https://support.apple.com/en-gb/108044` | `AOK-069`, `AOK-072`, `AOK-078`, `AOK-081`, `AOK-085`, `AOK-093` |
| 1 | `https://support.apple.com/en-ie/100464` | `AOK-007` |
| 1 | `https://support.apple.com/en-us/118106` | `AOK-022` |
| 1 | `https://support.apple.com/es-es/101968` | NONE |
| 1 | `https://support.apple.com/es-es/102327` | NONE |
| 1 | `https://support.apple.com/es-es/102854` | NONE |
| 1 | `https://support.apple.com/es-es/105103` | NONE |
| 1 | `https://support.apple.com/es-es/106348` | NONE |
| 1 | `https://support.apple.com/es-es/118259` | NONE |
| 1 | `https://support.apple.com/es-es/118427` | NONE |
| 1 | `https://support.apple.com/es-es/docs/iphone/134711` | `AOK-001` |
| 1 | `https://support.apple.com/es-es/docs/iphone/500016` | NONE |
| 1 | `https://support.apple.com/es-es/service-programs/status` | NONE |
| 1 | `https://support.apple.com/es-la/120579` | `AOK-026`, `AOK-058`, `AOK-051`, `AOK-089` |
| 1 | `https://support.apple.com/es-la/123114` | NONE |
| 1 | `https://support.apple.com/es-lamr/102474` | NONE |
| 1 | `https://support.apple.com/es-us/guide/iphone/iph4b302997c/ios` | NONE |
| 1 | `https://support.apple.com/guide/iphone/iphone-se-2nd-generation-iph4b7cbc094/27/ios/27` | NONE |
| 1 | `https://support.apple.com/service-programs` | NONE |

## Guardrail examples

- `https://support.apple.com/es-es/101969` appears in 6 card references and has no source row in the 91-source catalog. It remains an unmapped Apple-controlled URL.
- Locale variants such as `.../es-es/100464` vs catalog `.../en-gb/100464` are not treated as the same source automatically.
- `https://support.apple.com/es-es/108044` maps exactly to six different governed `AOK-*` rows, so URL equality alone cannot choose one semantic source ID.

## I2 consequence

AOK-I2 can safely version the raw governed dataset now. Conversion to the I1 normalized `AppleKnowledgeCard` contract must use an explicit mapping layer that preserves raw URLs and exposes unresolved/ambiguous references instead of fabricating source IDs.
