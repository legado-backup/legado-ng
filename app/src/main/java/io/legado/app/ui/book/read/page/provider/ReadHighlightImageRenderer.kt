package io.legado.app.ui.book.read.page.provider

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.util.LruCache
import io.legado.app.help.PaintPool
import splitties.init.appCtx
import java.io.File

/** 正文与规则预览共用的高亮背景图绘制，目标矩形由各自文字布局提供。 */
internal object ReadHighlightImageRenderer {
    private val highlightBitmapCache = LruCache<String, Bitmap>(16)

    fun draw(
        canvas: Canvas,
        bitmap: Bitmap,
        destination: RectF,
        style: ReadCharStyle,
    ) {
        val paint = PaintPool.obtain().apply {
            isAntiAlias = true
            isFilterBitmap = true
            this.style = Paint.Style.FILL
        }
        val scale = style.bgImageScale.coerceIn(0.1f, 5f)
        when (style.bgImageFit) {
            1 -> {
                val width = destination.width() * scale
                val drawHeight = destination.height() * scale
                val target = RectF(
                    destination.centerX() - width / 2,
                    destination.centerY() - drawHeight / 2,
                    destination.centerX() + width / 2,
                    destination.centerY() + drawHeight / 2,
                )
                canvas.save()
                canvas.clipRect(destination)
                canvas.drawBitmap(bitmap, null, target, paint)
                canvas.restore()
            }
            2 -> {
                val cover = maxOf(destination.width() / bitmap.width, destination.height() / bitmap.height) * scale
                val width = bitmap.width * cover
                val drawHeight = bitmap.height * cover
                val target = RectF(
                    destination.centerX() - width / 2,
                    destination.centerY() - drawHeight / 2,
                    destination.centerX() + width / 2,
                    destination.centerY() + drawHeight / 2,
                )
                canvas.save()
                canvas.clipRect(destination)
                canvas.drawBitmap(bitmap, null, target, paint)
                canvas.restore()
            }
            3 -> drawNineSlice(canvas, bitmap, destination, style, paint)
            else -> {
                val shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                shader.setLocalMatrix(Matrix().apply {
                    setScale(scale, scale)
                    postTranslate(destination.left, destination.top)
                })
                paint.shader = shader
                canvas.drawRect(destination, paint)
            }
        }
        PaintPool.recycle(paint)
    }

    private fun drawNineSlice(
        canvas: Canvas,
        bitmap: Bitmap,
        destination: RectF,
        style: ReadCharStyle,
        paint: Paint,
    ) {
        val sourceX = intArrayOf(
            0,
            (bitmap.width * style.npLeft).toInt().coerceIn(0, bitmap.width),
            (bitmap.width * (1f - style.npRight)).toInt().coerceIn(0, bitmap.width),
            bitmap.width,
        )
        val sourceY = intArrayOf(
            0,
            (bitmap.height * style.npTop).toInt().coerceIn(0, bitmap.height),
            (bitmap.height * (1f - style.npBottom)).toInt().coerceIn(0, bitmap.height),
            bitmap.height,
        )
        val left = minOf((sourceX[1] - sourceX[0]).toFloat(), destination.width() / 2)
        val right = minOf((sourceX[3] - sourceX[2]).toFloat(), destination.width() / 2)
        val top = minOf((sourceY[1] - sourceY[0]).toFloat(), destination.height() / 2)
        val bottom = minOf((sourceY[3] - sourceY[2]).toFloat(), destination.height() / 2)
        val targetX = floatArrayOf(destination.left, destination.left + left, destination.right - right, destination.right)
        val targetY = floatArrayOf(destination.top, destination.top + top, destination.bottom - bottom, destination.bottom)
        for (row in 0..2) for (column in 0..2) {
            if (sourceX[column] >= sourceX[column + 1] || sourceY[row] >= sourceY[row + 1]) continue
            canvas.drawBitmap(
                bitmap,
                Rect(sourceX[column], sourceY[row], sourceX[column + 1], sourceY[row + 1]),
                RectF(targetX[column], targetY[row], targetX[column + 1], targetY[row + 1]),
                paint,
            )
        }
    }

    fun loadBitmap(path: String): Bitmap? {
        if (path.isBlank()) return null
        highlightBitmapCache.get(path)?.let { return it }
        val bitmap = runCatching {
            when {
                path.startsWith("assets://") -> appCtx.assets.open(path.removePrefix("assets://"))
                    .use(BitmapFactory::decodeStream)
                path.startsWith("content://") -> appCtx.contentResolver.openInputStream(
                    android.net.Uri.parse(path)
                )?.use(BitmapFactory::decodeStream)
                else -> File(path).takeIf(File::isFile)?.let { BitmapFactory.decodeFile(it.absolutePath) }
            }
        }.getOrNull() ?: return null
        highlightBitmapCache.put(path, bitmap)
        return bitmap
    }
}
