package com.example

import com.example.ui.StringKeys
import com.example.ui.Translations
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Field

/**
 * Build-time test that every key defined in [StringKeys] has entries in both
 * the English and Thai translation maps. Missing keys fail the build before
 * they can reach the user as `??key??` fallbacks on screen.
 */
class TranslationKeyCoverageTest {

    @Test
    fun allStringKeysHaveEnglishAndThaiTranslations() {
        val enMissing = mutableListOf<String>()
        val thMissing = mutableListOf<String>()

        for (field in StringKeys::class.java.declaredFields) {
            if (field.type != String::class.java) continue
            val key = field.get(null) as String
            val enVal = Translations.getForTest(key, "en")
            val thVal = Translations.getForTest(key, "th")
            if (enVal == null || enVal.startsWith("??")) enMissing.add(key)
            if (thVal == null || thVal.startsWith("??")) thMissing.add(key)
        }

        assertTrue(
            "Missing English translations for ${enMissing.size} key(s): ${enMissing.joinToString(", ")}",
            enMissing.isEmpty()
        )
        assertTrue(
            "Missing Thai translations for ${thMissing.size} key(s): ${thMissing.joinToString(", ")}",
            thMissing.isEmpty()
        )
    }
}