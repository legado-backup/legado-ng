package io.legado.app.ui.association

import android.app.Application
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.BookSourcePart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class ImportBookSourceSelectionTest {
    private fun model(): ImportBookSourceViewModel {
        val model = ImportBookSourceViewModel(RuntimeEnvironment.getApplication())
        val sources = listOf(
            BookSource(bookSourceUrl = "empty"),
            BookSource(bookSourceUrl = "search", searchUrl = "/search"),
            BookSource(bookSourceUrl = "discovery", exploreUrl = "分类::/list"),
        )
        sources.forEachIndexed { index, source ->
            model.allSources.add(source)
            model.checkSources.add(null)
            model.selectStatus.add(false)
            model.newSourceStatus.add(index != 2)
            model.updateSourceStatus.add(index == 2)
            model.updatePreviewSource(index, source)
        }
        return model
    }

    @Test
    fun singleAndAllSelectionExcludeEmptyAndOutOfRangeItems() {
        val model = model()
        assertEquals(emptySet<Int>(), model.applySelection(setOf(0)))
        assertEquals(setOf(1, 2), model.applySelection(setOf(-1, 0, 1, 2, 3)))
        assertTrue(model.isSelectAll)
        assertEquals(2, model.selectCount)
        assertEquals(listOf(1, 2), model.selectedImportIndices())
        assertEquals(emptySet<Int>(), model.applySelection(emptySet()))
        assertFalse(model.isSelectAll)
    }

    @Test
    fun newAndUpdateSelectionUseTheSameGuard() {
        val model = model()
        assertEquals(setOf(1), model.applySelection(model.newSourceStatus.indices
            .filter { model.newSourceStatus[it] }.toSet()))
        assertTrue(model.isSelectAllNew)
        assertEquals(setOf(2), model.applySelection(model.updateSourceStatus.indices
            .filter { model.updateSourceStatus[it] }.toSet()))
        assertTrue(model.isSelectAllUpdate)
    }

    @Test
    fun editingToEmptyImmediatelyDeselectsAndRepairDoesNotAutoSelect() {
        val model = model()
        model.applySelection(setOf(1))
        model.updatePreviewSource(1, BookSource(bookSourceUrl = "search"))
        assertFalse(model.selectStatus[1])
        assertFalse(1 in model.selectableIndices)
        assertEquals(emptyList<Int>(), model.selectedImportIndices())
        model.updatePreviewSource(1, BookSource(bookSourceUrl = "search", mainJs = "function search() {}"))
        assertTrue(1 in model.selectableIndices)
        assertFalse(model.selectStatus[1])
        assertEquals(setOf(1), model.applySelection(setOf(1)))
    }

    @Test
    fun submitRechecksPayloadEvenIfCachedSelectionWasBypassed() {
        val model = model()
        model.applySelection(setOf(1, 2))
        model.selectStatus[0] = true
        model.allSources[1] = BookSource(bookSourceUrl = "search", lastUpdateTime = 100L)
        model.checkSources[1] = BookSourcePart(bookSourceUrl = "search", hasSearchUrl = true)
        assertEquals(listOf(2), model.selectedImportIndices())
    }

    @Test
    fun allEmptyPreviewHasNothingSelectableOrImportable() {
        val model = model()
        model.allSources.indices.forEach { model.updatePreviewSource(it, BookSource(bookSourceUrl = "$it")) }
        assertEquals(emptySet<Int>(), model.applySelection(setOf(0, 1, 2)))
        assertEquals(0, model.selectCount)
        assertFalse(model.isSelectAll)
        assertEquals(emptyList<Int>(), model.selectedImportIndices())
    }
}
