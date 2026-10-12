package com.example.problemfinder.data.browse.impl.local

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.example.problemfinder.data.browse.impl.model.entity.BrowseClusterEntity
import com.example.problemfinder.data.browse.impl.model.entity.BrowseQueryEntity
import com.example.problemfinder.data.browse.impl.model.entity.BrowseQueryItemEntity

@Database(
    entities = [
        BrowseClusterEntity::class,
        BrowseQueryEntity::class,
        BrowseQueryItemEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class BrowseDatabase : RoomDatabase() {
    abstract fun browseDao(): BrowseDao
}
