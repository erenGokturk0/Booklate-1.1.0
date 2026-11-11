package com.erengokturk.booklate.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.compose.ui.graphics.toArgb
import com.erengokturk.booklate.domain.model.PageWord
import com.erengokturk.booklate.ui.theme.HighlightActive
import com.erengokturk.booklate.ui.theme.HighlightSelection
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * PDF sayfasını gösteren, kelime koordinatlarını hedefleyen özel View.
 *
 * Compose içinde AndroidView ile kullanılır. Pinch-zoom, çift dokunma, kaydırma,
 * tek kelime seçimi ve uzun basıp sürükleyerek çoklu seçim destekler.
 */
@SuppressLint("ClickableViewAccessibility")
class PdfPageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    interface Listener {
        fun onWordTapped(wordIndex: Int)
        fun onSelectionStarted(wordIndex: Int)
        fun onSelectionExtended(startWord: Int, endWord: Int)
        fun onSelectionFinished(startWord: Int, endWord: Int)
        fun onEmptyTap()
    }

    var listener: Listener? = null

    private var bitmap: Bitmap? = null
    private var words: List<PageWord> = emptyList()
    private var selection: IntRange? = null
    private var activeWord: Int? = null
    private var pageKey: String = ""

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = HighlightSelection.toArgb()
        style = Paint.Style.FILL
    }
    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = HighlightActive.toArgb()
        style = Paint.Style.FILL
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        alpha = 30
        style = Paint.Style.FILL
    }
    private val backgroundPaint = Paint().apply { color = Color.TRANSPARENT }
    private val matrix = Matrix()
    private val inverse = Matrix()
    private val pageRect = RectF()
    private val shadowRect = RectF()
    private val wordRect = RectF()

    private var baseScale = 1f
    private var zoom = 1f
    private var panX = 0f
    private var panY = 0f
    private var fitted = false

    private var selectionDragging = false
    private var selectionAnchor = -1
    private var lastSelectedWord = -1

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val bmp = bitmap ?: return false
                if (baseScale <= 0f) return false
                val oldZoom = zoom
                val newZoom = (zoom * detector.scaleFactor).coerceIn(1f, 5f)
                if (newZoom == oldZoom) return false

                val centerX = width / 2f
                val centerY = height / 2f
                val contentX = (detector.focusX - centerX - panX) / (baseScale * oldZoom)
                val contentY = (detector.focusY - centerY - panY) / (baseScale * oldZoom)
                zoom = newZoom
                panX = detector.focusX - centerX - contentX * baseScale * zoom
                panY = detector.focusY - centerY - contentY * baseScale * zoom
                clampPan(bmp)
                invalidate()
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                if (zoom <= 1.01f) {
                    zoom = 1f
                    panX = 0f
                    panY = 0f
                }
                updateIntercept()
                invalidate()
            }
        },
    )

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val word = findWordAt(e.x, e.y)
                performClick()
                if (word != null) {
                    listener?.onWordTapped(word)
                } else {
                    listener?.onEmptyTap()
                }
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                val word = findWordAt(e.x, e.y) ?: return
                selectionDragging = true
                selectionAnchor = word
                lastSelectedWord = word
                selection = word..word
                performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                parent?.requestDisallowInterceptTouchEvent(true)
                listener?.onSelectionStarted(word)
                invalidate()
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float,
            ): Boolean {
                if (selectionDragging) return true
                if (zoom <= 1.01f) return false
                val bmp = bitmap ?: return false
                panX -= distanceX
                panY -= distanceY
                clampPan(bmp)
                invalidate()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val bmp = bitmap ?: return false
                if (zoom > 1.2f) {
                    zoom = 1f
                    panX = 0f
                    panY = 0f
                } else {
                    zoom = 2.5f
                    val centerX = width / 2f
                    val centerY = height / 2f
                    val contentX = (e.x - centerX) / baseScale
                    val contentY = (e.y - centerY) / baseScale
                    panX = e.x - centerX - contentX * baseScale * zoom
                    panY = e.y - centerY - contentY * baseScale * zoom
                }
                clampPan(bmp)
                updateIntercept()
                invalidate()
                return true
            }
        },
    )

    init {
        isClickable = true
        isFocusable = true
        contentDescription = "PDF sayfası"
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun setPage(
        newBitmap: Bitmap?,
        newWords: List<PageWord>,
        key: String,
    ) {
        val pageChanged = key != pageKey
        bitmap = newBitmap
        words = newWords
        pageKey = key
        if (pageChanged) {
            resetTransform()
            fitted = false
            selection = null
            activeWord = null
            selectionDragging = false
        }
        if (!fitted && width > 0 && height > 0) fitToView()
        invalidate()
    }

    fun setHighlights(selected: IntRange?, active: Int?) {
        selection = selected
        activeWord = active
        invalidate()
    }

    fun clearSelection() {
        selection = null
        selectionDragging = false
        invalidate()
    }

    fun resetZoom() {
        resetTransform()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            fitted = false
            fitToView()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        val bmp = bitmap ?: return
        if (bmp.isRecycled) return

        buildMatrix(bmp)
        pageRect.set(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat())
        matrix.mapRect(pageRect)
        val shadowOffset = 4f * resources.displayMetrics.density
        shadowRect.set(
            pageRect.left + shadowOffset / 2f,
            pageRect.top + shadowOffset,
            pageRect.right + shadowOffset / 2f,
            pageRect.bottom + shadowOffset,
        )
        canvas.drawRoundRect(
            shadowRect,
            6f,
            6f,
            shadowPaint,
        )
        canvas.drawBitmap(bmp, matrix, bitmapPaint)

        val radius = 4f * resources.displayMetrics.density
        val bmpW = bmp.width.toFloat()
        val bmpH = bmp.height.toFloat()

        selection?.forEach { index ->
            val word = words.getOrNull(index) ?: return@forEach
            val r = word.rect
            wordRect.set(r.left * bmpW, r.top * bmpH, r.right * bmpW, r.bottom * bmpH)
            matrix.mapRect(wordRect)
            canvas.drawRoundRect(wordRect, radius, radius, selectionPaint)
        }

        activeWord?.let { index ->
            val word = words.getOrNull(index)
            if (word != null && selection?.contains(index) != true) {
                val r = word.rect
                wordRect.set(r.left * bmpW, r.top * bmpH, r.right * bmpW, r.bottom * bmpH)
                matrix.mapRect(wordRect)
                canvas.drawRoundRect(wordRect, radius, radius, activePaint)
            }
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (bitmap == null) return false
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            updateIntercept()
        }
        scaleDetector.onTouchEvent(event)

        if (selectionDragging) {
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    val word = findWordAt(event.x, event.y)
                    if (word != null && word != lastSelectedWord) {
                        lastSelectedWord = word
                        val start = min(selectionAnchor, word)
                        val end = max(selectionAnchor, word)
                        selection = start..end
                        listener?.onSelectionExtended(start, end)
                        invalidate()
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    selectionDragging = false
                    listener?.onSelectionFinished(
                        selection?.first ?: selectionAnchor,
                        selection?.last ?: selectionAnchor,
                    )
                    updateIntercept()
                    return true
                }
            }
        }

        gestureDetector.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP ||
            event.actionMasked == MotionEvent.ACTION_CANCEL
        ) {
            updateIntercept()
        }
        return true
    }

    private fun fitToView() {
        val bmp = bitmap ?: return
        if (width <= 0 || height <= 0 || bmp.width <= 0 || bmp.height <= 0) return
        val margin = 2f * resources.displayMetrics.density
        baseScale = min(
            (width - margin * 2f) / bmp.width,
            (height - margin * 2f) / bmp.height,
        ).coerceAtLeast(0.01f)
        zoom = 1f
        panX = 0f
        panY = 0f
        fitted = true
        invalidate()
    }

    private fun buildMatrix(bmp: Bitmap) {
        val scale = baseScale * zoom
        val offsetX = (width - bmp.width * scale) / 2f + panX
        val offsetY = (height - bmp.height * scale) / 2f + panY
        matrix.reset()
        matrix.postScale(scale, scale)
        matrix.postTranslate(offsetX, offsetY)
    }

    private fun clampPan(bmp: Bitmap) {
        val scale = baseScale * zoom
        val scaledW = bmp.width * scale
        val scaledH = bmp.height * scale
        val maxPanX = max(0f, (scaledW - width) / 2f)
        val maxPanY = max(0f, (scaledH - height) / 2f)
        panX = panX.coerceIn(-maxPanX, maxPanX)
        panY = panY.coerceIn(-maxPanY, maxPanY)
    }

    private fun resetTransform() {
        zoom = 1f
        panX = 0f
        panY = 0f
        fitted = false
    }

    private fun updateIntercept() {
        parent?.requestDisallowInterceptTouchEvent(zoom > 1.01f || selectionDragging)
    }

    private fun findWordAt(viewX: Float, viewY: Float): Int? {
        val bmp = bitmap ?: return null
        if (bmp.width <= 0 || bmp.height <= 0 || words.isEmpty()) return null
        buildMatrix(bmp)
        matrix.invert(inverse)
        val point = floatArrayOf(viewX, viewY)
        inverse.mapPoints(point)
        val nx = point[0] / bmp.width
        val ny = point[1] / bmp.height
        if (nx < -0.05f || nx > 1.05f || ny < -0.05f || ny > 1.05f) return null

        val scale = baseScale * zoom
        val slop = (12f * resources.displayMetrics.density) / (bmp.width * scale)
        words.firstOrNull { it.rect.contains(nx, ny, slop) }?.let { return it.index }

        val nearest = words.minByOrNull { it.rect.distanceSquaredTo(nx, ny) } ?: return null
        val maxDistance = max(slop * 2.5f, 0.012f)
        return if (nearest.rect.distanceSquaredTo(nx, ny) <= maxDistance * maxDistance) {
            nearest.index
        } else {
            null
        }
    }
}
