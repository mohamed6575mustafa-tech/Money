package com.example

import androidx.compose.ui.graphics.Color
import com.example.core.ContrastUtils
import com.example.core.MoneyUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyCalculationUnitTest {

    @Test
    fun parseAmountToPiasters_handlesWesternAndArabicDigitsAndDecimals() {
        assertEquals(15050L, MoneyUtils.parseAmountToPiasters("150.50"))
        assertEquals(150000L, MoneyUtils.parseAmountToPiasters("1,500"))
        assertEquals(27525L, MoneyUtils.parseAmountToPiasters("٢٧٥٫٢٥"))
        assertNull(MoneyUtils.parseAmountToPiasters("0"))
        assertNull(MoneyUtils.parseAmountToPiasters("-45"))
        assertNull(MoneyUtils.parseAmountToPiasters("abc"))
    }

    @Test
    fun parseNonNegativeToPiasters_acceptsBlankAsZeroAndRejectsNegative() {
        assertEquals(0L, MoneyUtils.parseNonNegativeToPiasters(""))
        assertEquals(0L, MoneyUtils.parseNonNegativeToPiasters("0"))
        assertEquals(50000L, MoneyUtils.parseNonNegativeToPiasters("500"))
        assertNull(MoneyUtils.parseNonNegativeToPiasters("-10"))
    }

    @Test
    fun formatCurrency_formatsEgpAndAlternativeCurrencies() {
        assertEquals("1,250.50 ج.م", MoneyUtils.formatCurrency(125050L, "EGP"))
        assertEquals("-50.00 ر.س", MoneyUtils.formatCurrency(-5000L, "SAR"))
    }

    @Test
    fun yearMonthNavigation_wrapsYearBoundariesCorrectly() {
        assertEquals("2025-12", MoneyUtils.previousYearMonth("2026-01"))
        assertEquals("2026-01", MoneyUtils.nextYearMonth("2025-12"))
        assertEquals("2026-05", MoneyUtils.nextYearMonth("2026-04"))
    }

    @Test
    fun contrastUtils_selectsReadableForegroundForLightAndDarkColors() {
        assertEquals(Color.White, ContrastUtils.onColorFor(Color(0xFF064E3B)))
        assertEquals(Color(0xFF111827), ContrastUtils.onColorFor(Color(0xFFFDFBF7)))
    }
}
