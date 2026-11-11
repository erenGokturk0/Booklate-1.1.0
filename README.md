# Booklate 1.1.0

**Oku, dokun, öğren.**

[![Son sürüm](https://img.shields.io/github/v/release/erenGokturk0/Booklate-1.1.0?label=son%20s%C3%BCr%C3%BCm&color=2f855a)](https://github.com/erenGokturk0/Booklate-1.1.0/releases/latest)
[![Lisans](https://img.shields.io/badge/lisans-Apache%202.0-blue)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3ddc84)](https://developer.android.com)

### 📲 İndir

**➡️ [Booklate1_1_0.apk](https://github.com/erenGokturk0/Booklate-1.1.0/releases/download/v1.1.0/Booklate1_1_0.apk)**

| | |
|---|---|
| Sürüm | **1.1.0** (versionCode 5) |
| Dosya | `Booklate1_1_0.apk` (~9,5 MB) |
| Paket adı | `com.erengokturk.booklate` |
| Gereksinim | Android 8.0 (API 26) ve üzeri |
| İmza | Kendinden imzalı release anahtarı (SHA-256 parmak izi aşağıda) |

<details>
<summary>İmza parmak izi (SHA-256)</summary>

```text
99:0E:25:99:52:4D:11:3E:E1:1C:7B:A0:87:05:95:34:16:5A:6E:5B:0B:AA:20:6F:3D:5D:97:52:A3:C8:DE:64
```

</details>

**Kurulum**

1. APK'yı indir.
2. Android'in “Bilinmeyen kaynaklardan yükleme” iznini bu tarayıcı/dosya yöneticisi için aç.
3. `Booklate1_1_0.apk` dosyasına dokunup kur.
4. Uygulamayı aç → **Ayarlar → Gemini API anahtarı** alanına kendi anahtarını gir. (Anahtar cihazda kalır, sunucuya gönderilmez; yalnızca Google Gemini API'sine gider.)

Booklate, Android üzerinde PDF kitap okuyup bilmediğin bir kelimeye dokunduğunda o kelimeyi **içinde geçtiği cümleyle birlikte** yapay zekâya analiz ettiren, tamamen cihaz üzerinde çalışan bir dil öğrenme uygulamasıdır.

- PDF'i okur, sayfaları akıcı biçimde kaydırır.
- Kelimeye dokununca bağlamsal çeviri, sözlük anlamı, dil bilgisi ve ek çözümlemesi gösterir.
- Cihazın yerleşik TTS motoruyla sayfayı/cümleyi okur, kelimeleri eşzamanlı vurgular.
- Tüm kitaplar, ayarlar, kelime geçmişi ve önbellek cihazda kalır.
- Sunucu, reklam, analitik veya zorunlu üyelik yoktur.

> **Kitap dili her kitap için ayrıdır:** PDF eklerken seçilir, sonradan kütüphanedeki kitabın `⋮` menüsünden “Dili değiştir” ile güncellenebilir. Varsayılan olarak Rusça önerilir.
>
> **Ana dil** (açıklama dili) Ayarlar'dan seçilir ve tüm çeviri, dil bilgisi ve örnek çevirileri o dilde üretilir. Varsayılan ana dil Türkçe'dir.

Desteklenen diller (hem kitap hem ana dil olarak): Türkçe, İngilizce, Rusça, İspanyolca, İtalyanca, Almanca, Fransızca, Japonca, Azerbaycan Dili, Kazakça, Türkmence, Özbekçe, Kırgızca, Çince, Lehçe.

---

## 1. Özellikler

### Okuma
- Android'in yerleşik `PdfRenderer`'ı ile hızlı sayfa çizimi.
- `HorizontalPager` ile yatay sayfa geçişi, sayfa kaydırıcısı ve ileri/geri düğmeleri.
- Pinch-zoom, çift dokunma ile yakınlaştırma, parmakla kaydırma.
- Son okunan sayfayı ve okuma ilerlemesini otomatik kaydetme.
- PDF'i uygulama özel dizinine kopyalayarak kalıcı erişim (SAF izni kaybolsa bile kitap açılır).
- İlk sayfadan otomatik kapak görseli.

### Kelime analizi
- Tek kelimeye dokunma → otomatik analiz.
- Uzun basıp sürükleyerek çoklu kelime seçimi → "Çevir" araç çubuğu.
- Gemini'den yapılandırılmış JSON yanıtı:
  - bağlamsal Türkçe çeviri,
  - sözlük anlamı,
  - telaffuz/transkripsiyon,
  - sözcük türü, cinsiyet, tekil/çoğul, hâl, canlılık,
  - fiil görünüşü, zaman, kişi,
  - sözlük biçimi (lemma), kök, ekler ve eklerin işlevi,
  - dil bilgisi notu, örnek cümle ve çevirisi,
  - CEFR seviyesi, eş anlamlılar, eşdizimler.
- Kelime bazlı yerel önbellek: aynı kelime tekrar sorgulandığında Gemini kotası harcanmaz.
- Önbellekteki cümle bağlamı farklıysa arayüz uyarır ve "bu bağlam için yeniden analiz et" seçeneği sunar.
- Favorilere ekleme ve kelime geçmişi.

### Sesli okuma
- Cihazın yerleşik `TextToSpeech` motoru.
- Sayfa veya seçili cümle okuma.
- Duraklat/devam et.
- `onRangeStart` destekleyen motorlarda kelime vurgusu; desteklemeyenlerde cümle vurgusu.
- Hız, perde ve ses seçimi.

### Ayarlar
- Gemini API anahtarı, model seçimi ve bağlantı testi.
- Ana dil (açıklama dili) seçimi; 15 dil.
- TTS hızı, perdesi, sesi ve "Sesi dene".
- Tema: sistem / açık / koyu.
- Sayfa çözünürlüğü, ekranı açık tutma, dokunma ipuçları.
- Önbellek istatistikleri ve temizleme.
- Gizlilik ve hakkında bölümü.

---

## 2. Teknoloji Yığını

| Katman | Teknoloji |
|---|---|
| Dil | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 |
| Mimari | MVVM, tek `:app` modülü, manuel DI |
| Navigasyon | Navigation Compose 2.8.5 |
| PDF görüntüleme | Android `PdfRenderer` |
| PDF metin katmanı | `com.tom-roush:pdfbox-android:2.0.27.0` (Apache 2.0) |
| Yapay zekâ | Gemini REST API (`generativelanguage.googleapis.com`) |
| HTTP | OkHttp 4.12.0 |
| JSON | kotlinx.serialization 1.7.3 |
| Yerel veritabanı | Room 2.6.1 + KSP |
| Ayarlar | DataStore Preferences 1.1.1 |
| Görsel yükleme | Coil 2.7.0 |
| TTS | Android `TextToSpeech` |
| Build | AGP 8.7.3, Gradle 8.9, JDK 17 hedefi (JBR 21 ile derlenebilir) |
| SDK | compileSdk/targetSdk 35, minSdk 26 |

---

## 3. Gereksinimler

- **Android Studio** (Ladybug veya daha yeni önerilir).
- **Android SDK Platform 35** ve Build Tools.
- **JDK 17+**. Windows'ta Android Studio'nun gömülü JDK'sı (JBR 21) sorunsuz çalışır.
- **Android 8.0 (API 26)** veya üzeri bir cihaz/emülatör.
- Gemini analizi için **internet** ve **ücretsiz Gemini API anahtarı**.
- Sesli okuma için cihazda yüklü bir **TTS motoru** ve ilgili dil ses paketi.

---

## 4. Kurulum (adım adım)

### 4.1. Projeyi açma

1. Bu klasörü Android Studio ile aç: **File → Open**.
2. Gradle senkronizasyonunun bitmesini bekle.
3. `local.properties` dosyasında SDK yolunun doğru olduğundan emin ol:

```properties
sdk.dir=C\:\\Users\\<kullanıcı>\\AppData\\Local\\Android\\Sdk
```

> `local.properties` makineye özeldir ve `.gitignore` içindedir; paylaşma.

### 4.2. Gemini API anahtarı

İki yöntem vardır:

**A) Uygulama içinden (önerilen)**

1. [Google AI Studio](https://aistudio.google.com/app/apikey) adresine git.
2. Google hesabınla giriş yap.
3. **Create API key** ile ücretsiz bir anahtar oluştur (`AIza...`).
4. Uygulamada **Ayarlar → Gemini yapay zekâ** bölümüne anahtarı yapıştır.
5. **Kaydet** → **Bağlantıyı test et**.

Anahtar cihazdaki DataStore'da saklanır ve yalnızca doğrudan Google'a gönderilir.

**B) Geliştirici varsayılanı (isteğe bağlı)**

`local.properties` dosyasına ekle:

```properties
GEMINI_API_KEY=AIza...
```

`app/build.gradle.kts` bunu `BuildConfig.GEMINI_API_KEY` olarak üretir. Uygulama içinde kayıtlı bir anahtar varsa o kullanılır; yoksa bu değer devreye girer.

> **Güvenlik notu:** İstemci tarafında API anahtarı taşımak, dağıtılan bir APK'da anahtarın çıkarılabilmesi anlamına gelir. Bu proje kişisel/eğitim kullanımı için tasarlandı. Herkese açık bir uygulama yayınlayacaksan anahtarı bir sunucu/proxy arkasına al veya kullanıcıdan anahtarını girmesini iste. `local.properties` ve gerçek anahtarlar repoya eklenmemelidir.

### 4.3. Derleme

PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug
```

Çıktı:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Kurulum:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Statik analiz (lint):

```powershell
.\gradlew.bat :app:lintDebug
```

> HTML raporu: `app\build\reports\lint-results-debug.html`. Projede hata seviyesinde lint bulgusu yoktur; raporda yalnızca bağımlılık güncelleme önerileri gibi bilgilendirici uyarılar görülebilir.

> Debug sürümünün paket adı `com.erengokturk.booklate.debug`, release sürümününki `com.erengokturk.booklate` olur.

#### Release APK (imzalı)

Release derlemesi R8 ile küçültülür, kaynakları budanır ve imzalanır:

```powershell
.\gradlew.bat :app:assembleRelease
```

```text
app\build\outputs\apk\release\app-release.apk
```

İmza bilgileri `keystore.properties` dosyasından okunur:

```properties
storeFile=keystore/booklate-release.jks
storePassword=...
keyAlias=booklate
keyPassword=...
```

`app/build.gradle.kts` bu dosyayı `signingConfigs.release` için kullanır. Dosya veya keystore yoksa release imzasız kalır ve `assembleRelease` "missing required property storeFile" hatası verir.

> ⚠️ `keystore/booklate-release.jks` ve `keystore.properties` dosyalarını yedekle. Aynı anahtarla imzalanmayan güncellemeler cihaza kurulamaz (imza çakışması). Bu dosyaları kimseyle paylaşma; `.gitignore` içinde hariç tutulurlar.

> Debug ve release farklı paket adları kullandığı için ikisi cihazda yan yana kurulabilir, ancak veriler (kitap, ayar, geçmiş) aralarında paylaşılmaz.

### 4.4. Windows'ta Türkçe karakterli proje yolu

Proje yolu `Varsayılan Proje` gibi ASCII dışı karakter içeriyorsa AGP uyarır. Bu projede `gradle.properties` içine şu satır eklendi:

```properties
android.overridePathCheck=true
```

Uygulama derlemesi bu ayarla çalışır. Ancak **birim testleri** Windows'ta JDK'nın `@argfile` kodlaması nedeniyle `ClassNotFoundException` verebilir. Testleri çalıştırmak için projeyi ASCII bir yola kopyala:

```powershell
robocopy "C:\Users\<kullanıcı>\Documents\Varsayılan Proje" C:\booklate-build /E /XD build .gradle .idea
cd C:\booklate-build
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDebugUnitTest
```

---

## 5. Kullanım

### Kitap ekleme
1. **Kütüphane** sekmesinde **PDF ekle**'ye bas.
2. Dosya seçiciden PDF'i seç.
3. Açılan **“Kitap hangi dilde?”** penceresinden kitabın dilini seç. Seçim yalnızca bu kitap için geçerlidir.
4. PDF cihazdaki uygulama dizinine kopyalanır, ilk sayfa kapak olarak hazırlanır.
5. Kitap kartına dokunarak oku.

> Kitap dilini sonradan değiştirmek için kütüphanede kitap kapağının sağ üstündeki `⋮` → **Dili değiştir**. Yeni analizler ve seslendirme seçilen dile göre yapılır; eski önbellek sonuçları silinmez.

> Taranmış (görüntü tabanlı) PDF'lerde metin katmanı yoktur. Bu sayfalarda "Metin katmanı yok · taranmış PDF olabilir" uyarısı görünür; kelime seçimi ve TTS çalışmaz. OCR yol haritasındadır.

### Kelime sorgulama
- **Tek dokunma:** kelime seçilir ve analiz otomatik başlar.
- **Uzun bas + sürükle:** birden fazla kelime seç; alttaki **Çevir** ile analiz et.
- Analiz alt sayfasında:
  - önbellekten mi geldiği,
  - bağlamın eşleşip eşleşmediği,
  - favori, yenileme, kelime/cümle dinleme,
  - dil bilgisi ve ek kartları görünür.

### Sesli okuma
- Alt kontroldeki **Sayfayı oku** düğmesi tüm sayfayı sırayla okur.
- Konuşma sırasında **Duraklat**, ardından **Devam et**.
- **Durdur** ile tamamen biter.
- Seçili kelime/cümle için analiz sayfasındaki hoparlör simgelerini kullan.

### Geçmiş
- **Kelimeler** sekmesinde tüm sorgular listelenir.
- Arama, favoriler filtresi, detay ve silme vardır.
- Önbellek temizlenirse favoriler de silinir; kitaplar ve okuma ilerlemesi korunur.

---

## 6. Klasör Yapısı

```text
Booklate/
├─ build.gradle.kts                 # Eklenti sürümleri
├─ settings.gradle.kts              # :app modülü ve depolar
├─ gradle.properties                # JVM, AndroidX, path check
├─ local.properties                 # SDK yolu + (isteğe bağlı) Gemini anahtarı
├─ gradlew / gradlew.bat            # Gradle wrapper 8.9
└─ app/
   ├─ build.gradle.kts              # Bağımlılıklar, BuildConfig, imzalama
   ├─ proguard-rules.pro
   └─ src/
      ├─ main/
      │  ├─ AndroidManifest.xml
      │  ├─ res/                    # Tema, renkler, launcher ikonu
      │  └─ java/com/erengokturk/booklate/
      │     ├─ BooklateApp.kt       # Application, PDFBox init, container
      │     ├─ MainActivity.kt      # Compose girişi, tema, DI
      │     ├─ core/
      │     │  ├─ TextUtils.kt      # Normalizasyon, cache key, cümle sonu
      │     │  ├─ PageTextBuilder.kt# Kelimeleri cümlelere böler
      │     │  └─ Formatters.kt     # Tarih, boyut, hız biçimleri
      │     ├─ domain/model/
      │     │  ├─ Book.kt
      │     │  ├─ PageText.kt       # WordRect, PageWord, PageSentence
      │     │  ├─ WordAnalysis.kt   # Gemini yanıt modeli
      │     │  ├─ AnalysisModels.kt # İstek/sonuç/kayıt modelleri
      │     │  └─ UserSettings.kt
      │     ├─ data/
      │     │  ├─ local/            # Room entity, DAO, DB, DataStore
      │     │  ├─ library/          # PDF import, kapak, ilerleme
      │     │  ├─ pdf/              # PdfSession + PdfWordStripper
      │     │  └─ analysis/         # GeminiClient, prompt, cache repo
      │     ├─ tts/                 # TtsManager + modeller
      │     ├─ di/                  # AppContainer, CompositionLocal
      │     └─ ui/
      │        ├─ BooklateNavRoot.kt
      │        ├─ navigation/Routes.kt
      │        ├─ theme/
      │        ├─ components/Common.kt
      │        ├─ library/          # Kütüphane ekranı + VM
      │        ├─ reader/           # Reader ekranı, VM, PdfPageView
      │        ├─ analysis/         # Analiz alt sayfası
      │        ├─ history/          # Kelime geçmişi
      │        └─ settings/         # Ayarlar + VM
      └─ test/java/...              # Birim testleri
```

---

## 7. Mimari ve Veri Akışı

### 7.1. Manuel DI

Hilt/Koin yok. `AppContainer` uygulama ömrü boyunca tek örnekleri tutar:

```kotlin
class AppContainer(context: Context) {
    val database by lazy { BooklateDatabase.build(context) }
    val settingsRepository by lazy { SettingsRepository(context) }
    val geminiClient by lazy { GeminiClient(okHttpClient) }

    val libraryRepository by lazy {
        LibraryRepository(context, database.bookDao())
    }

    val wordAnalysisRepository by lazy {
        WordAnalysisRepository(
            wordLookupDao = database.wordLookupDao(),
            geminiClient = geminiClient,
            settingsRepository = settingsRepository,
        )
    }

    val ttsManager by lazy { TtsManager(context) }
}
```

`MainActivity` bunu `CompositionLocalProvider(LocalAppContainer provides container)` ile Compose ağacına verir. ViewModel'ler `viewModelFactory { initializer { ... } }` ile oluşturulur.

### 7.2. Okuma akışı

```text
LibraryScreen
  └─ LibraryViewModel.importPdf(uri)
       └─ LibraryRepository.importBook()
            ├─ PDF'i filesDir/books/<uuid>.pdf'e kopyala
            ├─ PdfRenderer ile sayfa sayısı + kapak
            ├─ PDFBox ile başlık/yazar meta verisi
            └─ Room'a BookEntity yaz

ReaderScreen
  └─ ReaderViewModel.loadPage(page, width)
       ├─ PdfSession.renderPage()   → Bitmap (LruCache)
       ├─ PdfSession.extractPage()  → PageText (kelime koordinatları)
       └─ ReaderUiState.pages[page]

Kelimeye dokunma
  └─ ReaderViewModel.analyze()
       └─ WordAnalysisRepository.analyze()
            ├─ Room cache lookup
            ├─ yoksa GeminiClient.analyze()
            └─ Room'a upsert + AnalysisOutcome
```

### 7.3. PDF metin katmanı

İki ayrı motor kullanılır:

1. **Görüntü:** `android.graphics.pdf.PdfRenderer`
   - Hızlı, donanım hızlandırmalı.
   - İstenen genişlik × kalite ile bitmap üretir.
   - `LruCache` ile bellek sınırlı önbellek.
2. **Metin:** PDFBox `PDFTextStripper`
   - Her `TextPosition`'dan kelime kutuları üretir.
   - Koordinatlar sayfa genişliğine/yüksekliğine bölünerek **0..1 aralığına normalize** edilir.
   - Böylece bitmap çözünürlüğünden bağımsız vurgulama yapılır.
   - `PageTextBuilder` kelimeleri noktalama işaretlerine göre cümlelere böler.

Önemli sınıflar:

- `data/pdf/PdfSession.kt`: render + metin çıkarma + önbellek.
- `data/pdf/PdfWordStripper.kt`: `TextPosition` → `PageWord`.
- `core/PageTextBuilder.kt`: kelime listesi → cümleler.
- `ui/reader/PdfPageView.kt`: zoom/pan, dokunma, çoklu seçim, vurgulama.

---

## 8. Gemini Entegrasyonu

### 8.1. İstek

`GeminiClient.analyze()` şu gövdeyi `POST` eder:

```text
POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent
x-goog-api-key: <API_KEY>
Content-Type: application/json
```

Gövde:

```json
{
  "systemInstruction": {
    "parts": [{ "text": "Sen Booklate adlı ..." }]
  },
  "contents": [{
    "role": "user",
    "parts": [{ "text": "HEDEF DİL: Rusça ... BAĞLAM CÜMLESİ: ..." }]
  }],
  "generationConfig": {
    "temperature": 0.2,
    "topP": 0.9,
    "maxOutputTokens": 1800,
    "responseMimeType": "application/json",
    "responseSchema": { "type": "OBJECT", "properties": { ... } }
  }
}
```

Avantajları:
- `responseMimeType = application/json` + `responseSchema` ile modelin serbest metin yerine şemaya uygun JSON üretmesi sağlanır.
- `temperature = 0.2` ile tutarlılık artırılır.
- Yanıt yine de `GeminiClient.parseAnalysis()` içinde savunmacı biçimde ayrıştırılır; bozuk JSON uygulamayı çökertmez.

### 8.2. Kotlin kullanımı

```kotlin
val outcome = container.wordAnalysisRepository.analyze(
    AnalysisRequest(
        word = "книгу",
        sentence = "Я читаю интересную книгу.",
        targetLanguage = "ru",
        nativeLanguage = "tr",
        bookTitle = "Savaş ve Barış",
        pageNumber = 42,
    ),
)

val analysis = outcome.analysis
println(analysis.contextualTranslation) // "kitabı"
println(analysis.grammaticalCase)       // "Akuzatif"
println(analysis.affixes)               // [-у: çekim eki, belirtme durumu]
```

### 8.3. Örnek model yanıtı

```json
{
  "word": "книгу",
  "contextualTranslation": "kitabı",
  "dictionaryMeaning": "kitap",
  "transcription": "кни́гу",
  "partOfSpeech": "isim",
  "gender": "dişil",
  "number": "tekil",
  "case": "Akuzatif",
  "animacy": "cansız",
  "verbAspect": "-",
  "tense": "-",
  "person": "-",
  "lemma": "книга",
  "root": "книг",
  "affixes": [
    {
      "affix": "-у",
      "type": "çekim eki",
      "function": "belirtme durumu (akuzatif), dişil tekil"
    }
  ],
  "grammarNotes": "Fiilin nesnesi olduğu için belirtme durumunda kullanılmış.",
  "example": "Он купил новую книгу.",
  "exampleTranslation": "Yeni bir kitap aldı.",
  "cefrLevel": "A2",
  "synonyms": ["том", "издание"],
  "collocations": ["читать книгу (kitap okumak)"]
}
```

### 8.4. Önbellek stratejisi

Anahtar:

```text
SHA-256("booklate|v2|p4|" + normalize(word) + "|" + kitapDili + "|" + anaDil)
```

- `v2`: `Constants.CACHE_SCHEMA_VERSION`
- `p4`: `Constants.PROMPT_VERSION`
- Kelime bazlıdır; aynı kelime farklı cümlede geçse bile aynı kayıt kullanılır.
- Kayıttaki cümle, şu anki cümleyle eşleşmiyorsa `contextMatches = false` döner. Arayüz bunu gösterir ve kullanıcı isterse **kota harcayarak** yeniden analiz eder.
- `hitCount` ve `lastAccessedAt` güncellenir.
- İstem/şema sürümü değişince anahtar değişir; eski kayıtlar yeni sonuçlarla karışmaz.

Bu tasarım kota dostudur; ancak bağlam değişince çeviri eski bağlama ait olabilir. Bu yüzden uyarı ve yenileme akışı zorunludur.

---

## 9. Veri Modeli

### `books`
| Alan | Açıklama |
|---|---|
| `id` | UUID |
| `title`, `author` | PDF meta verisi veya dosya adı |
| `filePath` | Uygulama özel dizinindeki PDF |
| `sourceUri` | İçe aktarılan orijinal URI (metadata) |
| `fileName`, `fileSize` | Dosya bilgisi |
| `pageCount` | Sayfa sayısı |
| `lastPage`, `lastPageOffset` | Okuma konumu |
| `coverPath` | Kapak JPEG yolu |
| `createdAt`, `lastOpenedAt` | Zaman damgaları |
| `languageCode` | Kitabın dili (her kitap için ayrı; `ru`, `en`, `ja`...) |

> Veritabanı sürümü **2**'dir. `MIGRATION_1_2`, `books` tablosuna `languageCode TEXT NOT NULL DEFAULT 'ru'` sütununu ekler; mevcut kitaplar Rusça kabul edilir ve kullanıcı `⋮` menüsünden değiştirebilir.

### `word_lookups`
| Alan | Açıklama |
|---|---|
| `cacheKey` | SHA-256 birincil anahtar |
| `word`, `normalizedWord` | Görünen ve normalize kelime |
| `translation` | Hızlı arama için bağlamsal çeviri |
| `targetLanguage`, `nativeLanguage` | Dil çifti |
| `sentence` | Analiz sırasındaki bağlam |
| `analysisJson` | `WordAnalysis` JSON'u |
| `bookTitle` | Kaynak kitap |
| `createdAt`, `lastAccessedAt`, `hitCount` | Kullanım istatistikleri |
| `isFavorite` | Favori |

### DataStore anahtarları
`gemini_api_key`, `gemini_model`, `target_language` (yeni kitaplar için önerilen dil), `native_language` (ana dil), `tts_rate`, `tts_pitch`, `tts_voice`, `theme_mode`, `keep_screen_on`, `render_quality`, `show_page_hints`.

---

## 10. Testler

Mevcut birim testleri (27 test):

- `TextUtilsTest`
  - cache key kararlılığı ve dil duyarlılığı,
  - prompt/şema sürümü değişimi,
  - cümle sonu algılama,
  - bağlam kısaltma,
  - baş harfler,
  - kullanımdan kalkan Gemini modellerinin güncel modellere eşlenmesi,
  - 15 dilli katalog ve dil kodu çözümleme.
- `PageTextBuilderTest`
  - cümlelere bölme,
  - noktalamanın önceki kelimeye eklenmesi,
  - kelime bütçesiyle bölme,
  - boş girdi,
  - karakter aralığı tutarlılığı.
- `TtsTextBuilderTest`
  - kelime karakter aralıklarının doğruluğu,
  - tek cümle üretimi,
  - boş sayfa.
- `GeminiClientTest`
  - düz JSON ve markdown çitli JSON ayıklama,
  - `case` alanının `grammaticalCase`'e eşlenmesi,
  - ekler, CEFR, eş anlamlılar,
  - bozuk/boş yanıt reddi.
- `GeminiPromptTest`
  - ana dilin isteme yazılması,
  - kitap dilinin hedef dil olarak kullanılması,
  - dile özgü olmayan dil bilgisi alanlarının opsiyonel işaretlenmesi.

Çalıştırma:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDebugUnitTest
```

> Bu ortamda uygulama derlemesi (`assembleDebug`), lint kontrolü (`lintDebug`) ve 27 birim testi başarıyla çalıştırıldı. Fiziksel cihaz/emülatör bağlı olmadığı için cihaz üstü davranışlar (TTS sesleri, Gemini canlı çağrısı, koyu tema sayfa görünümü) burada otomatik doğrulanmadı.

---

## 11. Sorun Giderme

| Belirti | Olası neden / çözüm |
|---|---|
| `Your project path contains non-ASCII characters` | `gradle.properties` içindeki `android.overridePathCheck=true` satırı duruyor mu? |
| Birim testleri `ClassNotFoundException` | Windows'ta Türkçe karakterli yol + JDK argfile sorunu. Projeyi ASCII yola kopyalayıp test et. |
| `Gemini API anahtarı ayarlanmamış` | Ayarlar → Gemini bölümüne anahtar gir ve kaydet. |
| `API anahtarı geçersiz` (401/403) | Anahtarı yeniden oluştur; Gemini API'nin projede etkin olduğundan emin ol. |
| `Ücretsiz Gemini kotası doldu` (429) | Günlük ücretsiz limit doldu. Bir süre bekle veya daha hafif model seç. |
| `Seçilen Gemini modeli bulunamadı` (404) | Eski modeller yeni API anahtarlarında kullanılamaz. Ayarlardan `gemini-3.5-flash-lite` / `gemini-3.6-flash` / `gemini-3.8-flash` seç. Uygulama eski model kaydını otomatik günceller. |
| PDF açılmıyor | Şifreli/DRM'li veya bozuk olabilir. Farklı bir PDF dene. |
| Kelime seçilmiyor / TTS susuyor | Taranmış PDF olabilir; metin katmanı yoktur. OCR gerekir. |
| TTS "ses paketi bulunamadı" | Android Ayarlar → Diller → Metin okuma çıktısı → ilgili dil ses paketini yükle. |
| Kelime vurgusu yok, sadece cümle vurgusu var | Cihazın TTS motoru `onRangeStart` desteklemiyor. Farklı bir motor/ses dene. |
| Sayfalar geç açılıyor | `render_quality` değerini düşür; büyük PDF'lerde bellek baskısı olabilir. |
| Önbellek yanlış bağlam gösteriyor | Uyarıdaki **Bu bağlam için yeniden analiz et** seçeneğini kullan (kota harcar). |
| API anahtarı kayboldu | Uygulama verileri temizlenmiş olabilir. Anahtarı yeniden gir. |

---

## 12. Sınırlamalar

- **OCR yok.** Taranmış/görüntü tabanlı PDF'lerde metin katmanı bulunmaz.
- **PDF metin kalitesi:** Bazı PDF'lerde ligatürler, çok sütunlu düzenler veya döndürülmüş sayfalar kelime kutularını kaydırabilir.
- **DRM/şifreli PDF'ler** desteklenmez.
- **İstemci tarafı API anahtarı** dağıtım için uygun değildir; kişisel kullanım içindir.
- **Ücretsiz Gemini kotası** model ve güne göre değişir; yoğun kullanımda 429 alınabilir.
- **TTS cihaza bağlıdır.** Ses kalitesi, dil desteği ve kelime vurgusu motorun yeteneklerine göre değişir.
- **Sayfa offset'i** kaydedilir ama açılışta yalnızca sayfa numarası geri yüklenir; tam piksel konumu değil.
- **Senkronizasyon yok.** Kitaplar, kelimeler ve ayarlar yalnızca o cihazda.
- **Arayüz (menü/buton) metinleri** şu an Türkçe. Ana dil seçimi Gemini'nin ürettiği tüm açıklamaları ve TTS dilini değiştirir; uygulama kabuğunun tam çevirisi henüz yok.
- **Dil kataloğu** 15 dili kapsar: tr, en, ru, es, it, de, fr, ja, az, kk, tk, uz, ky, zh, pl. Arapça/İbranice gibi RTL diller henüz yok.
- **Masaüstü/web** için mimari ayrıştırılmış olsa da yalnızca Android hedefi vardır.
- **Not/annotation dışa aktarma** henüz yok.

---

## 13. Yol Haritası

1. **OCR:** ML Kit Text Recognition veya Tesseract ile taranmış PDF desteği.
2. **Bağlam duyarlı önbellek:** Kelime + cümle hash'i ile isteğe bağlı ikinci seviye kayıt.
3. **Kelime listeleri:** Favorilerden Flashcard/quiz üretimi, aralıklı tekrar (SRS).
4. **Dışa aktarma:** CSV/Anki formatında kelime ve bağlam dışa aktarma.
5. **Gelişmiş PDF:** İçindekiler, arama, çift sayfa modu, gece okuma filtresi.
6. **Daha fazla dil:** Arapça, Portekizce, Korece gibi dilleri ekleme; RTL desteği ve uygulama arayüzünün çok dilli hale getirilmesi.
7. **Masaüstü/web:** `core` ve `domain` katmanlarını Kotlin Multiplatform'a taşıma.
8. **Güvenli anahtar yönetimi:** İsteğe bağlı kullanıcı proxy'si / kendi sunucun.
9. **Erişilebilirlik:** TalkBack, dinamik yazı boyutu, yüksek kontrast.

---

## 14. Gizlilik

- PDF dosyaları ve kelime kayıtları cihazda kalır.
- Sunucu, reklam, analitik veya hesap sistemi yoktur.
- Bir kelime analiz edilirken **yalnızca kelime ve içinde geçtiği cümle** seçilen Gemini modeline gönderilir. PDF'in tamamı yüklenmez.
- API anahtarı cihazdaki DataStore'da tutulur. Android yedekleme (`allowBackup=false`) kapalıdır.
- `local.properties` ve gerçek anahtarlar sürüm kontrolüne eklenmemelidir.

---

## 15. Lisans ve Teşekkür

Bu proje **Apache License 2.0** ile açık kaynak olarak yayınlanmıştır. Tam metin: [`LICENSE`](LICENSE).

Copyright 2026 erenGokturk

- Uygulama kodu: Apache License 2.0 kapsamında özgürce kullanılabilir, değiştirilebilir ve dağıtılabilir.
- **PDFBox-Android** — Apache License 2.0 (`com.tom-roush`).
- **Gemini API** — Google AI Studio kullanım koşullarına tabidir.
- **Jetpack Compose, Room, DataStore, OkHttp, Coil, kotlinx.serialization** — ilgili açık kaynak lisansları.

> Booklate yalnızca kişisel öğrenme amacıyla kullanılmalıdır. İşlediğin PDF'lerin telif haklarına saygı göster.

---

## 16. Sürüm Notları

### 1.1.0
- **Dil kataloğu 15 dile çıkarıldı:** Türkçe, İngilizce, Rusça, İspanyolca, İtalyanca, Almanca, Fransızca, Japonca, Azerbaycan Dili, Kazakça, Türkmence, Özbekçe, Kırgızca, Çince, Lehçe. Aynı liste hem kitap dili hem ana dil için kullanılır.
- **Kitap dili artık kitap başına:** `books` tablosuna `languageCode` sütunu eklendi (Room v2 + `MIGRATION_1_2`). PDF eklerken dil sorulur; kütüphanede `⋮` → **Dili değiştir** ile sonradan değiştirilebilir.
- **Ayarlardaki “öğrendiğim dil” kaldırıldı.** Ayarlar yalnızca **ana dil** (açıklama dili) ve okuma/TTS tercihlerini içerir. Yeni kitap eklerken son seçilen dil önerilir.
- **Analiz ve TTS kitabın dilini kullanır:** Gemini istemi hedef dili kitaptan, açıklama dilini ana dilden alır. İstemdeki Türkçe/Rusça sabitleri kaldırıldı; dil bilgisi alanları dile göre opsiyonel (`-`).
- **Önbellek:** `PROMPT_VERSION` 4'e çıkarıldı; aynı kelime farklı kitap dillerinde ayrı kayıt olarak tutulur.
- Kitaplık kartında ve okuma ekranındaki kitap bilgisinde dil rozeti gösterilir.

### 1.0.3
- **Release yapılandırması:** İmzalı `release` derlemesi eklendi (`keystore.properties` + `keystore/booklate-release.jks`). Release APK'da `.debug` / `-debug` sonekleri yoktur; paket adı `com.erengokturk.booklate`, sürüm adı `1.0.3` olur. R8 için eksik `com.gemalto.jp2` uyarısı bastırıldı.
- **Kelime dokunma konumu düzeltmesi:** PDFBox `TextPosition.y` değeri kelimenin üstü değil **taban çizgisi (baseline)** olduğu için kelime kutuları bir satır aşağı kayıyordu; hem vurgu hem dokunma alanı kelimenin altına düşüyordu. Kutular artık baseline'dan yukarı doğru hesaplanıyor.
- **Koyu tema kontrast düzeltmesi:** `TagChip` varsayılan olarak açık şeftali arka plan kullanıyor, yazı rengi ise koyu temanın açık rengini alıyordu. Eş anlamlılar, model etiketi ve slider değeri gibi çipler okunamıyordu. Çipler artık arka plana göre otomatik kontrastlı yazı rengi seçiyor; varsayılan arka plan tema duyarlı (`primaryContainer`).
- Alt navigasyon göstergesi ve ayarlardaki başarı bildirimi de tema duyarlı renklere taşındı.

### 1.0.2
- **Koyu tema düzeltmesi:** PDF sayfaları şeffaf arka planla çizildiği için koyu temada sayfa, uygulamanın koyu arka planıyla kararıyordu. Sayfa bitmap'i artık çizimden önce beyaza boyanıyor; koyu temada da sayfalar normal beyaz görünüyor.
- **Gemini model güncellemesi:** `gemini-2.5-flash`, `gemini-2.5-flash-lite` ve `gemini-2.0-flash` yeni API anahtarlarında kullanılamıyor. Model listesi `gemini-3.5-flash-lite` (varsayılan), `gemini-3.6-flash` ve `gemini-3.8-flash` olarak güncellendi. Kayıtlı eski model otomatik olarak yeni modele taşınıyor.

### 1.0.1
- **Kritik düzeltme:** `PdfRenderer` yalnızca `ARGB_8888` hedef bitmap kabul ettiği için sayfalar `RGB_565` ile oluşturuluyordu ve her PDF'te `Unsupported pixel format` hatası alınıyordu. Bitmap formatı `ARGB_8888` yapıldı.
- Bitmap önbelleği artık gerçek bayt boyutuna göre sınırlanıyor (önceden giriş sayısına göre ölçülüyordu).
- Okuyucu, uzak sayfaların bitmap'lerini bellekten bırakıyor; 300+ sayfalık kitaplarda OOM riski azaltıldı.
- Taranmış (metin katmanı olmayan) sayfalarda kelime seçimi/çeviri çalışmaz; bu sayfalar için "Metin katmanı yok" bilgisi gösterilir. OCR yol haritasındadır.

