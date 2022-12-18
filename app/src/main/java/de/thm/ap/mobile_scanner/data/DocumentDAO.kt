package de.thm.ap.mobile_scanner.data

import androidx.lifecycle.LiveData
import androidx.room.*
import de.thm.ap.mobile_scanner.model.Document
import de.thm.ap.mobile_scanner.model.DocumentTagRelation
import de.thm.ap.mobile_scanner.model.Tag

@Dao
interface DocumentDAO {

  @Query("SELECT * from document")
  fun findAllDocuments(): List<Document>

  @Query("SELECT * from document")
  fun findAllDocumentsSync(): LiveData<List<Document>>

  @Query("SELECT * from document WHERE documentId = :documentId")
  fun findDocumentById(documentId: Long): Document

  @Update(onConflict = OnConflictStrategy.REPLACE)
  suspend fun update(document: Document): Int

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun persist(document: Document): Long

  @Delete
  suspend fun delete(document: Document): Int

  @Query("SELECT * from tag")
  fun findAllTags(): List<Tag>

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun persist(tag: Tag): Long

  @Update(onConflict = OnConflictStrategy.REPLACE)
  suspend fun update(tag: Tag): Int

  @Delete
  suspend fun delete(tag: Tag): Int

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun persist(documentTagRelation: DocumentTagRelation)

  @Query("SELECT * from documentTagRelation")
  fun findAllDocumentTagRelation(): List<DocumentTagRelation>

  @Delete
  suspend fun delete(documentTagRelation: DocumentTagRelation)

  suspend fun persist(documentID: Long, tagID: Long) =
    persist(DocumentTagRelation(documentID, tagID))


  data class TagWithDocuments(
    @Embedded
    val tag: Tag,
    @Relation(
      parentColumn = "tagId",
      entityColumn = "documentId",
      associateBy = Junction(DocumentTagRelation::class)
    )
    val documents: List<Document>

  )

  @Transaction
  @Query("SELECT * FROM tag")
  suspend fun getTagsWithDocument(): List<TagWithDocuments>
}

