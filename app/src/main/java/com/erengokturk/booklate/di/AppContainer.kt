package com.erengokturk.booklate.di

import android.content.Context
import com.erengokturk.booklate.data.analysis.GeminiClient
import com.erengokturk.booklate.data.analysis.WordAnalysisRepository
import com.erengokturk.booklate.data.library.LibraryRepository
import com.erengokturk.booklate.data.local.BooklateDatabase
import com.erengokturk.booklate.data.local.SettingsRepository
import com.erengokturk.booklate.tts.TtsManager
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Küçük ölçekli, çerçevesiz bağımlılık kapsayıcısı.
 * Hilt/Koin gibi ek kod üretimi gerektirmez; sınıflar test edilirken kolayca değiştirilebilir.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: BooklateDatabase by lazy { BooklateDatabase.build(appContext) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(75, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val geminiClient: GeminiClient by lazy { GeminiClient(okHttpClient) }

    val libraryRepository: LibraryRepository by lazy {
        LibraryRepository(
            context = appContext,
            bookDao = database.bookDao(),
        )
    }

    val wordAnalysisRepository: WordAnalysisRepository by lazy {
        WordAnalysisRepository(
            wordLookupDao = database.wordLookupDao(),
            geminiClient = geminiClient,
            settingsRepository = settingsRepository,
        )
    }

    val ttsManager: TtsManager by lazy { TtsManager(appContext) }

    fun shutdown() {
        ttsManager.shutdown()
    }
}
