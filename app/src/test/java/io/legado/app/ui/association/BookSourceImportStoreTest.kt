package io.legado.app.ui.association

import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.ContentRule
import io.legado.app.utils.GSON
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BookSourceImportStoreTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun preservesFullConfigurationAndSameUrlRowsWithoutKeepingRulesInPreview() {
        val first = BookSource(bookSourceUrl = "same", bookSourceName = "第一版",
            mainJs = "function search() {}", ruleContent = ContentRule(content = "@js:正文"))
        val second = first.copy(bookSourceName = "第二版", mainJs = "function search() { return []; }")
        BookSourceImportStore(folder.root).use { store ->
            val row = store.append(first)
            store.append(second)
            assertFalse(row.emptyConfiguration)
            assertFalse(row.javaClass.declaredFields.any { it.name == "mainJs" || it.name.startsWith("rule") })
            assertEquals(GSON.toJson(first), GSON.toJson(store.read(0)))
            assertEquals(GSON.toJson(second), GSON.toJson(store.read(1)))
            val empty = first.copy(mainJs = null, ruleContent = null)
            assertTrue(store.replace(0, empty).emptyConfiguration)
            assertEquals(GSON.toJson(empty), GSON.toJson(store.read(0)))
            assertEquals(GSON.toJson(second), GSON.toJson(store.read(1)))
        }
        assertTrue(folder.root.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun closingTwiceDeletesOnlyItsOwnTemporaryFile() {
        val unrelated = File(folder.root, "keep.txt").apply { writeText("keep") }
        val store = BookSourceImportStore(folder.root)
        store.append(BookSource(bookSourceUrl = "empty"))
        store.close()
        store.close()
        assertTrue(unrelated.isFile)
        assertEquals(listOf("keep.txt"), folder.root.listFiles().orEmpty().map { it.name })
    }
}
