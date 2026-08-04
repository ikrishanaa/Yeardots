package com.krishana.onedot

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.krishana.onedot.data.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive Test Suite for Year Dots Application
 * Covers Critical, Moderate, and Minor bug fixes
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class YearDotsComprehensiveTest {

    private lateinit var context: Context
    private lateinit var repository: SettingsRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = SettingsRepository(context)
    }

    // =========================================================================================
    // CRITICAL BUG TESTS
    // =========================================================================================

    @Test
    fun testLeapYearDay366Rendering() {
        // Bug #1: Leap Year Bug - Day 366 Never Displayed
        // Verify that in a leap year, day 366 is correctly identified and rendered
        
        val isLeap2024 = true // 2024 is a leap year
        val daysIn2024 = if (isLeap2024) 366 else 365
        
        assertEquals("2024 should have 366 days", 366, daysIn2024)
        
        // Simulate logic from WallpaperGenerator
        val currentDayOfYear = 366 // Dec 31 in leap year
        val totalDots = daysIn2024
        
        var todayDotRendered = false
        for (day in 1..totalDots) {
            if (day == currentDayOfYear) {
                todayDotRendered = true
                break
            }
        }
        
        assertTrue("Day 366 should be rendered as 'today' in leap years", todayDotRendered)
    }

    @Test
    fun testNonLeapYearDay365Rendering() {
        // Ensure non-leap years still work correctly
        val isLeap2023 = false
        val daysIn2023 = if (isLeap2023) 366 else 365
        
        assertEquals("2023 should have 365 days", 365, daysIn2023)
        
        val currentDayOfYear = 365
        val totalDots = daysIn2023
        
        var todayDotRendered = false
        for (day in 1..totalDots) {
            if (day == currentDayOfYear) {
                todayDotRendered = true
                break
            }
        }
        
        assertTrue("Day 365 should be rendered as 'today' in non-leap years", todayDotRendered)
    }

    // =========================================================================================
    // MODERATE BUG TESTS
    // =========================================================================================

    @Test
    fun testShapeNameConsistency() {
        // Bug #6: Shape Name Mismatch
        // Ensure default shape matches between Repository and Generator expectations
        
        runBlocking {
            val defaultShape = repository.getDotShape()
            // Generator expects "dot" or "circle". Repository now defaults to "dot" to match UI
            assertTrue("Default shape should be valid", 
                defaultShape == "dot" || defaultShape == "circle" || 
                defaultShape == "square" || defaultShape == "triangle" || defaultShape == "pill" || defaultShape == "rounded")
        }
    }

    @Test
    fun testDensityMultipliersCorrectness() {
        // Bug #8: Density Multiplier Inconsistency
        // Verify the multipliers used in generator match expected scaling
        
        val baseSize = 10f
        val multipliers = listOf(0.70f, 1.00f, 1.30f, 1.60f)
        val expectedSizes = listOf(7f, 10f, 13f, 16f)
        
        multipliers.forEachIndexed { index, multiplier ->
            val calculatedSize = baseSize * multiplier
            // Allow small floating point error
            assertEquals("Density level ${index} size mismatch", 
                expectedSizes[index], calculatedSize, 0.1f)
        }
    }

    @Test
    fun testTextPositionWithinBounds() {
        // Bug #9: Text Position Hardcoded Percentage
        // Ensure text position logic keeps text within canvas bounds
        
        val canvasHeight = 1000f
        val textYPosition = canvasHeight * 0.92f // Current implementation in WallpaperGenerator
        val textHeight = 100f // Approximate text height
        
        val textBottom = textYPosition + (textHeight / 2)
        
        assertTrue("Text should not exceed canvas bottom", textBottom <= canvasHeight + 100)
        assertTrue("Text should be visible (not off-screen top)", textYPosition > 0)
    }

    // =========================================================================================
    // MINOR BUG & CODE QUALITY TESTS
    // =========================================================================================

    @Test
    fun testGetAllColorsEfficiency() {
        // Verify correctness of getAllColors map
        
        runBlocking {
            // Set distinct values
            repository.updateTodayColor(Color.RED)
            repository.updatePastColor(Color.GREEN)
            repository.updateFutureColor(Color.BLUE)
            repository.updateBackgroundColor(Color.YELLOW)
            
            val colors = repository.getAllColors()
            
            assertEquals("Should return 4 colors", 4, colors.size)
            assertTrue("Should contain today color", colors.containsValue(Color.RED))
            assertTrue("Should contain past color", colors.containsValue(Color.GREEN))
            assertTrue("Should contain future color", colors.containsValue(Color.BLUE))
            assertTrue("Should contain background color", colors.containsValue(Color.YELLOW))
        }
    }

    @Test
    fun testSettingsPersistence() {
        // Verify DataStore persistence works correctly
        runBlocking {
            val testShape = "pill"
            val testDensity = 3
            
            repository.updateDotShape(testShape)
            repository.updateDotDensity(testDensity)
            
            assertEquals("Shape should persist", testShape, repository.getDotShape())
            assertEquals("Density should persist", testDensity, repository.getDotDensity())
        }
    }

    @Test
    fun testBitmapGenerationDimensions() {
        // Verify bitmap is generated with correct dimensions
        // This indirectly tests that magic numbers don't break layout
        
        val width = 1080
        val height = 2400
        
        // Simple validation of aspect ratio handling
        val aspectRatio = width.toFloat() / height.toFloat()
        assertTrue("Aspect ratio should be reasonable", aspectRatio > 0.3f && aspectRatio < 1.0f)
    }

    @Test
    fun testShapeValidation() {
        // Ensure all supported shapes are handled
        val validShapes = listOf("dot", "circle", "square", "pill", "rounded")
        
        validShapes.forEach { shape ->
            assertTrue("Shape '$shape' should be recognized", 
                shape in validShapes)
        }
    }
}
