package com.erengokturk.booklate.data.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.erengokturk.booklate.data.local.BookDao
import com.erengokturk.booklate.data.local.BookEntity
import com.erengokturk.booklate.domain.model.Book
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class BookImportException(message: String, cause: Throwable? = null) : Exception(message, cause)

class LibraryRepository(
    private val context: Context,
    private val bookDao: BookDao,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val booksDir: File
        get() = File(context.filesDir, "books").apply { mkdirs() }
    private val coversDir: File
        get() = File(context.filesDir, "covers").apply { mkdirs() }

    val books: Flow<List<Book>> = bookDao.observeAll().map { entities ->
        entities.map { it.toDomain() }
    }

    fun observeBook(id: String): Flow<Book?> =
        bookDao.observeById(id).map { it?.toDomain() }

    suspend fun getBook(id: String): Book? = withContext(io) {
        bookDao.getById(id)?.toDomain()
    }

    suspend fun importBook(
        uri: Uri,
        languageCode: String = "ru",
        onProgress: (Float) -> Unit = {},
    ): Book = withContext(io) {
        val resolver = context.contentResolver
        val source = uri.toString()

        bookDao.findBySourceUri(source)?.let { existing ->
            if (File(existing.filePath).exists()) {
                // Aynı dosya yeniden seçildiyse dil seçimini de güncelle.
                bookDao.updateLanguage(existing.id, languageCode)
                bookDao.touchOpened(existing.id, System.currentTimeMillis())
                return@withContext existing.copy(languageCode = languageCode).toDomain()
            }
            // Dosya silinmiş; eski kaydı temizleyip yeniden içe aktar.
            bookDao.delete(existing.id)
            File(existing.filePath).delete()
            existing.coverPath?.let { runCatching { File(it).delete() } }
        }

        val (displayName, size) = queryDocumentMeta(uri)
        val safeName = displayName.ifBlank { "Kitap.pdf" }
        val id = UUID.randomUUID().toString()
        val destination = File(booksDir, "$id.pdf")
        val coverFile = File(coversDir, "$id.jpg")

        try {
            onProgress(0f)
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(128 * 1024)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        if (size > 0) {
                            onProgress((copied.toFloat() / size).coerceIn(0f, 0.98f))
                        }
                    }
                    output.flush()
                }
            } ?: throw BookImportException("Dosya okunamadı. Lütfen başka bir PDF seçin.")

            if (!isPdf(destination)) {
                throw BookImportException("Seçilen dosya geçerli bir PDF değil.")
            }

            val pageCount = renderCover(destination, coverFile)
            val (title, author) = readMetadata(destination)
            val fallbackTitle = fallbackTitle(safeName)
            val now = System.currentTimeMillis()

            val book = Book(
                id = id,
                title = title ?: fallbackTitle,
                author = author,
                filePath = destination.absolutePath,
                sourceUri = source,
                fileName = safeName,
                pageCount = pageCount,
                lastPage = 0,
                lastPageOffset = 0f,
                coverPath = coverFile.takeIf { it.exists() }?.absolutePath,
                fileSize = destination.length(),
                createdAt = now,
                lastOpenedAt = now,
                languageCode = languageCode,
            )
            bookDao.upsert(BookEntity.fromDomain(book))
            onProgress(1f)
            book
        } catch (error: Exception) {
            runCatching { destination.delete() }
            runCatching { coverFile.delete() }
            if (error is BookImportException) throw error
            throw BookImportException(
                "Kitap içe aktarılamadı: ${error.message ?: "bilinmeyen hata"}",
                error,
            )
        }
    }

    suspend fun updateProgress(bookId: String, page: Int, offset: Float) = withContext(io) {
        bookDao.updateProgress(
            id = bookId,
            page = page.coerceAtLeast(0),
            offset = offset.coerceIn(0f, 1f),
            openedAt = System.currentTimeMillis(),
        )
    }

    suspend fun touchOpened(bookId: String) = withContext(io) {
        bookDao.touchOpened(bookId, System.currentTimeMillis())
    }

    /** Kitabın okunduğu/analiz edildiği dili günceller. */
    suspend fun updateLanguage(bookId: String, languageCode: String) = withContext(io) {
        bookDao.updateLanguage(bookId, languageCode)
    }

    suspend fun deleteBook(book: Book) = withContext(io) {
        runCatching { File(book.filePath).delete() }
        book.coverPath?.let { runCatching { File(it).delete() } }
        bookDao.delete(book.id)
    }

    suspend fun localStorageBytes(): Long = withContext(io) {
        listOf(booksDir, coversDir).sumOf { dir ->
            dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
    }

    private fun queryDocumentMeta(uri: Uri): Pair<String, Long> {
        var name = ""
        var size = -1L
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0) name = cursor.getString(nameIndex).orEmpty()
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                }
            }
        }
        if (name.isBlank()) {
            name = uri.lastPathSegment?.substringAfterLast('/') ?: "Kitap.pdf"
        }
        return name to size
    }

    private fun isPdf(file: File): Boolean {
        if (!file.exists() || file.length() < 5) return false
        return runCatching {
            file.inputStream().use { stream ->
                val header = ByteArray(5)
                stream.read(header)
                String(header, Charsets.US_ASCII) == "%PDF-"
            }
        }.getOrDefault(false)
    }

    private fun renderCover(pdf: File, coverFile: File): Int {
        var descriptor: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            descriptor = ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(descriptor)
            val pageCount = renderer.pageCount
            if (pageCount <= 0) throw BookImportException("PDF içinde sayfa bulunamadı.")

            renderer.openPage(0).use { page ->
                val width = 480
                val height = (width.toFloat() * page.height / page.width)
                    .toInt()
                    .coerceIn(100, 900)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                FileOutputStream(coverFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }
                bitmap.recycle()
            }
            return pageCount
        } finally {
            runCatching { renderer?.close() }
            runCatching { descriptor?.close() }
        }
    }

    private fun readMetadata(pdf: File): Pair<String?, String?> = runCatching {
        PDDocument.load(pdf).use { document ->
            val info = document.documentInformation
            val rawTitle = info?.title?.trim().orEmpty()
            val rawAuthor = info?.author?.trim().orEmpty()
            val title = rawTitle
                .takeIf { it.isNotBlank() && it.length in 2..180 && !it.equals("untitled", true) }
            val author = rawAuthor.takeIf { it.isNotBlank() && it.length in 2..120 }
            title to author
        }
    }.getOrDefault(null to null)

    private fun fallbackTitle(fileName: String): String {
        val base = fileName.substringBeforeLast('.').ifBlank { "Adsız kitap" }
        val cleaned = base.replace('_', ' ').replace('-', ' ').replace(Regex("\\s+"), " ").trim()
        return cleaned.ifBlank { "Adsız kitap" }
            .take(180)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
