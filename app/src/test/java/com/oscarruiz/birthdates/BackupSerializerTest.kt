package com.oscarruiz.birthdates

import com.oscarruiz.birthdates.backup.BackupSerializer
import com.oscarruiz.birthdates.backup.InvalidBackupException
import com.oscarruiz.birthdates.domain.model.Birthday
import com.oscarruiz.birthdates.domain.model.Relation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializerTest {

    @Test
    fun roundTrip_keepsAllFields() {
        val original = listOf(
            Birthday(id = "a", name = "María López", day = 21, month = 9, year = 1992, relation = Relation.FRIEND, notes = "Libros", updatedAt = 10),
            Birthday(id = "b", name = "Pedro", day = 29, month = 2, updatedAt = 20),
        )
        val decoded = BackupSerializer.decode(BackupSerializer.encode(original, now = 0))
        assertEquals(original, decoded)
    }

    @Test
    fun invalidEntries_areDropped() {
        val json = """
            {"schemaVersion":1,"exportedAt":0,"birthdays":[
              {"id":"ok","name":"Ana","day":1,"month":1},
              {"id":"bad-date","name":"X","day":31,"month":4},
              {"id":"no-name","name":"  ","day":1,"month":1}
            ]}
        """.trimIndent()
        assertEquals(listOf("ok"), BackupSerializer.decode(json).map { it.id })
    }

    @Test
    fun unknownFields_areIgnored() {
        val json = """{"schemaVersion":1,"exportedAt":0,"extra":true,"birthdays":[{"id":"x","name":"Ana","day":1,"month":1,"color":"red"}]}"""
        assertEquals(1, BackupSerializer.decode(json).size)
    }

    @Test(expected = InvalidBackupException::class)
    fun garbage_throws() {
        BackupSerializer.decode("not json")
    }

    @Test(expected = InvalidBackupException::class)
    fun newerSchema_throws() {
        BackupSerializer.decode("""{"schemaVersion":99,"exportedAt":0,"birthdays":[]}""")
    }

    @Test
    fun missingId_getsGenerated() {
        val json = """{"schemaVersion":1,"exportedAt":0,"birthdays":[{"name":"Ana","day":1,"month":1}]}"""
        assertTrue(BackupSerializer.decode(json).single().id.isNotBlank())
    }
}
