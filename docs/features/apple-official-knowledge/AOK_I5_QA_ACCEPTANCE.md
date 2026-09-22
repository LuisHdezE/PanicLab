# AOK-I5 — QA / Acceptance Final

## Objetivo

Cerrar Apple Official Knowledge v1 con un gate ejecutable que demuestre que el dataset gobernado, las consultas, las superficies Android/iOS y el firewall frente al diagnóstico determinista siguen siendo coherentes después de AOK-I1..I4.

AOK-I5 no cambia contenido Apple, reglas diagnósticas ni comportamiento del producto. Es un incremento de QA, arquitectura y evidencia.

## Baseline

- base: `main@1f886c273868394fdceb1601059549727c0bbf53`
- dataset: `APPLE_OFFICIAL_KNOWLEDGE_V1`
- versión: `1.0.0`
- alcance congelado real: `iPhone 5` → `current audited generations`
- familias: `19`
- modelos exactos: `48`
- fuentes: `91`
- fichas: `241`
- estado de ficha: `241/241 PILOT_READY`
- nivel: `241/241 DETAILED`
- Rule Pack effect: `241/241 NONE`

> Nota histórica: el texto inicial de Issue #30 mencionaba cobertura desde iPhone 6. La recopilación gobernada se amplió posteriormente hasta iPhone 5 y ese lower bound quedó congelado en el manifest de AOK-I2. I5 valida el alcance congelado real sin reescribir el historial del issue.

## Matriz de aceptación

| Gate | Evidencia ejecutable | Criterio |
| --- | --- | --- |
| Dataset congelado | `AppleOfficialKnowledgeAcceptanceTest` | manifest 1.0.0, 19/91/241, iPhone 5 lower bound, 48 modelos exactos |
| Autoridad oficial | validators + acceptance | toda fuente primaria/catalogada usa HTTPS en host Apple `apple.com` |
| Estado de ficha | card validator + acceptance | `PILOT_READY`, `DETAILED`, `rulePackEffect=NONE` |
| Resolución de fuentes | source-reference audit + acceptance | 776/117/68/4/45/9/36 y jamás inventar AOK IDs |
| Herramientas por modelo | acceptance | SSR y Recovery Diagnostics permanecen capacidades independientes |
| Guardrail iPhone 12/13/14 | acceptance | 12/13: SSR sí, Recovery Diagnostics no; 14: ambos soportados |
| Históricos | acceptance | programas históricos/ended/contextual nunca se representan como current puro |
| No Sound 12/12 Pro | acceptance + runtime | `AOKF-12-013` solo 12/12 Pro, nunca mini/Pro Max |
| Reemplazos genéricos | acceptance | iPhone 12 no hereda back-glass/coil ni housing de generaciones posteriores |
| Aplicabilidad exacta | runtime tests | XR/XS no hereda X; Multi-Touch 6 Plus no se extiende a iPhone 6 |
| Android composition | `AndroidAppleOfficialKnowledgeRuntimeTest` | runtime embebido 19/48/241 y NONE |
| iOS bridge | `DiagnosticBridgeTests` | Swift consume catálogo embebido y conserva resolución de fuentes |
| iOS presentation | `PanicLabIOSUITests` | Apple Oficial es superficie separada y no expone acción Analizar |
| Firewall estructural | `scripts/verify-aok-firewall.sh` | no existen imports de AOK hacia core determinista ni del core determinista hacia AOK |
| Regression KMP | workflows I0/I1/I7 | diagnóstico, Rule Pack, Kover, Android host, frameworks y native equivalence permanecen verdes |

## Gate dedicado

`.github/workflows/aok-i5-acceptance.yml` ejecuta en Linux:

1. validación sintáctica de todos los JSON gobernados AOK;
2. `scripts/verify-aok-firewall.sh`;
3. suite JVM compartida, incluyendo `AppleOfficialKnowledgeAcceptanceTest`;
4. composición Android AOK con Robolectric;
5. publicación de evidencia de tests como artifact.

Los workflows KMP existentes continúan siendo la evidencia cross-platform para iOS y regresión determinista. I5 no duplica esos runners macOS dentro del workflow AOK dedicado.

## Firewall

La frontera sigue siendo estricta:

- `shared/.../diagnostic`, `parser` y `rulepack` no importan `com.example.appleknowledge`;
- `DiagnosticRepositoryImpl` y `ui/analysis` Android no importan AOK;
- producción AOK no importa `com.example.diagnostic`, `com.example.rulepack` ni `com.example.parser`;
- la superficie AOK nativa no llama `NativeDiagnosticFacade`.

Esto complementa el contrato de dominio `AppleKnowledgeRulePackEffect.NONE`; no lo reemplaza.

## Criterio de cierre

AOK-I5 puede marcarse `COMPLETE` únicamente si, sobre el mismo HEAD de PR:

- el workflow `AOK I5 Acceptance` termina `SUCCESS`;
- los cinco workflows KMP aplicables terminan `SUCCESS`;
- PR sigue mergeable y sin cambios posteriores al HEAD validado;
- no se modifica el dataset gobernado durante la fase de cierre.

El merge y el cierre de Issue #30 son gates separados y requieren la aprobación gobernada correspondiente.
