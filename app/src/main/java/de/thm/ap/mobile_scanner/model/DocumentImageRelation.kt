package de.thm.ap.mobile_scanner.model

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    primaryKeys = ["documentId", "imageId"]
)
data class DocumentImageRelation(
    @ColumnInfo(index = true)
    var documentId: Long = 0,
    @ColumnInfo(index = true)
    var imageId: Long = 0
)