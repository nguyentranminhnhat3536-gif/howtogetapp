package com.ethanstudio.snapsheet.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderTest {
    // "Hóa đơn" viết bằng mã Unicode để chắc chắn là ký tự dựng sẵn.
    private val hoaDon = "Hóa đơn"
    private val hoaDonUpper = "HÓA ĐƠN"

    private val folders = listOf(
        Folder(id = 1, name = "Work", createdAt = 100),
        Folder(id = 2, name = hoaDon, createdAt = 200),
    )

    // --- Tên thư mục ---

    @Test
    fun blankNameIsEmpty() {
        assertEquals(FolderNameCheck.Empty, checkFolderName("", folders))
        assertEquals(FolderNameCheck.Empty, checkFolderName("   ", folders))
    }

    @Test
    fun newNameIsTrimmed() {
        assertEquals(FolderNameCheck.Ok("Travel"), checkFolderName("  Travel  ", folders))
    }

    @Test
    fun sameNameIgnoringCaseIsTaken() {
        assertEquals(FolderNameCheck.Taken, checkFolderName("work", folders))
        assertEquals(FolderNameCheck.Taken, checkFolderName("  WORK ", folders))
        assertEquals(FolderNameCheck.Taken, checkFolderName(hoaDonUpper, folders))
    }

    @Test
    fun renamingFolderToItsOwnNameIsAllowed() {
        assertEquals(FolderNameCheck.Ok("Work"), checkFolderName("Work", folders, editingId = 1))
        assertEquals(FolderNameCheck.Ok("WORK"), checkFolderName("WORK", folders, editingId = 1))
    }

    @Test
    fun renamingToAnotherFoldersNameIsTaken() {
        assertEquals(FolderNameCheck.Taken, checkFolderName("work", folders, editingId = 2))
    }

    @Test
    fun longNameIsCutToFortyChars() {
        val result = checkFolderName("a".repeat(60), emptyList())
        assertEquals(FolderNameCheck.Ok("a".repeat(FOLDER_NAME_MAX)), result)
        assertEquals(40, (result as FolderNameCheck.Ok).name.length)
    }

    @Test
    fun longNameIsComparedAfterCutting() {
        val existing = listOf(Folder(id = 7, name = "a".repeat(40), createdAt = 1))
        assertEquals(FolderNameCheck.Taken, checkFolderName("a".repeat(60), existing))
    }

    // --- Lọc tài liệu theo thư mục ---

    private val docs = listOf(
        Doc(1, "a", 1, 1, folderId = 2),
        Doc(2, "b", 2, 1),
        Doc(3, "c", 3, 1, folderId = 5),
        Doc(4, "d", 4, 2, folderId = 2),
    )

    @Test
    fun allFilterKeepsEveryDoc() {
        assertEquals(docs, filterByFolder(docs, null))
    }

    @Test
    fun folderFilterKeepsOnlyDocsInThatFolder() {
        assertEquals(listOf(1L, 4L), filterByFolder(docs, 2).map { it.id })
        assertEquals(listOf(3L), filterByFolder(docs, 5).map { it.id })
    }

    @Test
    fun unknownFolderGivesEmptyList() {
        assertTrue(filterByFolder(docs, 99).isEmpty())
    }

    @Test
    fun oldDocsHaveNoFolder() {
        // Tài liệu từ bản cũ (tạo không có folderId) nằm ngoài mọi thư mục.
        assertNull(Doc(1, "x", 3, 2).folderId)
    }
}
