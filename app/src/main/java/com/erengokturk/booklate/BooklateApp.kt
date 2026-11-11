package com.erengokturk.booklate

import android.app.Application
import com.erengokturk.booklate.di.AppContainer
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class BooklateApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // PDFBox'un font ve kaynak dosyalarını Android varlıklarından okuyabilmesi için.
        PDFBoxResourceLoader.init(this)
        container = AppContainer(this)
    }
}
