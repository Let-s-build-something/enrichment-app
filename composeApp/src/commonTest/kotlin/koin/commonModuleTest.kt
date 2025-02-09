package koin

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class SecureAppSettingsTest {
    protected abstract val settings: SecureAppSettings

    @BeforeTest
    fun setup() {
        settings.clear() // Ensure clean state before each test
    }

    @AfterTest
    fun teardown() {
        settings.clear() // Clean up after each test
    }

    @Test
    fun testPutAndGetString() {
        settings.putString("testKey", "HelloWorld")
        assertEquals("HelloWorld", settings.getStringOrNull("testKey"))
    }

    @Test
    fun testRemoveKey() {
        settings.putString("toRemove", "Value")
        settings.remove("toRemove")
        assertNull(settings.getStringOrNull("toRemove"))
    }

    @Test
    fun testHasKey() {
        settings.putString("checkKey", "Value")
        assertTrue(settings.hasKey("checkKey"))
        assertFalse(settings.hasKey("missingKey"))
    }

    @Test
    fun testPutAndGetBoolean() {
        settings.putBoolean("boolKey", true)
        assertEquals(true, settings.getBoolean("boolKey", false))
    }

    @Test
    fun testPutAndGetInt() {
        settings.putInt("intKey", 42)
        assertEquals(42, settings.getInt("intKey", 0))
    }
}

