package com.example.problemfinder.data.opportunities.impl.db

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.example.problemfinder.data.opportunities.impl.db.converters.DateConverter
import com.example.problemfinder.data.opportunities.impl.db.converters.SectionConverter
import com.example.problemfinder.data.opportunities.impl.db.dao.BrowseDao
import com.example.problemfinder.data.opportunities.impl.db.dao.DiscoveryDao
import com.example.problemfinder.data.opportunities.impl.db.dao.OpportunityDao
import com.example.problemfinder.data.opportunities.impl.db.dao.OpportunityDetailsDao
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunityEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.BrowseQueryEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.BrowseQueryItemEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.DiscoveryCollectionItemEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.DiscoveryCollectionEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.DiscoveryDashboardEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.EvidenceDateCountEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.EvidenceSourceCountEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunityDetailsEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunityEvidenceEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunityEvidenceMembershipEntity

import com.example.problemfinder.data.opportunities.impl.db.dao.OpportunitySearchDao
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunitySearchQueryEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunitySearchQueryItemEntity

@Database(
    entities = [
        DiscoveryCollectionItemEntity::class,
        DiscoveryCollectionEntity::class,
        DiscoveryDashboardEntity::class,
        OpportunityEntity::class,
        BrowseQueryEntity::class,
        BrowseQueryItemEntity::class,
        OpportunityDetailsEntity::class,
        OpportunityEvidenceEntity::class,
        OpportunityEvidenceMembershipEntity::class,
        EvidenceSourceCountEntity::class,
        EvidenceDateCountEntity::class,
        OpportunitySearchQueryEntity::class,
        OpportunitySearchQueryItemEntity::class,
    ],
    version = 2,
    exportSchema = true
)
@ColumnTypeConverters(
    value = [
        DateConverter::class,
        SectionConverter::class
    ]
)
abstract class OpportunitiesDatabase : RoomDatabase() {
    abstract fun opportunityDao(): OpportunityDao
    abstract fun browseDao(): BrowseDao
    abstract fun discoveryDao(): DiscoveryDao

    abstract fun searchDao(): OpportunitySearchDao
    abstract fun discoveryDetailsDao(): OpportunityDetailsDao
}
