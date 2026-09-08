package io.legado.app.help.source

import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.BookInfoRule
import io.legado.app.data.entities.rule.ContentRule
import io.legado.app.data.entities.rule.ExploreRule
import io.legado.app.data.entities.rule.ReviewRule
import io.legado.app.data.entities.rule.SearchRule
import io.legado.app.data.entities.rule.TocRule
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookSourceConfigurationTest {
    private fun emptySource() = BookSource(
        bookSourceUrl = "https://example.com#empty",
        bookSourceName = "空配置",
        bookSourceGroup = "保留分组",
        bookSourceComment = "只有说明不是抓取配置",
    )

    @Test
    fun metadataAndEmptyRuleObjectsDoNotMakeAConfiguredSource() {
        val source = emptySource()
        assertTrue(source.isEmptyConfiguration())
        // 检查不得调用 getSearchRule 等会把 null 变成默认对象的 getter。
        assertNull(source.ruleSearch)
        assertNull(source.ruleContent)
        source.ruleSearch = SearchRule(checkKeyWord = " \t\n")
        source.ruleExplore = ExploreRule()
        source.ruleBookInfo = BookInfoRule(canReName = "true")
        source.ruleToc = TocRule()
        source.ruleContent = ContentRule(content = " \n", imageStyle = "FULL")
        source.ruleReview = ReviewRule()
        assertTrue(source.isEmptyConfiguration())
    }

    @Test
    fun sourceConfigurationFieldsAreIndividuallyProtected() {
        val configurationFields = setOf(
            "searchUrl", "exploreUrl", "mainJs", "jsLib", "loginUrl", "loginUi",
            "loginCheckJs", "bookUrlPattern", "coverDecodeJs", "header", "exploreScreen",
        )
        val metadataFields = setOf(
            "bookSourceUrl", "bookSourceName", "bookSourceGroup", "bookSourceType",
            "customOrder", "enabled", "enabledExplore", "enabledCookieJar", "concurrentRate",
            "bookSourceComment", "variableComment", "lastUpdateTime", "respondTime", "weight",
        )
        val ruleFields = setOf(
            "ruleSearch", "ruleExplore", "ruleBookInfo", "ruleToc", "ruleContent", "ruleReview",
        )
        // 新字段必须明确归类，防止导入拦截静默忽略新能力。
        assertEquals(
            configurationFields + metadataFields + ruleFields + setOf("eventListener", "customButton"),
            instanceFields(BookSource::class.java).map { it.name }.toSet(),
        )
        configurationFields.forEach { name ->
            val source = emptySource()
            val field = BookSource::class.java.getDeclaredField(name).apply { isAccessible = true }
            field.set(source, " \t\n")
            assertTrue(name, source.isEmptyConfiguration())
            field.set(source, "configured")
            assertFalse(name, source.isEmptyConfiguration())
        }
        assertFalse(emptySource().copy(eventListener = true).isEmptyConfiguration())
        assertFalse(emptySource().copy(customButton = true).isEmptyConfiguration())
    }

    @Test
    fun everyRuleFieldExceptDisplayOptionsPreventsEmptyClassification() {
        val rules = listOf(
            "ruleSearch" to SearchRule(), "ruleExplore" to ExploreRule(),
            "ruleBookInfo" to BookInfoRule(), "ruleToc" to TocRule(),
            "ruleContent" to ContentRule(), "ruleReview" to ReviewRule(),
        )
        val displayOnly = setOf("ruleBookInfo.canReName", "ruleContent.imageStyle")
        rules.forEach { (name, rule) ->
            instanceFields(rule.javaClass).forEach { field ->
                assertEquals("New rule field needs explicit classification: $name.${field.name}",
                    String::class.java, field.type)
                val source = emptySource()
                BookSource::class.java.getDeclaredField(name).apply { isAccessible = true }.set(source, rule)
                field.isAccessible = true
                field.set(rule, "configured")
                assertEquals("$name.${field.name}", "$name.${field.name}" in displayOnly,
                    source.isEmptyConfiguration())
                field.set(rule, null)
            }
        }
    }

    @Test
    fun discoveryOnlyAndSingleFileJsAreNotRejected() {
        assertFalse(emptySource().copy(exploreUrl = "分类::/list").isEmptyConfiguration())
        assertFalse(emptySource().copy(mainJs = "function search(key) { return []; }").isEmptyConfiguration())
        assertFalse(emptySource().copy(ruleBookInfo = BookInfoRule(downloadUrls = "a@href")).isEmptyConfiguration())
        assertFalse(emptySource().copy(ruleContent = ContentRule(callBackJs = "onEvent()")).isEmptyConfiguration())
    }

    private fun instanceFields(type: Class<*>) = type.declaredFields.filterNot {
        Modifier.isStatic(it.modifiers) || it.isSynthetic
    }
}
