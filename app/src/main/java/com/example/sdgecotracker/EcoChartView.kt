package com.example.sdgecotracker

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/** Line chart showing daily eco scores for the current user. */
class EcoChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var data: List<Pair<String, Int>> = emptyList()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#76B852")
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#347414")
        style = Paint.Style.FILL
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3376B852")
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A330D")
        textSize = 22f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#555555")
        textSize = 18f
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DDDDDD")
        strokeWidth = 1f
    }

    fun setData(data: List<Pair<String, Int>>) {
        this.data = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) {
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("No history data yet", width / 2f, height / 2f, textPaint)
            return
        }

        val padL = 60f; val padR = 20f; val padT = 20f; val padB = 50f
        val w = width - padL - padR
        val h = height - padT - padB

        val maxScore = (data.maxOf { it.second } + 10).coerceAtLeast(100)
        val minScore = (data.minOf { it.second } - 10).coerceAtMost(0)
        val range = (maxScore - minScore).toFloat()

        // Grid lines
        for (i in 0..4) {
            val yVal = minScore + (range * i / 4).toInt()
            val y = padT + h - (h * (yVal - minScore) / range)
            canvas.drawLine(padL, y, padL + w, y, gridPaint)
            labelPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(yVal.toString(), padL - 4f, y + 6f, labelPaint)
        }

        // Build path
        val path = Path()
        val fillPath = Path()
        data.forEachIndexed { i, (_, score) ->
            val x = padL + w * i / (data.size - 1).coerceAtLeast(1)
            val y = padT + h - (h * (score - minScore) / range)
            if (i == 0) { path.moveTo(x, y); fillPath.moveTo(x, padT + h) ; fillPath.lineTo(x, y) }
            else { path.lineTo(x, y); fillPath.lineTo(x, y) }
        }
        // Close fill path
        fillPath.lineTo(padL + w * (data.size - 1) / (data.size - 1).coerceAtLeast(1), padT + h)
        fillPath.close()
        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)

        // Dots + date labels (show every Nth label to avoid overlap)
        val step = ((data.size / 5) + 1).coerceAtLeast(1)
        data.forEachIndexed { i, (date, score) ->
            val x = padL + w * i / (data.size - 1).coerceAtLeast(1)
            val y = padT + h - (h * (score - minScore) / range)
            canvas.drawCircle(x, y, 5f, dotPaint)
            if (i % step == 0 || i == data.size - 1) {
                labelPaint.textAlign = Paint.Align.CENTER
                val shortDate = date.take(6) // "May 13"
                canvas.drawText(shortDate, x, padT + h + 35f, labelPaint)
            }
        }
    }
}
