package com.oaktech.pilldiary.tabs.controllers

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.oaktech.pilldiary.Entry
import com.oaktech.pilldiary.Pill

/**
 * Class to handle SQLite database interactions
 */
class PillBase(context: Context?) :
    SQLiteOpenHelper(context, "PillBase", null, 2) {
    private val ENTRY_TABLE = "Entries";
    private val PILL_TABLE = "Pills";

    private val DATE_KEY = "Date"
    private val ENTRY_MESSAGE_KEY = "Entry_Message"
    private val FROM_ADMISTERED_PILL_KEY = "From_Administered_Pill"
    private val NAME_KEY = "Name"
    private val DOSAGE_KEY = "Dosage"

    override fun onCreate(db: SQLiteDatabase) {
        // method to create table for pills and entries in database if they don't exist
        db.execSQL("CREATE TABLE IF NOT EXISTS ${ENTRY_TABLE}($DATE_KEY TEXT, $ENTRY_MESSAGE_KEY TEXT, $FROM_ADMISTERED_PILL_KEY INTEGER);")
        db.execSQL("CREATE TABLE IF NOT EXISTS $PILL_TABLE($NAME_KEY TEXT, $DOSAGE_KEY TEXT);")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // method called if the database version is changed
    }

    /**
     * Method to execute SQL queries
     *
     * @param query SQL query
     */
    fun execute(query: String) {
        val base: SQLiteDatabase = this.writableDatabase
        base.execSQL(query)
        base.close()
    }

    /**
     * Method to add/edit an Entry object into the SQLite database
     *
     * If the Entry object has an id value, it is considered as a new object and will be
     * inserted into the database, otherwise the rowid in the table matching
     * the id will be updated
     *
     * @param entry Entry object to be upserted
     */
    fun upsertEntry(entry: Entry) {
        val isPillAdministration = if (entry.isPillAdministeration) 1 else 0
        if (entry.id == null) {
            execute("INSERT INTO $ENTRY_TABLE VALUES('${entry.displayDate()}' , '${entry.entryMessage}', '${isPillAdministration}');")
        } else {
            execute("UPDATE $ENTRY_TABLE SET $DATE_KEY = '${entry.displayDate()}', $ENTRY_MESSAGE_KEY = '${entry.entryMessage}' WHERE rowid = '${entry.id}';")
        }
    }

    /**
     * Method to delete an Entry object from the SQLite database
     */
    fun deleteEntry(entry: Entry) {
        execute("DELETE FROM $ENTRY_TABLE WHERE rowid = '${entry.id}';")
    }

    /**
     * Method to get all entries from the database
     *
     * @param onlyPillAdministrations whether to return only entries that were created automatically
     * from pill administration. If false, return only entries that were not created automatically
     * from pill administration
     *
     * @return all Entries in the database based on applied filter, will return all Entries
     * if filter is null
     */
    fun getEntries(onlyPillAdministrations: Boolean?): ArrayList<Entry> {
        val entries: ArrayList<Entry> = ArrayList()
        val base: SQLiteDatabase = this.readableDatabase
        lateinit var cursor: Cursor
        if (onlyPillAdministrations == null) {
            cursor = base.rawQuery("SELECT rowid, * FROM $ENTRY_TABLE;", null)
        } else {
            val filterValue = if (onlyPillAdministrations) 1 else 0
            cursor = base.rawQuery(
                "SELECT rowid, * FROM $ENTRY_TABLE WHERE $FROM_ADMISTERED_PILL_KEY = $filterValue;",
                null
            )
        }
        if (cursor.count > 0) {
            cursor.moveToFirst()
            do {
                val entry = Entry(cursor.getString(1), cursor.getString(2))
                entry.id = (cursor.getInt(0))
                entries.add(entry)
            } while (cursor.moveToNext())
        }
        cursor.close()
        base.close()
        return entries
    }

    /**
     * Method to add/edit a Pill object into the SQLite database
     *
     * If the Pill object has an id value, it is considered as a new object and will be
     * inserted into the database, otherwise the rowid in the table matching
     * the id will be updated
     *
     * @param pill Pill object to be upserted
     */
    fun upsertPill(pill: Pill) {
        if (pill.id == null) {
            execute("INSERT INTO $PILL_TABLE VALUES('${pill.name}', '${pill.dosage}');")
        } else {
            execute("UPDATE $PILL_TABLE SET $NAME_KEY = '${pill.name}', $DOSAGE_KEY = '${pill.dosage}' WHERE rowid = '${pill.id}';")
        }
    }

    /**
     * Method to delete a Pill object from the SQLite database
     */
    fun deletePill(pill: Pill) {
        execute("DELETE FROM $PILL_TABLE WHERE rowid = '${pill.id}';")
    }

    /**
     * Method to get all pills in database
     *
     * @return all pills in the database
     */
    fun getPills(): ArrayList<Pill> {
        val pills: ArrayList<Pill> = ArrayList()
        val base: SQLiteDatabase = this.readableDatabase
        val cursor: Cursor = base.rawQuery("SELECT rowid, * FROM $PILL_TABLE;", null)
        if (cursor.count > 0) {
            cursor.moveToFirst()
            do {
                pills.add(
                    Pill(
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getInt(0)
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        base.close()
        return pills
    }

    /**
     * Method to check if pill name already exists in database
     *
     * @param pillName prospective name for Pill
     *
     * @return True if a pill with that name already exists in the database
     */
    fun pillNameAlreadyExists(pillName: String): Boolean {
        val base: SQLiteDatabase = this.readableDatabase
        val cursor: Cursor =
            base.rawQuery("SELECT count(*) FROM $PILL_TABLE WHERE $NAME_KEY = '$pillName';", null)
        cursor.moveToFirst()
        val count: Int = cursor.getInt(0)
        cursor.close()
        base.close()
        return count > 0
    }
}