package ru.skillbranch.devintensive.ui.custom

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorRes
import androidx.annotation.Dimension
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import ru.skillbranch.devintensive.App
import ru.skillbranch.devintensive.R
import ru.skillbranch.devintensive.extensions.*

class CircleImageView  @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    companion object {
        private const val DEFAULT_BORDER_WIDTH = 2//.dpToPixels
        private const val DEFAULT_BORDER_COLOR = Color.WHITE
    }

    private var borderWidth: Int = DEFAULT_BORDER_WIDTH.dpToPx(resources)//dpToPx(DEFAULT_BORDER_WIDTH)
    private var borderColor = DEFAULT_BORDER_COLOR

    private val paint: Paint = Paint().apply { isAntiAlias = true }
    private val paintBorder: Paint = Paint().apply { isAntiAlias = true }
    private val paintBackground: Paint = Paint().apply { isAntiAlias = true }
    private var circleCenter: Int = 0
    private var heightCircle: Int = 0

    private var civImage: Bitmap? = null
    private var civDrawable: Drawable? = null

    init {
        if(attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.CircleImageView, defStyleAttr, 0)

            borderWidth = a.getDimensionPixelSize(R.styleable.CircleImageView_cv_borderWidth, DEFAULT_BORDER_WIDTH.dpToPx(resources))//dpToPx(DEFAULT_BORDER_WIDTH))
            borderColor = a.getColor(R.styleable.CircleImageView_cv_borderColor, DEFAULT_BORDER_COLOR)

            a.recycle()
        }
    }

    fun getBorderWidth() = borderWidth.pxToDp(resources)//pxToDp(borderWidth)

    fun setBorderWidth(@Dimension(unit = Dimension.DP)dp: Int) {
        if (dp.dpToPx(resources) == borderWidth) return
        borderWidth = dp.dpToPx(resources)//dpToPx(dp)
    }

    fun getBorderColor(): Int = borderColor

    fun setBorderColor(hex: String) {
        val color = Color.parseColor(hex)
        if(color == borderColor) return
        borderColor = color
    }

    fun setBorderColor(@ColorRes colorId: Int) {
        if(colorId == borderColor) return
        borderColor = ContextCompat.getColor(App.applicationContext(), colorId)
    }

    override fun getScaleType(): ScaleType =
        super.getScaleType().let { if (it == null || it != ScaleType.CENTER_INSIDE) ScaleType.CENTER_CROP else it }

    override fun setScaleType(scaleType: ScaleType) {
        if (scaleType != ScaleType.CENTER_CROP && scaleType != ScaleType.CENTER_INSIDE)
            throw IllegalArgumentException(String.format("ScaleType is not supported." , scaleType))
        else
            super.setScaleType(scaleType)
    }

    override fun onDraw(canvas: Canvas) {
        loadBitmap()

        if (civImage == null) return

        val circleCenterWithBorder = circleCenter + borderWidth
        val margeRadius = 0f

        canvas.drawCircle(circleCenterWithBorder.toFloat(), circleCenterWithBorder.toFloat(), circleCenterWithBorder - margeRadius, paintBorder)
        canvas.drawCircle(circleCenterWithBorder.toFloat(), circleCenterWithBorder.toFloat(), circleCenter - margeRadius, paintBackground)
        canvas.drawCircle(circleCenterWithBorder.toFloat(), circleCenterWithBorder.toFloat(), circleCenter - margeRadius, paint)
    }

    private fun loadBitmap() {
        if (civDrawable == drawable) return

        civDrawable = drawable
        civImage = drawableToBitmap(civDrawable)
        updateShader()
    }

    private fun updateShader() {
        civImage?.also {
            val shader = BitmapShader(it, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val scale: Float
            val dx: Float
            val dy: Float

            when (scaleType) {
                ScaleType.CENTER_CROP -> if (it.width * height > width * it.height) {
                    scale = height / it.height.toFloat()
                    dx = (width - it.width * scale) * 0.5f
                    dy = 0f
                } else {
                    scale = width / it.width.toFloat()
                    dx = 0f
                    dy = (height - it.height * scale) * 0.5f
                }
                ScaleType.CENTER_INSIDE -> if (it.width * height < width * it.height) {
                    scale = height / it.height.toFloat()
                    dx = (width - it.width * scale) * 0.5f
                    dy = 0f
                } else {
                    scale = width / it.width.toFloat()
                    dx = 0f
                    dy = (height - it.height * scale) * 0.5f
                }
                else -> {
                    scale = 0f
                    dx = 0f
                    dy = 0f
                }
            }

            shader.setLocalMatrix(Matrix().apply {
                setScale(scale, scale)
                postTranslate(dx, dy)
            })

            paint.shader = shader
        }
    }

    private fun drawableToBitmap(drawable: Drawable?): Bitmap? =
        when (drawable) {
            null -> null
            is BitmapDrawable -> drawable.bitmap
            else -> try {
                val bitmap =
                    Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (civImage != null)
            updateShader()

        val usableWidth = width - (paddingLeft + paddingRight)
        val usableHeight = height - (paddingTop + paddingBottom)

        heightCircle = Math.min(usableWidth, usableHeight)

        circleCenter = ((heightCircle - borderWidth * 2) / 2)
        paintBorder.color = borderColor

        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = measure(widthMeasureSpec)
        val height = measure(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    private fun measure(measureSpec: Int): Int {
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        return when (specMode) {
            MeasureSpec.EXACTLY -> specSize
            MeasureSpec.AT_MOST -> specSize
            else -> heightCircle
        }
    }
}