package io.legado.app.help.storage

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackupResourcesTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun rejectsTraversalAndSiblingPrefix() {
        val root = temp.newFolder("backup")
        listOf("../backup-sibling/file", "/absolute", "a/../../file", "a\\file", "file:x").forEach {
            assertThrows(IllegalArgumentException::class.java) { BackupResources.safeFile(root, it) }
        }
    }

    @Test fun resourceRewritingNeverChangesRuleTextOrNames() {
        val json = com.google.gson.JsonParser.parseString("""{
            "pattern":"backup-resource://resources/test", "name":"/a/name",
            "bgImage":"backup-resource://resources/test", "fontPath":"font",
            "lightImages":["image"], "sampleText":"font"
        }""")
        val result = BackupResources.transform(json) { "resolved:$it" }.asJsonObject
        assertEquals("backup-resource://resources/test", result.get("pattern").asString)
        assertEquals("/a/name", result.get("name").asString)
        assertEquals("font", result.get("sampleText").asString)
        assertEquals("resolved:font", result.get("fontPath").asString)
        assertEquals("resolved:image", result.getAsJsonArray("lightImages")[0].asString)
    }

    @Test fun extractionPreservesNestedResourceBytes() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use {
            it.putNextEntry(ZipEntry("resources/theme/font.ttf"))
            it.write("font-data".toByteArray())
            it.closeEntry()
        }
        val root = temp.newFolder()
        BackupResources.extract(output.toByteArray().inputStream(), root)
        assertEquals("font-data", File(root, "resources/theme/font.ttf").readText())
    }

    @Test fun verifiesConfigurationDigestBeforeRestore() {
        val root = temp.newFolder()
        File(root, "config.xml").writeText("<map />")
        File(root, BackupResources.MANIFEST).writeText("""{"version":1,"modules":["other"],"files":{}}""")
        BackupResources.finishManifest(root, listOf("config.xml"))
        assertEquals(setOf("other"), BackupResources.validate(root))
        File(root, "config.xml").appendText("damaged")
        assertThrows(IllegalArgumentException::class.java) { BackupResources.validate(root) }
    }

    @Test fun rejectsFileFromUnselectedModule() {
        val root = temp.newFolder()
        File(root, "config.xml").writeText("<map />")
        File(root, "bookshelf.json").writeText("[]")
        File(root, BackupResources.MANIFEST).writeText("""{"version":1,"modules":["rss"],"files":{}}""")
        BackupResources.finishManifest(root, listOf("config.xml", "bookshelf.json"))
        assertThrows(IllegalArgumentException::class.java) { BackupResources.validate(root) }
    }
}
