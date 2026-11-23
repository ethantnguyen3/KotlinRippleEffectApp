package com.example.rippleeffect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Checks that every supported language has a complete translation, so a
 * missing key never shows English in the middle of a translated screen.
 * See docs/user-stories/02-show-app-in-device-language.md (T12).
 */
class StringResourcesTest {

    private val resDir: File = listOf(File("src/main/res"), File("app/src/main/res"))
        .first { it.isDirectory }

    private val formatArg = Regex("%(\\d+\\$)?[sdf]")

    /** name -> text for <string> and <plurals> (plurals keyed "name#quantity"). */
    private fun readStrings(folder: String): Map<String, String> {
        val file = File(resDir, "$folder/strings.xml")
        assertTrue("missing $file", file.isFile)
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val result = linkedMapOf<String, String>()
        val strings = doc.getElementsByTagName("string")
        for (i in 0 until strings.length) {
            val e = strings.item(i) as Element
            if (e.getAttribute("translatable") == "false") continue
            result[e.getAttribute("name")] = e.textContent
        }
        val plurals = doc.getElementsByTagName("plurals")
        for (i in 0 until plurals.length) {
            val p = plurals.item(i) as Element
            // Quantities differ per language (CLDR rules), so only "other" is compared by key.
            val items = p.getElementsByTagName("item")
            for (j in 0 until items.length) {
                val item = items.item(j) as Element
                if (item.getAttribute("quantity") == "other") {
                    result["${p.getAttribute("name")}#other"] = item.textContent
                }
            }
        }
        return result
    }

    private fun translationFolders(): List<String> =
        resDir.listFiles()!!
            .filter { it.isDirectory && Regex("values-[a-z]{2,3}(-r[A-Z]{2})?").matches(it.name) }
            .filter { File(it, "strings.xml").isFile }
            .map { it.name }
            .sorted()

    @Test
    fun supportedLanguagesArePresent() {
        assertEquals(listOf("values-es", "values-pt", "values-sw"), translationFolders())
    }

    @Test
    fun everyTranslationHasEveryKey() {
        val english = readStrings("values")
        for (folder in translationFolders()) {
            val translated = readStrings(folder)
            assertEquals("keys missing in $folder", emptySet<String>(), english.keys - translated.keys)
            assertEquals("extra keys in $folder", emptySet<String>(), translated.keys - english.keys)
        }
    }

    @Test
    fun translationsUseTheSameFormatArguments() {
        val english = readStrings("values")
        for (folder in translationFolders()) {
            val translated = readStrings(folder)
            for ((key, text) in english) {
                val expected = formatArg.findAll(text).map { it.value }.sorted().toList()
                val actual = formatArg.findAll(translated[key] ?: "").map { it.value }.sorted().toList()
                assertEquals("format arguments differ for '$key' in $folder", expected, actual)
            }
        }
    }

    @Test
    fun noTranslationIsBlank() {
        for (folder in listOf("values") + translationFolders()) {
            readStrings(folder).forEach { (key, text) ->
                assertTrue("'$key' is blank in $folder", text.isNotBlank())
            }
        }
    }

    @Test
    fun packagedLocalesMatchResourceFolders() {
        // localeFilters keeps library translations (e.g. French) out of the APK, so
        // Android cannot pick a language the app's own text does not have.
        val gradle = listOf(File("app/build.gradle.kts"), File("build.gradle.kts")).first { it.isFile }.readText()
        val filters = Regex("localeFilters\\s*\\+=\\s*listOf\\(([^)]*)\\)").find(gradle)
        assertTrue("app/build.gradle.kts must set androidResources.localeFilters", filters != null)
        val packaged = Regex("\"([a-zA-Z-]+)\"").findAll(filters!!.groupValues[1]).map { it.groupValues[1] }.sorted().toList()
        val expected = (listOf("en") + translationFolders().map { it.removePrefix("values-") }).sorted()
        assertEquals(expected, packaged)
    }

    @Test
    fun localeConfigMatchesResourceFolders() {
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(File(resDir, "xml/locales_config.xml"))
        val locales = doc.getElementsByTagName("locale")
        val declared = (0 until locales.length)
            .map { (locales.item(it) as Element).getAttributeNS("http://schemas.android.com/apk/res/android", "name") }
            .sorted()
        val expected = (listOf("en") + translationFolders().map { it.removePrefix("values-") }).sorted()
        assertEquals(expected, declared)
    }
}
