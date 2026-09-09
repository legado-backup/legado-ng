package io.legado.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.legado.app.ui.book.read.page.provider.ReadCharStyle
import io.legado.app.ui.book.read.page.provider.ReadHighlightImageRenderer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadHighlightImageRendererTest {
    @Test
    fun tileStartsAtTextBoundsAndRepeats() {
        withBitmaps { source, output ->
            draw(source, output, ReadCharStyle(bgImageFit = 0))
            assertEquals(Color.RED, output.getPixel(9, 9))
            assertEquals(Color.BLUE, output.getPixel(13, 9))
            assertEquals(Color.RED, output.getPixel(17, 9))
            assertEquals(Color.TRANSPARENT, output.getPixel(7, 9))
        }
    }

    @Test
    fun stretchScalesAroundCenterWhileCoverFillsBounds() {
        withBitmaps { source, output ->
            draw(source, output, ReadCharStyle(bgImageFit = 1, bgImageScale = 0.5f))
            assertEquals(Color.TRANSPARENT, output.getPixel(9, 9))
            assertEquals(Color.RED, output.getPixel(18, 15))
            assertEquals(Color.BLUE, output.getPixel(29, 15))
            output.eraseColor(Color.TRANSPARENT)
            draw(source, output, ReadCharStyle(bgImageFit = 2))
            assertEquals(Color.RED, output.getPixel(9, 9))
            assertEquals(Color.BLUE, output.getPixel(38, 22))
            assertEquals(Color.TRANSPARENT, output.getPixel(40, 22))
        }
    }

    @Test
    fun nineSlicePreservesCornerSizeAndStretchesCenter() {
        withBitmaps { source, output ->
            // 两像素边缘保持原始宽度，中间绿色区域拉伸。
            for (y in 2..5) for (x in 2..5) source.setPixel(x, y, Color.GREEN)
            draw(
                source, output,
                ReadCharStyle(
                    bgImageFit = 3,
                    npLeft = 0.25f, npRight = 0.25f,
                    npTop = 0.25f, npBottom = 0.25f,
                ),
            )
            assertEquals(Color.RED, output.getPixel(8, 8))
            assertEquals(Color.BLUE, output.getPixel(39, 8))
            assertEquals(Color.GREEN, output.getPixel(24, 16))
            assertEquals(Color.TRANSPARENT, output.getPixel(24, 24))
        }
    }

    private fun draw(source: Bitmap, output: Bitmap, style: ReadCharStyle) {
        ReadHighlightImageRenderer.draw(Canvas(output), source, RectF(8f, 8f, 40f, 24f), style)
    }

    private fun withBitmaps(test: (Bitmap, Bitmap) -> Unit) {
        val source = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val output = Bitmap.createBitmap(48, 32, Bitmap.Config.ARGB_8888)
        try {
            for (y in 0..7) for (x in 0..7) {
                source.setPixel(x, y, if (x < 4) Color.RED else Color.BLUE)
            }
            test(source, output)
        } finally {
            source.recycle()
            output.recycle()
        }
    }
}
