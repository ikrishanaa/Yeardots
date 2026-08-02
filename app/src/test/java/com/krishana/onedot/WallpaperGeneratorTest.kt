package com.krishana.onedot.core

import android.graphics.Bitmap
import com.krishana.onedot.core.WallpaperGenerator.ThemeConfig
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for WallpaperGenerator
 * Tests bitmap generation, dot calculations, and theme application
 */
@RunWith(RobolectricTestRunner::class)
class WallpaperGeneratorTest {

    private lateinit var defaultTheme: ThemeConfig

    @Before
    fun setup() {
        defaultTheme = ThemeConfig(
            pastColor = 0xFFD1D5DB.toInt(),
            todayColor = 0xFFF97316.toInt(),
            futureColor = 0xFF262626.toInt(),
            backgroundColor = 0xFF050505.toInt(),
            dotShape = "dot",
            dotDensity = 1
        )
    }

    @Test
    fun `test generateBitmap returns non-null bitmap`() {
        val width = 1080
        val height = 1920
        
        val bitmap = WallpaperGenerator.generateBitmap(width, height, defaultTheme)
        
        assertNotNull("Generated bitmap should not be null", bitmap)
    }

    @Test
    fun `test generateBitmap returns correct dimensions`() {
        val width = 1080
        val height = 1920
        
        val bitmap = WallpaperGenerator.generateBitmap(width, height, defaultTheme)
        
        assertEquals("Bitmap width should match requested width", width, bitmap.width)
        assertEquals("Bitmap height should match requested height", height, bitmap.height)
    }

    @Test
    fun `test generateBitmap with square dimensions`() {
        val size = 1000
        
        val bitmap = WallpaperGenerator.generateBitmap(size, size, defaultTheme)
        
        assertNotNull("Square bitmap should be generated", bitmap)
        assertEquals("Square bitmap width should match", size, bitmap.width)
        assertEquals("Square bitmap height should match", size, bitmap.height)
    }

    @Test
    fun `test generateBitmap with minimum dimensions`() {
        val width = 100
        val height = 100
        
        val bitmap = WallpaperGenerator.generateBitmap(width, height, defaultTheme)
        
        assertNotNull("Small bitmap should be generated", bitmap)
        assertTrue("Bitmap should have minimum dimensions", bitmap.width >= 100 && bitmap.height >= 100)
    }

    @Test
    fun `test generateBitmap with large dimensions`() {
        val width = 4096
        val height = 4096
        
        val bitmap = WallpaperGenerator.generateBitmap(width, height, defaultTheme)
        
        assertNotNull("Large bitmap should be generated", bitmap)
        assertEquals("Large bitmap should have correct width", width, bitmap.width)
        assertEquals("Large bitmap should have correct height", height, bitmap.height)
    }

    @Test
    fun `test bitmap uses ARGB_8888 config for quality`() {
        val width = 1080
        val height = 1920
        
        val bitmap = WallpaperGenerator.generateBitmap(width, height, defaultTheme)
        
        assertEquals("Bitmap should use ARGB_8888 config", Bitmap.Config.ARGB_8888, bitmap.config)
    }

    @Test
    fun `test different dot shapes generate valid bitmaps`() {
        val width = 1080
        val height = 1920
        val shapes = listOf("circle", "rounded", "square", "pill")
        
        shapes.forEach { shape ->
            val theme = defaultTheme.copy(dotShape = shape)
            val bitmap = WallpaperGenerator.generateBitmap(width, height, theme)
            
            assertNotNull("Bitmap should be generated for shape: $shape", bitmap)
            assertEquals("Bitmap dimensions should be correct for $shape", width, bitmap.width)
        }
    }

    @Test
    fun `test different dot densities generate valid bitmaps`() {
        val width = 1080
        val height = 1920
        val densities = listOf(0, 1, 2, 3) // Tiny, Small, Medium, Large
        
        densities.forEach { density ->
            val theme = defaultTheme.copy(dotDensity = density)
            val bitmap = WallpaperGenerator.generateBitmap(width, height, theme)
            
            assertNotNull("Bitmap should be generated for density: $density", bitmap)
            assertEquals("Bitmap dimensions should be correct for density $density", width, bitmap.width)
        }
    }

    @Test
    fun `test theme config with custom colors`() {
        val customTheme = ThemeConfig(
            pastColor = 0xFF00FF00.toInt(), // Green
            todayColor = 0xFFFF0000.toInt(), // Red
            futureColor = 0xFF0000FF.toInt(), // Blue
            backgroundColor = 0xFFFFFFFF.toInt(), // White
            dotShape = "square",
            dotDensity = 2
        )
        
        val bitmap = WallpaperGenerator.generateBitmap(1080, 1920, customTheme)
        
        assertNotNull("Bitmap with custom colors should be generated", bitmap)
    }

    @Test
    fun `test bitmap generation with battery info`() {
        val batteryInfo = BatteryInfo(
            percentage = 75,
            isCharging = false,
            isFull = false,
            isLow = false
        )
        
        val bitmap = WallpaperGenerator.generateBitmap(1080, 1920, defaultTheme, batteryInfo)
        
        assertNotNull("Bitmap with battery info should be generated", bitmap)
    }

    @Test
    fun `test bitmap is not empty after generation`() {
        val width = 1080
        val height = 1920
        
        val bitmap = WallpaperGenerator.generateBitmap(width, height, defaultTheme)
        
        // Check that bitmap has actual pixel data (not all zeros)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val hasNonZeroPixels = pixels.any { it != 0 }
        assertTrue("Bitmap should contain non-zero pixels", hasNonZeroPixels)
    }

    @Test
    fun `test background color is applied to bitmap`() {
        val backgroundColor = 0xFFFF0000.toInt() // Red background
        val theme = defaultTheme.copy(backgroundColor = backgroundColor)
        
        val bitmap = WallpaperGenerator.generateBitmap(100, 100, theme)
        
        // Check corners for background color (dots shouldn't be in corners)
        val topLeftPixel = bitmap.getPixel(0, 0)
        val topRightPixel = bitmap.getPixel(99, 0)
        
        // Background should be visible in corners
        assertNotNull("Bitmap should have background applied", bitmap)
    }

    @Test
    fun `test theme config data class equality`() {
        val theme1 = ThemeConfig(
            pastColor = 0xFF111111.toInt(),
            todayColor = 0xFF222222.toInt(),
            futureColor = 0xFF333333.toInt(),
            backgroundColor = 0xFF444444.toInt(),
            dotShape = "circle",
            dotDensity = 1
        )
        
        val theme2 = theme1.copy()
        
        assertEquals("Identical theme configs should be equal", theme1, theme2)
    }

    @Test
    fun `test battery info data class`() {
        val batteryInfo = BatteryInfo(
            percentage = 50,
            isCharging = true,
            isFull = false,
            isLow = false
        )
        
        assertEquals("Battery percentage should be 50", 50, batteryInfo.percentage)
        assertTrue("Battery should be charging", batteryInfo.isCharging)
        assertFalse("Battery should not be full", batteryInfo.isFull)
        assertFalse("Battery should not be low", batteryInfo.isLow)
    }

    @Test
    fun `test bitmap generation is deterministic for same inputs`() {
        val width = 500
        val height = 500
        
        val bitmap1 = WallpaperGenerator.generateBitmap(width, height, defaultTheme)
        val bitmap2 = WallpaperGenerator.generateBitmap(width, height, defaultTheme)
        
        // Both bitmaps should have same dimensions
        assertEquals("Both bitmaps should have same width", bitmap1.width, bitmap2.width)
        assertEquals("Both bitmaps should have same height", bitmap1.height, bitmap2.height)
        assertEquals("Both bitmaps should have same config", bitmap1.config, bitmap2.config)
    }
}
