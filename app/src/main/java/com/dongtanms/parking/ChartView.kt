package com.dongtanms.parking

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class ChartView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val barPaint = Paint().apply {
        color = Color.parseColor("#426A44") // 동탄명성교회 그린
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 32f
        isAntiAlias = true
    }

    private var data: IntArray = IntArray(24) // 기본: 시간대별

    fun setData(values: IntArray) {
        data = values
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val barWidth = width / (data.size * 1.2f)
        val maxVal = (data.maxOrNull() ?: 1).toFloat()

        data.forEachIndexed { index, value ->
            val left = index * (barWidth * 1.2f)
            val top = height - (value / maxVal * (height * 0.8f))
            val right = left + barWidth
            val bottom = height * 0.95f

            val rect = RectF(left, top, right, bottom)
            canvas.drawRect(rect, barPaint)

            // 아래에 시간 표시
            if (index % 3 == 0) {
                canvas.drawText(index.toString(), left, height.toFloat(), textPaint)
            }
        }
    }
}