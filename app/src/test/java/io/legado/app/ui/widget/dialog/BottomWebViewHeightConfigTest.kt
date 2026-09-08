package io.legado.app.ui.widget.dialog

import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomWebViewHeightConfigTest {

    @Test
    fun `positive pixel height enables fixed mode`() {
        val spec = resolveBottomSheetHeightSpec(1_000, 480, null, first = true)

        assertEquals(480, spec.layoutHeight)
        assertEquals(480, spec.fixedHeight)
    }

    @Test
    fun `valid percentage takes precedence and enables fixed mode`() {
        val spec = resolveBottomSheetHeightSpec(1_000, 480, 0.75f, first = true)

        assertEquals(750, spec.layoutHeight)
        assertEquals(750, spec.fixedHeight)
    }

    @Test
    fun `invalid percentage does not create zero height fixed mode`() {
        assertEquals(
            BottomSheetHeightSpec(480, 480),
            resolveBottomSheetHeightSpec(1_000, 480, 0f, first = true),
        )
        assertEquals(
            BottomSheetHeightSpec(ViewGroup.LayoutParams.MATCH_PARENT, null),
            resolveBottomSheetHeightSpec(1_000, null, 1.1f, first = true),
        )
        assertEquals(
            BottomSheetHeightSpec(null, null),
            resolveBottomSheetHeightSpec(1_000, -3, -0.5f, first = false),
        )
    }

    @Test
    fun `layout constants retain flexible behavior`() {
        val matchParent = resolveBottomSheetHeightSpec(
            1_000,
            ViewGroup.LayoutParams.MATCH_PARENT,
            null,
            first = true,
        )
        val wrapContent = resolveBottomSheetHeightSpec(
            1_000,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            null,
            first = false,
        )

        assertNull(matchParent.fixedHeight)
        assertNull(wrapContent.fixedHeight)
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, matchParent.layoutHeight)
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, wrapContent.layoutHeight)
    }

    @Test
    fun `fixed mode supplies stable defaults`() {
        val spec = resolveBottomSheetBehaviorSpec(
            fixedHeight = 750,
            resetFixedDefaults = false,
            state = null,
            peekHeight = null,
            skipCollapsed = null,
            fitToContents = null,
            draggableOnNestedScroll = null,
            maxHeight = null,
        )

        assertEquals(BottomSheetBehavior.STATE_EXPANDED, spec.state)
        assertEquals(750, spec.peekHeight)
        assertEquals(750, spec.maxHeight)
        assertTrue(spec.skipCollapsed == true)
        assertTrue(spec.fitToContents == true)
        assertFalse(spec.draggableOnNestedScroll == true)
    }

    @Test
    fun `paragraph review config keeps explicit non fitting mode`() {
        val spec = resolveBottomSheetBehaviorSpec(
            fixedHeight = 750,
            resetFixedDefaults = false,
            state = null,
            peekHeight = null,
            skipCollapsed = true,
            fitToContents = false,
            draggableOnNestedScroll = null,
            maxHeight = null,
        )

        assertEquals(BottomSheetBehavior.STATE_EXPANDED, spec.state)
        assertEquals(750, spec.peekHeight)
        assertEquals(750, spec.maxHeight)
        assertTrue(spec.skipCollapsed == true)
        assertFalse(spec.fitToContents == true)
        assertFalse(spec.draggableOnNestedScroll == true)
    }

    @Test
    fun `explicit behavior fields override fixed mode defaults`() {
        val spec = resolveBottomSheetBehaviorSpec(
            fixedHeight = 480,
            resetFixedDefaults = false,
            state = BottomSheetBehavior.STATE_COLLAPSED,
            peekHeight = 320,
            skipCollapsed = false,
            fitToContents = false,
            draggableOnNestedScroll = true,
            maxHeight = 640,
        )

        assertEquals(BottomSheetBehavior.STATE_COLLAPSED, spec.state)
        assertEquals(320, spec.peekHeight)
        assertEquals(640, spec.maxHeight)
        assertFalse(spec.skipCollapsed == true)
        assertFalse(spec.fitToContents == true)
        assertTrue(spec.draggableOnNestedScroll == true)
    }

    @Test
    fun `leaving fixed mode restores flexible constraints without changing state`() {
        val spec = resolveBottomSheetBehaviorSpec(
            fixedHeight = null,
            resetFixedDefaults = true,
            state = null,
            peekHeight = null,
            skipCollapsed = null,
            fitToContents = null,
            draggableOnNestedScroll = null,
            maxHeight = null,
        )

        assertNull(spec.state)
        assertEquals(BottomSheetBehavior.PEEK_HEIGHT_AUTO, spec.peekHeight)
        assertEquals(-1, spec.maxHeight)
        assertFalse(spec.skipCollapsed == true)
        assertTrue(spec.fitToContents == true)
        assertTrue(spec.draggableOnNestedScroll == true)
    }

    @Test
    fun `leaving fixed mode only resets defaults owned by fixed mode`() {
        val spec = resolveBottomSheetBehaviorSpec(
            fixedHeight = null,
            resetFixedDefaults = true,
            state = null,
            peekHeight = null,
            skipCollapsed = null,
            fitToContents = null,
            draggableOnNestedScroll = null,
            maxHeight = null,
            resetPeekHeight = false,
            resetSkipCollapsed = true,
            resetFitToContents = false,
            resetDraggableOnNestedScroll = true,
            resetMaxHeight = false,
        )

        assertNull(spec.peekHeight)
        assertFalse(spec.skipCollapsed == true)
        assertNull(spec.fitToContents)
        assertTrue(spec.draggableOnNestedScroll == true)
        assertNull(spec.maxHeight)
    }

    @Test
    fun `full screen snapshot normalizes transient states`() {
        assertEquals(
            BottomSheetBehavior.STATE_EXPANDED,
            stableBottomSheetState(BottomSheetBehavior.STATE_DRAGGING),
        )
        assertEquals(
            BottomSheetBehavior.STATE_EXPANDED,
            stableBottomSheetState(BottomSheetBehavior.STATE_SETTLING),
        )
        assertEquals(
            BottomSheetBehavior.STATE_COLLAPSED,
            stableBottomSheetState(BottomSheetBehavior.STATE_COLLAPSED),
        )
        assertNull(stableBottomSheetState(null))
    }

    @Test
    fun `non fitting fixed sheet stays anchored to bottom`() {
        assertEquals(
            250,
            resolveBottomSheetExpandedOffset(
                parentHeight = 1_000,
                sheetHeight = 750,
                isFullScreen = false,
                configuredExpandedOffset = null,
                isFixedHeight = true,
                fitToContents = false,
            ),
        )
        assertEquals(
            0,
            resolveBottomSheetExpandedOffset(
                parentHeight = 1_000,
                sheetHeight = 750,
                isFullScreen = false,
                configuredExpandedOffset = null,
                isFixedHeight = true,
                fitToContents = true,
            ),
        )
    }

    @Test
    fun `full screen overrides an explicit expanded offset`() {
        assertEquals(
            0,
            resolveBottomSheetExpandedOffset(
                parentHeight = 1_000,
                sheetHeight = 750,
                isFullScreen = true,
                configuredExpandedOffset = 160,
                isFixedHeight = true,
                fitToContents = false,
            ),
        )
    }
}
