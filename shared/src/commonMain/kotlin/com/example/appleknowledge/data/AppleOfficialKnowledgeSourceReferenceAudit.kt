package com.example.appleknowledge.data

enum class AppleSourceReferenceRole {
    PRIMARY,
    SECONDARY
}

data class AppleSourceReferenceOccurrence(
    val cardId: String,
    val role: AppleSourceReferenceRole,
    val url: String
)

data class AppleSourceReferenceAuditEntry(
    val url: String,
    val occurrenceCount: Int,
    val exactSourceIds: List<String>,
    val localePathCandidateIds: List<String>
) {
    val hasExactMatch: Boolean get() = exactSourceIds.isNotEmpty()
    val isExactAmbiguous: Boolean get() = exactSourceIds.size > 1
    val isUnmapped: Boolean get() = exactSourceIds.isEmpty()
}

data class AppleSourceReferenceAuditSummary(
    val totalOccurrences: Int,
    val uniqueUrls: Int,
    val exactSingleUniqueUrls: Int,
    val exactAmbiguousUniqueUrls: Int,
    val unmatchedUniqueUrls: Int,
    val localePathCandidateUniqueUrls: Int,
    val noCandidateUniqueUrls: Int,
    val exactSingleOccurrences: Int,
    val exactAmbiguousOccurrences: Int,
    val unmatchedOccurrences: Int
)

data class AppleSourceReferenceAuditResult(
    val summary: AppleSourceReferenceAuditSummary,
    val entries: List<AppleSourceReferenceAuditEntry>
)

object AppleOfficialKnowledgeSourceReferenceAudit {
    private val localeSegmentRegex = Regex(
        "^[a-z]{2}(?:-[a-z]{2,4})?$",
        RegexOption.IGNORE_CASE
    )

    fun audit(
        sources: List<AppleKnowledgeSourceSeedRecord>,
        cards: List<AppleKnowledgeCardSeedRecord>
    ): AppleSourceReferenceAuditResult {
        val exactIdsByUrl = sources
            .groupBy { it.source.officialUrl }
            .mapValues { (_, records) -> records.map { it.source.id }.distinct() }

        val candidateIdsBySupportPath = sources
            .mapNotNull { record ->
                supportPathKey(record.source.officialUrl)?.let { key -> key to record.source.id }
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
            .mapValues { (_, ids) -> ids.distinct() }

        val occurrences = cards.flatMap { card ->
            buildList {
                add(
                    AppleSourceReferenceOccurrence(
                        cardId = card.id,
                        role = AppleSourceReferenceRole.PRIMARY,
                        url = card.primarySourceUrl
                    )
                )
                card.secondarySourceRaw
                    .split(" | ")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .forEach { url ->
                        add(
                            AppleSourceReferenceOccurrence(
                                cardId = card.id,
                                role = AppleSourceReferenceRole.SECONDARY,
                                url = url
                            )
                        )
                    }
            }
        }

        val entries = occurrences
            .groupBy { it.url }
            .map { (url, urlOccurrences) ->
                val exactIds = exactIdsByUrl[url].orEmpty()
                val candidateIds = if (exactIds.isEmpty()) {
                    supportPathKey(url)
                        ?.let { candidateIdsBySupportPath[it] }
                        .orEmpty()
                } else {
                    emptyList()
                }
                AppleSourceReferenceAuditEntry(
                    url = url,
                    occurrenceCount = urlOccurrences.size,
                    exactSourceIds = exactIds.sorted(),
                    localePathCandidateIds = candidateIds.sorted()
                )
            }
            .sortedBy { it.url }

        val exactSingle = entries.filter { it.exactSourceIds.size == 1 }
        val exactAmbiguous = entries.filter { it.exactSourceIds.size > 1 }
        val unmatched = entries.filter { it.exactSourceIds.isEmpty() }
        val localeCandidates = unmatched.filter { it.localePathCandidateIds.isNotEmpty() }

        return AppleSourceReferenceAuditResult(
            summary = AppleSourceReferenceAuditSummary(
                totalOccurrences = occurrences.size,
                uniqueUrls = entries.size,
                exactSingleUniqueUrls = exactSingle.size,
                exactAmbiguousUniqueUrls = exactAmbiguous.size,
                unmatchedUniqueUrls = unmatched.size,
                localePathCandidateUniqueUrls = localeCandidates.size,
                noCandidateUniqueUrls = unmatched.size - localeCandidates.size,
                exactSingleOccurrences = exactSingle.sumOf { it.occurrenceCount },
                exactAmbiguousOccurrences = exactAmbiguous.sumOf { it.occurrenceCount },
                unmatchedOccurrences = unmatched.sumOf { it.occurrenceCount }
            ),
            entries = entries
        )
    }

    private fun supportPathKey(url: String): String? {
        val prefix = "https://support.apple.com/"
        if (!url.startsWith(prefix, ignoreCase = true)) return null

        val rawSegments = url
            .substring(prefix.length)
            .substringBefore('?')
            .substringBefore('#')
            .trim('/')
            .split('/')
            .filter { it.isNotBlank() }

        if (rawSegments.isEmpty()) return ""

        val normalizedSegments = if (localeSegmentRegex.matches(rawSegments.first())) {
            rawSegments.drop(1)
        } else {
            rawSegments
        }

        return normalizedSegments.joinToString("/").lowercase()
    }
}
