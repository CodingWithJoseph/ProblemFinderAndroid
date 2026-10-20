package com.example.problemfinder.data.opportunities.impl.util

import com.example.problemfinder.core.model.details.OpportunityDetailsPage
import com.example.problemfinder.data.opportunities.impl.model.dto.OpportunityDetailsDto

internal fun OpportunityDetailsDto.toDetailsPage(): OpportunityDetailsPage {
    // TODO [DETAILS-1C] Implement DTO -> validated app model. Return OpportunityDetailsPage, not an entity.
    // Prerequisite: finish the model checks in DETAILS-1A and review the DTO in DETAILS-1B.
    //
    // Write this conversion in small pieces:
    // 1. Add a mapper for OpportunityEvidenceDto -> OpportunityEvidence. Preserve nullable fields,
    //    rename numComments -> commentCount, parse postedAt when supplied, and retain signed score.
    // 2. Map the aggregate lists: platform -> EvidenceSourceCount.source; calendar date/count ->
    //    EvidenceDateCount. Validate calendar dates separately from full event timestamps.
    // 3. For parent name/summary/problemStatement, obtain the non-null value, trim it and require
    //    non-blank text. A missing legacy value should fail mapping, not become "Untitled" or empty.
    // 4. Build OpportunityDetails using those cleaned values, counts, mapped aggregate lists and
    //    parsed firstSeen/lastSeenDate. Wire lastSeenDate maps to model.lastSeen.
    // 5. Map BOTH topEvidence and posts with the evidence mapper, preserving the order of each list.
    //    Do not merge these lists or deduplicate across their separate roles.
    // 6. Build OpportunityDetailsPage with the parent, both lists and page/pageSize/totalPosts.
    //    Validate paging metadata. The current server assigns postCount and totalPosts from the same
    //    count, so reject contradictory values rather than choosing one silently.
    //
    // Date policy: null stays null; a supplied malformed timestamp is invalid data, not a null/current
    // time fallback. Cluster ID matching the requested route belongs in the repository because this
    // mapper has no requested ID argument. This function checks the response's own positive identity.
    //
    // Ownership: this file only transforms/validates values. No HTTP, DAO calls, coroutine launches,
    // screen state updates or writes belong here. Map the complete page before the later transaction.
    //
    // DETAILS-1 completion check (write unit tests now): a valid fixture maps every renamed field;
    // required text null/blank fails; optional missing post body/URL succeeds; malformed supplied dates
    // fail; negative score succeeds; negative counts/invalid page metadata fail. No database is needed.
    // NEXT: DETAILS-2A entities. Add model -> entity and entity -> model functions here in that phase.
    throw UnsupportedOperationException("Complete DETAILS-1: detail/evidence mapping")
}
