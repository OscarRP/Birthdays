package com.oscarruiz.birthdates.data.local

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import com.oscarruiz.birthdates.domain.model.Birthday
import com.oscarruiz.birthdates.domain.model.Relation
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "birthdays")
data class BirthdayEntity(
    @PrimaryKey val id: String,
    val name: String,
    val day: Int,
    val month: Int,
    val year: Int?,
    val relation: String?,
    val notes: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

fun BirthdayEntity.toDomain() = Birthday(
    id = id,
    name = name,
    day = day,
    month = month,
    year = year,
    relation = relation?.let { value -> Relation.entries.firstOrNull { it.name == value } },
    notes = notes,
    updatedAt = updatedAt,
)

fun Birthday.toEntity() = BirthdayEntity(
    id = id,
    name = name,
    day = day,
    month = month,
    year = year,
    relation = relation?.name,
    notes = notes,
    updatedAt = updatedAt,
)

@Dao
abstract class BirthdayDao {
    @Query("SELECT * FROM birthdays ORDER BY name COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<BirthdayEntity>>

    @Query("SELECT * FROM birthdays")
    abstract suspend fun getAll(): List<BirthdayEntity>

    @Query("SELECT * FROM birthdays WHERE id = :id")
    abstract suspend fun getById(id: String): BirthdayEntity?

    @Upsert
    abstract suspend fun upsert(entity: BirthdayEntity)

    @Upsert
    abstract suspend fun upsertAll(entities: List<BirthdayEntity>)

    @Query("DELETE FROM birthdays WHERE id = :id")
    abstract suspend fun deleteById(id: String)

    @Query("DELETE FROM birthdays")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun replaceAll(entities: List<BirthdayEntity>) {
        deleteAll()
        upsertAll(entities)
    }
}

@Database(entities = [BirthdayEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun birthdayDao(): BirthdayDao

    companion object {
        const val NAME = "birthdays.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME).build()
    }
}
