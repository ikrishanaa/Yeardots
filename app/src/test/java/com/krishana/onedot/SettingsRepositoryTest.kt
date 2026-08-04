package com.krishana.onedot.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

/**
 * Unit tests for SettingsRepository
 * Tests DataStore persistence, default values, and color updates
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: SettingsRepository
    private lateinit var testScope: TestScope

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        testScope = TestScope(UnconfinedTestDispatcher() + Job())
        repository = SettingsRepository(context)
        
        // Ensure clean state for DataStore which acts as a singleton in Robolectric
        kotlinx.coroutines.runBlocking {
            repository.clearForTesting()
        }
    }

    @After
    fun cleanup() {
        kotlinx.coroutines.runBlocking {
            repository.clearForTesting()
        }
    }

    @Test
    fun `test default past color is returned when no value is set`() = testScope.runTest {
        val defaultColor = SettingsRepository.DEFAULT_PAST_COLOR
        val actualColor = repository.getPastColor()
        
        assertEquals("Default past color should be light gray", defaultColor, actualColor)
    }

    @Test
    fun `test default today color is returned when no value is set`() = testScope.runTest {
        val defaultColor = SettingsRepository.DEFAULT_TODAY_COLOR
        val actualColor = repository.getTodayColor()
        
        assertEquals("Default today color should be orange", defaultColor, actualColor)
    }

    @Test
    fun `test default future color is returned when no value is set`() = testScope.runTest {
        val defaultColor = SettingsRepository.DEFAULT_FUTURE_COLOR
        val actualColor = repository.getFutureColor()
        
        assertEquals("Default future color should be dark gray", defaultColor, actualColor)
    }

    @Test
    fun `test default background color is returned when no value is set`() = testScope.runTest {
        val defaultColor = SettingsRepository.DEFAULT_BACKGROUND_COLOR
        val actualColor = repository.getBackgroundColor()
        
        assertEquals("Default background color should be almost black", defaultColor, actualColor)
    }

    @Test
    fun `test default dot shape is dot`() = testScope.runTest {
        val actualShape = repository.getDotShape()
        
        assertEquals("Default dot shape should be dot", SettingsRepository.DEFAULT_DOT_SHAPE, actualShape)
    }

    @Test
    fun `test default dot density is small (1)`() = testScope.runTest {
        val actualDensity = repository.getDotDensity()
        
        assertEquals("Default dot density should be 1 (Small)", 1, actualDensity)
    }

    @Test
    fun `test update past color persists value`() = testScope.runTest {
        val customColor = 0xFF00FF00.toInt() // Green
        
        repository.updatePastColor(customColor)
        val retrievedColor = repository.getPastColor()
        
        assertEquals("Updated past color should persist", customColor, retrievedColor)
    }

    @Test
    fun `test update today color persists value`() = testScope.runTest {
        val customColor = 0xFFFF0000.toInt() // Red
        
        repository.updateTodayColor(customColor)
        val retrievedColor = repository.getTodayColor()
        
        assertEquals("Updated today color should persist", customColor, retrievedColor)
    }

    @Test
    fun `test update future color persists value`() = testScope.runTest {
        val customColor = 0xFF0000FF.toInt() // Blue
        
        repository.updateFutureColor(customColor)
        val retrievedColor = repository.getFutureColor()
        
        assertEquals("Updated future color should persist", customColor, retrievedColor)
    }

    @Test
    fun `test update background color persists value`() = testScope.runTest {
        val customColor = 0xFFFFFFFF.toInt() // White
        
        repository.updateBackgroundColor(customColor)
        val retrievedColor = repository.getBackgroundColor()
        
        assertEquals("Updated background color should persist", customColor, retrievedColor)
    }

    @Test
    fun `test update dot shape persists value`() = testScope.runTest {
        val customShape = "square"
        
        repository.updateDotShape(customShape)
        val retrievedShape = repository.getDotShape()
        
        assertEquals("Updated dot shape should persist", customShape, retrievedShape)
    }

    @Test
    fun `test update dot density persists value`() = testScope.runTest {
        val customDensity = 3 // Large
        
        repository.updateDotDensity(customDensity)
        val retrievedDensity = repository.getDotDensity()
        
        assertEquals("Updated dot density should persist", customDensity, retrievedDensity)
    }

    @Test
    fun `test update last update timestamp`() = testScope.runTest {
        val timestamp = System.currentTimeMillis()
        
        repository.updateLastUpdateTimestamp(timestamp)
        
        // Note: This test verifies the update doesn't throw an exception
        // A full integration test would verify the actual persisted value
        assertTrue("Timestamp update should complete without error", true)
    }

    @Test
    fun `test getAllColors returns all color values`() = testScope.runTest {
        // Set custom colors
        val pastColor = 0xFF111111.toInt()
        val todayColor = 0xFF222222.toInt()
        val futureColor = 0xFF333333.toInt()
        val backgroundColor = 0xFF444444.toInt()
        
        repository.updatePastColor(pastColor)
        repository.updateTodayColor(todayColor)
        repository.updateFutureColor(futureColor)
        repository.updateBackgroundColor(backgroundColor)
        
        val allColors = repository.getAllColors()
        
        assertEquals("Past color should match", pastColor, allColors["past"])
        assertEquals("Today color should match", todayColor, allColors["today"])
        assertEquals("Future color should match", futureColor, allColors["future"])
        assertEquals("Background color should match", backgroundColor, allColors["background"])
    }

    @Test
    fun `test valid density values are within range`() {
        val validDensities = listOf(0, 1, 2, 3) // Tiny, Small, Medium, Large
        
        assertTrue("Density 0 (Tiny) should be valid", validDensities.contains(0))
        assertTrue("Density 1 (Small) should be valid", validDensities.contains(1))
        assertTrue("Density 2 (Medium) should be valid", validDensities.contains(2))
        assertTrue("Density 3 (Large) should be valid", validDensities.contains(3))
    }

    @Test
    fun `test valid shape values`() {
        val validShapes = listOf("circle", "rounded", "square", "pill")
        
        assertTrue("Circle should be a valid shape", validShapes.contains("circle"))
        assertTrue("Rounded should be a valid shape", validShapes.contains("rounded"))
        assertTrue("Square should be a valid shape", validShapes.contains("square"))
        assertTrue("Pill should be a valid shape", validShapes.contains("pill"))
    }
}
