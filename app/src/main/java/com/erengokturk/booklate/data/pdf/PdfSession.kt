package com.erengokturk.booklate.data.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import com.erengokturk.booklate.domain.model.PageText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import java.io.File
import java.io.StringWriter

data class PdfPageSize(val widthPt: Float, val heightPt: Float)

class PdfOpenException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Tek bir PDF oturumunu yönetir.
 *
 * - Görselleştirme: Android'in yerleşik PdfRenderer'ı (hızlı, donanım hızlandırmalı).
 * - Metin katmanı: Apache PDFBox (kelime koordinatları için).
 *
 * PdfRenderer ve PDDocument thread-safe değildir; bu yüzden ayrı mutex'ler kullanılır.
 */
class PdfSession private constructor(
    private val file: File,
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
) : AutoCloseable {

    private val renderMutex = Mutex()
    private val textMutex = Mutex()
    private val pageTextCache = object : LinkedHashMap<Int, PageText>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, PageText>?): Boolean =
            size > 12
    }
    private val bitmapCache: LruCache<String, Bitmap> =
        object : LruCache<String, Bitmap>(cacheSizeBytes()) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
        }

    private var textDocument: PDDocument? = null
    private var textDocumentTried = false
    private var closed = false

    val pageCount: Int get() = renderer.pageCount

    suspend fun pageSize(index: Int): PdfPageSize = renderMutex.withLock {
        withContext(Dispatchers.IO) {
            checkOpen()
            renderer.openPage(index).use { page ->
                PdfPageSize(page.width.toFloat(), page.height.toFloat())
            }
        }
    }

    suspend fun renderPage(index: Int, targetWidth: Int, quality: Float): Bitmap =
        renderMutex.withLock {
            withContext(Dispatchers.IO) {
                checkOpen()
                val width = (targetWidth * quality).toInt()
                    .coerceIn(720, 2400)
                val key = "$index@$width"
                bitmapCache.get(key)?.takeUnless { it.isRecycled }?.let { return@withContext it }
                renderer.openPage(index).use { page ->
                    var outWidth = width
                    var outHeight = (outWidth.toFloat() * page.height / page.width)
                        .toInt()
                        .coerceAtLeast(1)

                    // Aşırı büyük bitmap'leri engelle (ARGB_8888'de ~48 MB sınırı).
                    val maxPixels = 12_000_000L
                    val pixels = outWidth.toLong() * outHeight.toLong()
                    if (pixels > maxPixels) {
                        val factor = kotlin.math.sqrt(maxPixels.toDouble() / pixels).toFloat()
                        outWidth = (outWidth * factor).toInt().coerceAtLeast(1)
                        outHeight = (outHeight * factor).toInt().coerceAtLeast(1)
                    }

                    // PdfRenderer yalnızca ARGB_8888 hedef bitmap kabul eder; RGB_565
                    // "Unsupported pixel format" hatası verir.
                    val bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
                    // PDF'lerin çoğunda sayfa arka planı boyanmaz (şeffaf kalır).
                    // Beyaza boyamazsak koyu temada sayfa, uygulama arka planıyla kararır.
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmapCache.put(key, bitmap)
                    bitmap
                }
            }
        }

    suspend fun extractPage(index: Int, size: PdfPageSize): PageText {
        cachedPageText(index)?.let { return it }
        return textMutex.withLock {
            withContext(Dispatchers.IO) {
                checkOpen()
                cachedPageText(index)?.let { return@withContext it }
                val document = obtainTextDocument() ?: return@withContext PageText.EMPTY

                try {
                    val stripper = PdfWordStripper(
                        pageWidth = size.widthPt.coerceAtLeast(1f),
                        pageHeight = size.heightPt.coerceAtLeast(1f),
                    ).apply {
                        startPage = index + 1
                        endPage = index + 1
                    }
                    stripper.writeText(document, StringWriter())
                    val result = stripper.build()
                    synchronized(pageTextCache) { pageTextCache[index] = result }
                    result                } catch (error: Exception) {
                    PageText.EMPTY
                }
            }
        }
    }

    private fun cachedPageText(index: Int): PageText? =
        synchronized(pageTextCache) { pageTextCache[index] }

    private fun obtainTextDocument(): PDDocument? {
        if (textDocumentTried) return textDocument
        textDocumentTried = true
        textDocument = runCatching {
            PDDocument.load(file, com.tom_roush.pdfbox.io.MemoryUsageSetting.setupMainMemoryOnly())
        }.getOrNull()
        return textDocument
    }

    override fun close() {
        if (closed) return
        closed = true
        runCatching { textDocument?.close() }
        textDocument = null
        runCatching { renderer.close() }
        runCatching { descriptor.close() }
        bitmapCache.evictAll()
        synchronized(pageTextCache) { pageTextCache.clear() }
    }

    private fun checkOpen() {
        if (closed) throw PdfOpenException("PDF oturumu kapatıldı.")
    }

    companion object {
        suspend fun open(file: File): PdfSession = withContext(Dispatchers.IO) {
            if (!file.exists() || !file.canRead()) {
                throw PdfOpenException("PDF dosyası bulunamadı.")
            }
            try {
                val descriptor = ParcelFileDescriptor.open(
                    file,
                    ParcelFileDescriptor.MODE_READ_ONLY,
                )
                val renderer = PdfRenderer(descriptor)
                if (renderer.pageCount <= 0) {
                    runCatching { renderer.close() }
                    runCatching { descriptor.close() }
                    throw PdfOpenException("PDF içinde okunabilir sayfa bulunamadı.")
                }
                PdfSession(file, descriptor, renderer)
            } catch (error: PdfOpenException) {
                throw error
            } catch (error: Exception) {
                throw PdfOpenException(
                    "PDF açılamadı. Şifreli veya bozuk bir dosya olabilir.",
                    error,
                )
            }
        }

        fun cropBoxOrDefault(page: com.tom_roush.pdfbox.pdmodel.PDPage): PDRectangle =
            page.cropBox ?: PDRectangle.A4

        private fun cacheSizeBytes(): Int {
            val maxHeapBytes = Runtime.getRuntime().maxMemory()
            val quarter = (maxHeapBytes / 4).coerceAtMost(96L * 1024 * 1024)
            return quarter.coerceAtLeast(16L * 1024 * 1024).toInt()
        }
    }
}
