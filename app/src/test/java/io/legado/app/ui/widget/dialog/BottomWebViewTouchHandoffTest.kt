package io.legado.app.ui.widget.dialog

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BottomWebViewTouchHandoffTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private class ScrollPage(context: Context) : WebView(context) {
        var aboveTop = false
        var cancelled = false
        override fun canScrollVertically(direction: Int) = direction < 0 && aboveTop
        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.actionMasked == MotionEvent.ACTION_CANCEL) cancelled = true
            return true
        }
    }

    private class SheetParent(context: Context) : FrameLayout(context) {
        var distance = 0
        var stopped = false
        override fun onStartNestedScroll(child: View, target: View, axes: Int) = true
        override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
            consumed[1] = dy
            distance += dy
        }
        override fun onStopNestedScroll(child: View) { stopped = true }
    }

    @Test
    fun `same gesture only transfers after page reaches top`() {
        val parent = SheetParent(context)
        val page = ScrollPage(context)
        parent.addView(page)
        page.isNestedScrollingEnabled = true
        val handoff = BottomWebViewTouchHandoff(page) { true }
        fun send(action: Int, y: Float): Boolean {
            val event = MotionEvent.obtain(0, 100, action, 10f, y, 0)
            return try { handoff.onTouch(page, event) } finally { event.recycle() }
        }
        try {
            page.aboveTop = true
            assertFalse(send(MotionEvent.ACTION_DOWN, 0f))
            assertFalse(send(MotionEvent.ACTION_MOVE, 100f))
            assertEquals(0, parent.distance)
            assertFalse(page.cancelled)
            page.aboveTop = false
            assertTrue(send(MotionEvent.ACTION_MOVE, 200f))
            assertEquals(-100, parent.distance)
            assertTrue(page.cancelled)
            assertTrue(send(MotionEvent.ACTION_UP, 200f))
            assertTrue(parent.stopped)
        } finally {
            handoff.finish()
            parent.removeView(page)
            page.destroy()
        }
    }
}
