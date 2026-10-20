package com.example.problemfinder.core.model.details

import com.example.problemfinder.core.model.opportunity.Opportunity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.time.Instant

data class OpportunityDetails(
    val id: Int,
    val name: String,
    val summary: String,
    val problemStatement: String,
    val postCount: Int,
    val firstSeen: Instant?,
    val lastSeen: Instant?,
    val authorCount: Int,
    val communityCount: Int,
    val sourceBreakdown: List<EvidenceSourceCount>,
    val postVolumeByDate: List<EvidenceDateCount>,
) {
    init {
        require(id > 0) { "id must be greater than zero: $id" }

        require(name.isNotBlank()) { "name must contain non-whitespace text: $name" }
        require(summary.isNotBlank()) { "summary must contain non-whitespace text: $summary" }
        require(problemStatement.isNotBlank()) { "problemStatement must contain non-whitespace text: $problemStatement" }

        require(postCount >= 0) { "postCount must be nonnegative: $postCount" }
        require(authorCount >= 0) { "authorCount must be nonnegative: $authorCount" }
        require(communityCount >= 0) { "communityCount must be nonnegative: $communityCount" }
    }
}

data class EvidenceSourceCount(val source: String, val count: Int) {
    init {
        require(source.isNotBlank()) {
            "source must contain non-whitespace text: $source"
        }

        require(count >= 0) {
            "count must be nonnegative: $count"
        }
    }
}

data class EvidenceDateCount(val date: String, val count: Int) {
    init {
        require(date.isValidDate()) {
            "date must follow YYYY-MM-DD format: $date"
        }

        require(count >= 0) {
            "count must be nonnegative: $count"
        }
    }
}

data class OpportunityEvidence(
    val id: Int,
    val title: String,
    val body: String?,
    val url: String?,
    val author: String?,
    val community: String?,
    val source: String?,
    val score: Int?,
    val commentCount: Int?,
    val postedAt: Instant?,
    val problemStatement: String?,
) {
    init {
        require(id > 0) { "id must be greater than zero: $id" }
        require(title.isNotBlank()) { "title must contain non-whitespace text: $title" }
        if (commentCount != null) {
            require(commentCount >= 0) { "commentCount must be nonnegative: $commentCount" }
        }
    }
}

data class OpportunityDetailsPage(
    val details: OpportunityDetails,
    val topEvidence: List<OpportunityEvidence>,
    val posts: List<OpportunityEvidence>,
    val page: Int,
    val pageSize: Int,
    val totalPosts: Int,
) {
    init {
        require(page >= 1) { "page must be nonnegative: $page" }
        require(pageSize in 1..200) { "pageSize be in between (inclusive) 1-200: $pageSize" }
        require(totalPosts >= 0) { "totalPosts must be nonnegative: $totalPosts" }
    }
}

/**
 * TODO [DETAILS-2] The local source assembles this object from Room after DETAILS-1 is complete.
 *
 * It describes the selected account/cluster at one moment:
 * - details/topEvidence: cached parent content and highlighted evidence.
 * - posts: ordinary evidence accumulated from the contiguous pages successfully stored so far.
 * - lastLoadedPage/pageSize/totalPosts: saved progress used to decide whether another page exists.
 * - cacheReady=false: the account's database read has not finished yet.
 * - cacheReady=true, hasSynced=false: the read finished, but no successful server response was saved.
 * - hasSynced=true: a validated response was stored, even if a valid evidence collection is empty.
 *
 * cachedOpportunity is the existing shared card row. Its watch state changes when any feature uses
 * the common Add/Remove use case. LEFT JOIN it so absence does not hide otherwise valid detail text.
 * The current GET /clusters/{id} response supplies neither category nor isWatched. For a cold link,
 * no shared card means membership is UNKNOWN. Keep watch disabled until an authoritative card or
 * an extended backend contract supplies it; do not turn absence into isWatched=false.
 *
 * Finish check: database reads alone can reconstruct the screen and its paging progress after restart.
 */
data class OpportunityDetailsSnapshot(
    val userId: String? = null,
    val details: OpportunityDetails? = null,
    val cachedOpportunity: Opportunity? = null,
    val topEvidence: List<OpportunityEvidence> = emptyList(),
    val posts: List<OpportunityEvidence> = emptyList(),
    val lastLoadedPage: Int = 0,
    val pageSize: Int = 20,
    val totalPosts: Int = 0,
    val cacheReady: Boolean = false,
    val hasSynced: Boolean = false,
)

@Suppress("CheckResult")
fun String.isValidDate(): Boolean {
    return try {
        LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE);
        true
    } catch (error: DateTimeParseException) {
        print(error.message)
        false
    }
}
