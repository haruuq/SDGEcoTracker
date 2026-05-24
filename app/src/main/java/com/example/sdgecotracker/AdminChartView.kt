package com.example.sdgecotracker

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/** Horizontal bar chart showing category distribution for admin analytics. */
class AdminChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var data: Map<String, Int> = emptyMap()

    private val barColors = listOf(
        Color.parseColor("#76B852"),
        Color.parseColor("#347414"),
        Color.parseColor("#2D5A18"),
        Color.parseColor("#A8D878"),
        Color.parseColor("#1A330D")
    )
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A330D")
        textSize = 24f
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 20f
        isFakeBoldText = true
    }

    fun setData(data: Map<String, Int>) {
        this.data = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) {
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("No community data yet", width / 2f, height / 2f, textPaint)
            return
        }
        val maxVal = data.values.maxOrNull()?.toFloat() ?: 1f
        val barH = (height - 20f) / data.size * 0.7f
        val gap = (height - 20f) / data.size * 0.3f
        val labelW = 150f
        val barAreaW = width - labelW - 60f

        data.entries.forEachIndexed { i, (cat, count) ->
            val y = 10f + i * (barH + gap)
            val barW = barAreaW * count / maxVal

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = barColors[i % barColors.size]
                style = Paint.Style.FILL
            }
            val rect = RectF(labelW, y, labelW + barW, y + barH)
            canvas.drawRoundRect(rect, 6f, 6f, paint)

            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(cat, labelW - 8f, y + barH * 0.7f, textPaint)

            valuePaint.textAlign = Paint.Align.LEFT
            canvas.drawText("$count", labelW + barW + 6f, y + barH * 0.7f, valuePaint.apply { color = Color.parseColor("#1A330D") })
        }
    }
}
