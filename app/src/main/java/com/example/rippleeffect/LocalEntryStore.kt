package com.example.rippleeffect

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * On-device SQLite store for submitted sales and expense entries.
 *
 * The report list is built from this store, so entries survive the app being
 * closed, killed or the phone restarting. The database file lives in the
 * app's private internal storage, and every query is parameterised.
 * See docs/user-stories/01-keep-entries-after-restart.md.
 */
class LocalEntryStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                $COL_ID TEXT PRIMARY KEY NOT NULL,
                $COL_DESCRIPTION TEXT NOT NULL,
                $COL_AMOUNT REAL NOT NULL,
                $COL_CURRENCY TEXT NOT NULL,
                $COL_TYPE TEXT NOT NULL,
                $COL_DATE_CREATED INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Only version 1 exists. Future schema changes must migrate here
        // instead of dropping the table, so users never lose entries.
    }

    /** Every submitted entry, newest first. */
    suspend fun loadAll(): List<SubmittedEntry> = withContext(Dispatchers.IO) {
        readableDatabase.query(
            TABLE,
            arrayOf(COL_ID, COL_DESCRIPTION, COL_AMOUNT, COL_CURRENCY, COL_TYPE, COL_DATE_CREATED),
            null, null, null, null,
            "$COL_DATE_CREATED DESC, rowid DESC"
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val currencyCode = cursor.getString(3)
                    add(
                        SubmittedEntry(
                            id = cursor.getString(0),
                            description = cursor.getString(1),
                            amount = cursor.getDouble(2),
                            currency = Currency.entries.firstOrNull { it.code == currencyCode } ?: Currency.USD,
                            type = cursor.getString(4),
                            dateCreated = Date(cursor.getLong(5))
                        )
                    )
                }
            }
        }
    }

    /**
     * Saves [entries] in one transaction: either all of them are stored or none.
     * An id that is already stored is skipped, so saving twice never duplicates.
     */
    suspend fun insertAll(entries: List<SubmittedEntry>) = withContext(Dispatchers.IO) {
        if (entries.isEmpty()) return@withContext
        val db = writableDatabase
        db.beginTransaction()
        try {
            entries.forEach { entry ->
                val values = ContentValues().apply {
                    put(COL_ID, entry.id)
                    put(COL_DESCRIPTION, entry.description)
                    put(COL_AMOUNT, entry.amount)
                    put(COL_CURRENCY, entry.currency.code)
                    put(COL_TYPE, entry.type)
                    put(COL_DATE_CREATED, entry.dateCreated.time)
                }
                val rowId = db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE)
                // -1 with CONFLICT_IGNORE means the id already exists; anything
                // else that fails throws and rolls the transaction back.
                check(rowId != -1L || exists(db, entry.id)) { "Failed to insert entry ${entry.id}" }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun exists(db: SQLiteDatabase, id: String): Boolean =
        db.query(TABLE, arrayOf(COL_ID), "$COL_ID = ?", arrayOf(id), null, null, null).use { it.moveToFirst() }

    companion object {
        private const val DATABASE_NAME = "ripple_effect.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE = "submitted_entries"
        private const val COL_ID = "id"
        private const val COL_DESCRIPTION = "description"
        private const val COL_AMOUNT = "amount"
        private const val COL_CURRENCY = "currency"
        private const val COL_TYPE = "type"
        private const val COL_DATE_CREATED = "date_created"
    }
}
