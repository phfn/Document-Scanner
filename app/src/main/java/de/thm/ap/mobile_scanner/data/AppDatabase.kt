package de.thm.ap.mobile_scanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import de.thm.ap.mobile_scanner.data.*
import de.thm.ap.mobile_scanner.model.Document
import de.thm.ap.mobile_scanner.model.DocumentTagRelation
import de.thm.ap.mobile_scanner.model.Tag

@Database(
  entities = [Document::class, Tag::class, DocumentTagRelation::class],
  version = 1,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun documentDao(): DocumentDAO

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null
    fun getDb(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "document_database"
        ).build().also { INSTANCE = it }
      }
    }
  }
}

