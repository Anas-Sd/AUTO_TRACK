package com.autotrack.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject

class OfflineQueueDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "autotrack_offline.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_QUEUE = "offline_transactions"
        const val COLUMN_ID = "id"
        const val COLUMN_PAYLOAD = "payload"
        const val COLUMN_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_QUEUE (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PAYLOAD TEXT NOT NULL,
                $COLUMN_CREATED_AT INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_QUEUE")
        onCreate(db)
    }

    fun enqueueTransaction(jsonPayload: JSONObject): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PAYLOAD, jsonPayload.toString())
            put(COLUMN_CREATED_AT, System.currentTimeMillis())
        }
        return db.insert(TABLE_QUEUE, null, values)
    }

    fun getPendingTransactions(): List<Pair<Long, JSONObject>> {
        val list = mutableListOf<Pair<Long, JSONObject>>()
        val db = readableDatabase
        val cursor = db.query(TABLE_QUEUE, arrayOf(COLUMN_ID, COLUMN_PAYLOAD), null, null, null, null, "$COLUMN_ID ASC")
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID))
                val raw = it.getString(it.getColumnIndexOrThrow(COLUMN_PAYLOAD))
                try {
                    list.add(Pair(id, JSONObject(raw)))
                } catch (e: Exception) {
                    // ignore corrupted json
                }
            }
        }
        return list
    }

    fun removeTransaction(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_QUEUE, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }
}
