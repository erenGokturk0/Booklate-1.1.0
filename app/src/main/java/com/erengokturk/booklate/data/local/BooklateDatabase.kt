package com.erengokturk.booklate.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BookEntity::class, WordLookupEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class BooklateDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun wordLookupDao(): WordLookupDao

    companion object {
        /**
         * v1'de kitap dili yoktu; tek bir genel dil ayarı kullanılıyordu.
         * v2 ile her kitap kendi dilini taşır. Eski kitaplar varsayılan
         * olarak Rusça kabul edilir; kullanıcı kitap menüsünden değiştirebilir.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE books ADD COLUMN languageCode TEXT NOT NULL DEFAULT 'ru'",
                )
            }
        }

        fun build(context: Context): BooklateDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                BooklateDatabase::class.java,
                "booklate.db",
            )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
    }
}
