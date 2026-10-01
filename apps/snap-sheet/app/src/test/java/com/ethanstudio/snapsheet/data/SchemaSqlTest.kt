package com.ethanstudio.snapsheet.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Nâng dữ liệu v1 → v2 không được làm mất tài liệu cũ. */
class SchemaSqlTest {
    private val addColumn = "ALTER TABLE `docs` ADD COLUMN `folderId` INTEGER"
    private val createFolders = "CREATE TABLE IF NOT EXISTS `folders`"

    @Test
    fun migrationHasExactlyTwoStatements() {
        assertEquals(2, MIGRATION_1_2_SQL.size)
    }

    @Test
    fun migrationCreatesFoldersTableWithAllColumns() {
        val create = MIGRATION_1_2_SQL.single { it.startsWith(createFolders) }
        assertTrue(create.contains("`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL"))
        assertTrue(create.contains("`name` TEXT NOT NULL"))
        assertTrue(create.contains("`createdAt` INTEGER NOT NULL"))
    }

    @Test
    fun migrationAddsNullableFolderColumnToDocs() {
        val alter = MIGRATION_1_2_SQL.single { it.startsWith(addColumn) }
        // Cột phải cho phép NULL: tài liệu cũ chưa thuộc thư mục nào (Doc.folderId: Long? = null).
        assertFalse(alter.uppercase().contains("NOT NULL"))
        assertFalse(alter.uppercase().contains("DEFAULT"))
    }

    @Test
    fun migrationNeverDropsOrDeletesData() {
        for (sql in MIGRATION_1_2_SQL) {
            val upper = sql.uppercase()
            assertFalse(sql, upper.contains("DROP"))
            assertFalse(sql, upper.contains("DELETE"))
            assertFalse(sql, upper.contains("RENAME"))
            assertFalse(sql, upper.contains("UPDATE"))
        }
    }
}
