#!/usr/bin/env python3
from pathlib import Path
import sys

CAPABILITY_NAME = "apple_official_knowledge_capabilities_v1.json"
SOURCE_NAMES = [
    "apple_official_knowledge_sources_v1_part1.json",
    "apple_official_knowledge_sources_v1_part2.json",
    "apple_official_knowledge_sources_v1_part3.json",
]
CARD_NAMES = [
    "apple_official_knowledge_cards_v1_part1.json",
    "apple_official_knowledge_cards_v1_part2a.json",
    "apple_official_knowledge_cards_v1_part2b.json",
    "apple_official_knowledge_cards_v1_part2c.json",
    "apple_official_knowledge_cards_v1_part2d.json",
    "apple_official_knowledge_cards_v1_part3a.json",
    "apple_official_knowledge_cards_v1_part3b.json",
    "apple_official_knowledge_cards_v1_part3c.json",
    "apple_official_knowledge_cards_v1_part3d.json",
    "apple_official_knowledge_cards_v1_part4a.json",
    "apple_official_knowledge_cards_v1_part4b.json",
    "apple_official_knowledge_cards_v1_part4c.json",
    "apple_official_knowledge_cards_v1_part4d.json",
    "apple_official_knowledge_cards_v1_part5a.json",
    "apple_official_knowledge_cards_v1_part5b.json",
    "apple_official_knowledge_cards_v1_part5c.json",
    "apple_official_knowledge_cards_v1_part5d.json",
]


def raw_literal(resource_dir: Path, file_name: str) -> str:
    text = (resource_dir / file_name).read_text(encoding="utf-8")
    if '"""' in text:
        raise ValueError(f"{file_name} contains an unsupported triple quote")
    if "$" in text:
        raise ValueError(f"{file_name} contains an unsupported dollar sign")
    return f'"""{text}"""'


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: generate-apple-official-knowledge-embedded.py <resource-dir> <output-file>", file=sys.stderr)
        return 2

    resource_dir = Path(sys.argv[1]).resolve()
    output_file = Path(sys.argv[2]).resolve()

    required = [CAPABILITY_NAME, *SOURCE_NAMES, *CARD_NAMES]
    missing = [name for name in required if not (resource_dir / name).is_file()]
    if missing:
        raise FileNotFoundError(f"missing AOK resources: {', '.join(missing)}")

    lines = [
        "package com.example.appleknowledge.runtime",
        "",
        "internal object AppleOfficialKnowledgeEmbeddedResources {",
        "    fun bundle(): AppleOfficialKnowledgeResourceBundle = AppleOfficialKnowledgeResourceBundle(",
        f"        capabilitiesJson = {raw_literal(resource_dir, CAPABILITY_NAME)},",
        "        sourceJsonParts = listOf(",
    ]

    for index, name in enumerate(SOURCE_NAMES):
        suffix = "" if index == len(SOURCE_NAMES) - 1 else ","
        lines.append(f"            {raw_literal(resource_dir, name)}{suffix}")

    lines.extend([
        "        ),",
        "        cardJsonParts = listOf(",
    ])

    for index, name in enumerate(CARD_NAMES):
        suffix = "" if index == len(CARD_NAMES) - 1 else ","
        lines.append(f"            {raw_literal(resource_dir, name)}{suffix}")

    lines.extend([
        "        )",
        "    )",
        "}",
        "",
    ])

    output_file.parent.mkdir(parents=True, exist_ok=True)
    output_file.write_text("\n".join(lines), encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
