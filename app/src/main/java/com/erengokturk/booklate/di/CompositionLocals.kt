package com.erengokturk.booklate.di

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer sağlanmadı. MainActivity içindeki CompositionLocalProvider'ı kontrol edin.")
}
