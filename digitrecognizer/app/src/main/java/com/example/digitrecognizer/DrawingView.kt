package com.example.digitrecognizer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 60f           // настраиваемая толщина штриха
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private var drawPath = Path()
    private lateinit var bufferBitmap: Bitmap
    private lateinit var bufferCanvas: Canvas
    private val bgColor = Color.WHITE

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (::bufferBitmap.isInitialized) bufferBitmap.recycle()
        bufferBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bufferCanvas = Canvas(bufferBitmap)
        bufferCanvas.drawColor(bgColor)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(bufferBitmap, 0f, 0f, null)
        canvas.drawPath(drawPath, paint)
    }

    private var lastX = 0f
    private var lastY = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                drawPath.moveTo(x, y)
                lastX = x; lastY = y
            }
            MotionEvent.ACTION_MOVE -> {
                drawPath.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2)
                lastX = x; lastY = y
            }
            MotionEvent.ACTION_UP -> {
                drawPath.lineTo(lastX, lastY)
                bufferCanvas.drawPath(drawPath, paint)
                drawPath.reset()
            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun clear() {
        bufferCanvas.drawColor(bgColor)
        invalidate()
    }

    fun getBitmap(): Bitmap {
        // возвращаем копию, чтобы внешняя логика не изменила наш internal bitmap
        return bufferBitmap.copy(Bitmap.Config.ARGB_8888, false)
    }
}