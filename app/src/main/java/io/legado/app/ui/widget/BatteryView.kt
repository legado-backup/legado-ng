package io.legado.app.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ReplacementSpan
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.canvasrecorder.CanvasRecorderFactory
import io.legado.app.utils.canvasrecorder.recordIfNeededThenDraw
import io.legado.app.utils.dpToPx
import kotlin.math.ceil

class BatteryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {
    private val batteryTypeface by lazy {
        Typeface.createFromAsset(context.assets, "font/number.ttf")
    }
    private val canvasRecorder = CanvasRecorderFactory.create()
    var isBattery = false
        set(value) {
            field = value
            if (value && !isInEditMode) {
                super.setTypeface(batteryTypeface)
            }
            invalidate()
        }

    init {
        setPadding(4.dpToPx(), 3.dpToPx(), 6.dpToPx(), 3.dpToPx())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isFallbackLineSpacing = false
        }
    }

    override fun setTypeface(tf: Typeface?) {
        if (!isBattery) {
            super.setTypeface(tf)
        }
    }

    fun setColor(@ColorInt color: Int) {
        setTextColor(color)
        invalidate()
    }

    fun setBattery(battery: Int, text: String? = null) {
        val number = battery.toString()
        val label = if (text.isNullOrEmpty()) number else "$text  $number"
        setText(SpannableString(label).apply {
            if (isBattery) {
                setSpan(
                    BatterySpan(), label.length - number.length, label.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        })
        invalidate()
    }

    private class BatterySpan : ReplacementSpan() {
        private val batteryPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val frame = RectF()
        private val numberBounds = Rect()

        override fun getSize(
            paint: Paint,
            text: CharSequence,
            start: Int,
            end: Int,
            fm: Paint.FontMetricsInt?
        ): Int {
            // Preserve the original line height, including when the battery is the only text.
            if (fm != null) paint.getFontMetricsInt(fm)
            return ceil(paint.textSize * (BODY_WIDTH + TERMINAL_GAP + TERMINAL_WIDTH)).toInt()
        }

        override fun draw(
            canvas: Canvas,
            text: CharSequence,
            start: Int,
            end: Int,
            x: Float,
            top: Int,
            y: Int,
            bottom: Int,
            paint: Paint
        ) {
            val size = paint.textSize
            val bodyWidth = size * BODY_WIDTH
            val bodyHeight = size * 0.95f
            val stroke = (size * 0.065f).coerceAtLeast(0.6f.dpToPx())
            // Align the shell optically with the adjacent time digits, not the TextView bounds.
            paint.getTextBounds("0", 0, 1, numberBounds)
            val centerY = y + (numberBounds.top + numberBounds.bottom) / 2f
            frame.set(
                x + stroke / 2f,
                centerY - bodyHeight / 2f + stroke / 2f,
                x + bodyWidth - stroke / 2f,
                centerY + bodyHeight / 2f - stroke / 2f
            )
            batteryPaint.set(paint)
            batteryPaint.isAntiAlias = true
            batteryPaint.style = Paint.Style.STROKE
            batteryPaint.strokeWidth = stroke
            val radius = bodyHeight * 0.24f
            canvas.drawRoundRect(frame, radius, radius, batteryPaint)

            batteryPaint.style = Paint.Style.FILL
            val terminalLeft = x + bodyWidth + size * TERMINAL_GAP
            val terminalWidth = size * TERMINAL_WIDTH
            frame.set(
                terminalLeft, centerY - bodyHeight * 0.2f,
                terminalLeft + terminalWidth, centerY + bodyHeight * 0.2f
            )
            canvas.drawRoundRect(frame, terminalWidth / 2f, terminalWidth / 2f, batteryPaint)

            batteryPaint.textSize = size * 0.78f
            batteryPaint.textAlign = Paint.Align.CENTER
            batteryPaint.getTextBounds("0", 0, 1, numberBounds)
            val numberBaseline = centerY - (numberBounds.top + numberBounds.bottom) / 2f
            canvas.drawText(text, start, end, x + bodyWidth / 2f, numberBaseline, batteryPaint)
        }

        private companion object {
            const val BODY_WIDTH = 1.8f
            const val TERMINAL_GAP = 0.08f
            const val TERMINAL_WIDTH = 0.12f
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        canvasRecorder.invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (AppConfig.optimizeRender) {
            canvasRecorder.recordIfNeededThenDraw(canvas, width, height) {
                super.onDraw(this)
            }
        } else {
            super.onDraw(canvas)
        }
    }

    @Suppress("UNNECESSARY_SAFE_CALL")
    override fun invalidate() {
        super.invalidate()
        canvasRecorder?.invalidate()
    }

}
