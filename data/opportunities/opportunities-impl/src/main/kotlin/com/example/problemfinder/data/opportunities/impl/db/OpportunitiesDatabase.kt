package com.example.problemfinder.data.opportunities.impl.db

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunityEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.BrowseQueryEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.BrowseQueryItemEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.DiscoveryCollectionItemEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.DiscoveryCollectionEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.DiscoveryDashboardEntity

/**
 * One Room database for shared cards and the separate Browse/Discovery arrangements that reference
 * them. This lets each local data source update content, membership, and metadata in one transaction.
 */
@Database(
    entities = [
        DiscoveryCollectionItemEntity::class,
        DiscoveryCollectionEntity::class,
        DiscoveryDashboardEntity::class,
        OpportunityEntity::class,
        BrowseQueryEntity::class,
        BrowseQueryItemEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class OpportunitiesDatabase : RoomDatabase() {
    abstract fun opportunityDao(): OpportunityDao
    abstract fun browseDao(): BrowseDao
    abstract fun discoveryDao(): DiscoveryDao
}
