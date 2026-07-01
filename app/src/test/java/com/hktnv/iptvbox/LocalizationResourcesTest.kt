package com.hktnv.iptvbox

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizationResourcesTest {
    @Test
    fun defaultTurkishStringsAreProvidedFromResources() {
        val strings = readDefaultStrings()

        assertEquals("Kapat", strings["action_close"])
        assertEquals("Oynatma listesi silinsin mi?", strings["playlist_delete_title"])
        assertEquals("Oynatıcı arayüzü", strings["settings_player_ui_title"])
        assertEquals("Modern OSD", strings["settings_player_ui_value"])
    }

    private fun readDefaultStrings(): Map<String, String> {
        val file = listOf(
            File("src/main/res/values/strings.xml"),
            File("app/src/main/res/values/strings.xml"),
        ).first(File::isFile)
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("string")
        return buildMap {
            for (index in 0 until nodes.length) {
                val node = nodes.item(index)
                put(node.attributes.getNamedItem("name").nodeValue, node.textContent)
            }
        }
    }
}
