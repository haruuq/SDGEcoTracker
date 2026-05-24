package com.example.sdgecotracker

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "EcoTrack.db"
        private const val DATABASE_VERSION = 5

        const val TABLE_USERS = "users"
        const val COLUMN_USER_ID = "id"
        const val COLUMN_USER_NAME = "username"
        const val COLUMN_USER_PASSWORD = "password"
        const val COLUMN_USER_ROLE = "role"

        const val TABLE_LOG = "consumption_log"
        const val COLUMN_ID = "id"
        const val COLUMN_LOG_USER_ID = "user_id"
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_ITEM = "item"
        const val COLUMN_QUANTITY = "quantity"
        const val COLUMN_DATE = "date"
        const val COLUMN_SCORE = "score"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createUsersTable = ("CREATE TABLE $TABLE_USERS ("
                + "$COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$COLUMN_USER_NAME TEXT, "
                + "$COLUMN_USER_PASSWORD TEXT DEFAULT '1234', "
                + "$COLUMN_USER_ROLE TEXT)")
        db?.execSQL(createUsersTable)

        val createLogTable = ("CREATE TABLE $TABLE_LOG ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$COLUMN_LOG_USER_ID INTEGER, "
                + "$COLUMN_CATEGORY TEXT, "
                + "$COLUMN_ITEM TEXT, "
                + "$COLUMN_QUANTITY TEXT, "
                + "$COLUMN_DATE TEXT, "
                + "$COLUMN_SCORE INTEGER DEFAULT 0, "
                + "FOREIGN KEY($COLUMN_LOG_USER_ID) REFERENCES $TABLE_USERS($COLUMN_USER_ID))")
        db?.execSQL(createLogTable)

        // Placeholder users: Jess (user), Miguel (user), Admin (admin) - all password 1234
        db?.execSQL("INSERT INTO $TABLE_USERS ($COLUMN_USER_NAME, $COLUMN_USER_PASSWORD, $COLUMN_USER_ROLE) VALUES ('Jess', '1234', 'user')")
        db?.execSQL("INSERT INTO $TABLE_USERS ($COLUMN_USER_NAME, $COLUMN_USER_PASSWORD, $COLUMN_USER_ROLE) VALUES ('Miguel', '1234', 'user')")
        db?.execSQL("INSERT INTO $TABLE_USERS ($COLUMN_USER_NAME, $COLUMN_USER_PASSWORD, $COLUMN_USER_ROLE) VALUES ('Admin', '1234', 'admin')")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_LOG")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun login(username: String, password: String): User? {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE $COLUMN_USER_NAME = ? AND $COLUMN_USER_PASSWORD = ?",
            arrayOf(username, password)
        )
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ROLE))
            )
        }
        cursor.close()
        return user
    }

    fun addEntry(userId: Int, category: String, item: String, quantity: String, date: String, score: Int): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_LOG_USER_ID, userId)
            put(COLUMN_CATEGORY, category)
            put(COLUMN_ITEM, item)
            put(COLUMN_QUANTITY, quantity)
            put(COLUMN_DATE, date)
            put(COLUMN_SCORE, score)
        }
        return db.insert(TABLE_LOG, null, values)
    }

    fun getAllEntries(userId: Int? = null): List<EcoEntry> {
        val list = mutableListOf<EcoEntry>()
        val db = this.readableDatabase
        val query = if (userId != null) {
            "SELECT * FROM $TABLE_LOG WHERE $COLUMN_LOG_USER_ID = $userId ORDER BY $COLUMN_ID DESC"
        } else {
            "SELECT * FROM $TABLE_LOG ORDER BY $COLUMN_ID DESC"
        }
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                list.add(EcoEntry(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LOG_USER_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_QUANTITY)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SCORE))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getDailyScores(userId: Int): List<Pair<String, Int>> {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_DATE, MAX(0, 100 - SUM($COLUMN_SCORE)) FROM $TABLE_LOG WHERE $COLUMN_LOG_USER_ID = ? GROUP BY $COLUMN_DATE ORDER BY $COLUMN_DATE ASC",
            arrayOf(userId.toString())
        )
        val result = mutableListOf<Pair<String, Int>>()
        if (cursor.moveToFirst()) {
            do { result.add(Pair(cursor.getString(0), cursor.getInt(1))) } while (cursor.moveToNext())
        }
        cursor.close()
        return result
    }

    fun getWeeklyAvgScore(userId: Int, weeksBack: Int): Int {
        val entries = getAllEntries(userId)
        if (entries.isEmpty()) return 0
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        cal.add(java.util.Calendar.WEEK_OF_YEAR, -weeksBack)
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
        val startDate = cal.time
        cal.add(java.util.Calendar.DAY_OF_WEEK, 6)
        val endDate = cal.time
        val weekEntries = entries.filter {
            try {
                val d = sdf.parse(it.date) ?: return@filter false
                !d.before(startDate) && !d.after(endDate)
            } catch (e: Exception) { false }
        }
        if (weekEntries.isEmpty()) return 0
        // Compute daily eco scores (100 - SUM of penalties per day), then average across days
        val dailyScores = weekEntries.groupBy { it.date }
            .values
            .map { dayEntries -> (100 - dayEntries.sumOf { it.score }).coerceAtLeast(0) }
        return dailyScores.average().toInt()
    }

    fun deleteEntry(id: Int) {
        val db = this.writableDatabase
        db.delete(TABLE_LOG, "$COLUMN_ID=?", arrayOf(id.toString()))
    }

    fun updateEntry(id: Int, category: String, item: String, quantity: String, date: String, score: Int): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CATEGORY, category)
            put(COLUMN_ITEM, item)
            put(COLUMN_QUANTITY, quantity)
            put(COLUMN_DATE, date)
            put(COLUMN_SCORE, score)
        }
        return db.update(TABLE_LOG, values, "$COLUMN_ID=?", arrayOf(id.toString()))
    }

    fun getTodayCount(userId: Int, date: String): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_LOG WHERE $COLUMN_DATE = ? AND $COLUMN_LOG_USER_ID = ?",
            arrayOf(date, userId.toString())
        )
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()
        return count
    }

    fun getTodayScore(userId: Int, date: String): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COALESCE(SUM($COLUMN_SCORE), 0) FROM $TABLE_LOG WHERE $COLUMN_DATE = ? AND $COLUMN_LOG_USER_ID = ?",
            arrayOf(date, userId.toString())
        )
        cursor.moveToFirst()
        val totalPenalty = cursor.getInt(0)
        cursor.close()
        return (100 - totalPenalty).coerceAtLeast(0)
    }

    fun getAllUsers(): List<User> {
        val list = mutableListOf<User>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(User(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ROLE))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun addUser(username: String, password: String, role: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_NAME, username)
            put(COLUMN_USER_PASSWORD, password)
            put(COLUMN_USER_ROLE, role)
        }
        return db.insert(TABLE_USERS, null, values)
    }

    fun updateUser(userId: Int, username: String, password: String?, role: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_NAME, username)
            put(COLUMN_USER_ROLE, role)
            if (password != null) put(COLUMN_USER_PASSWORD, password)
        }
        db.update(TABLE_USERS, values, "$COLUMN_USER_ID=?", arrayOf(userId.toString()))
    }

    fun deleteUser(userId: Int) {
        val db = this.writableDatabase
        db.delete(TABLE_LOG, "$COLUMN_LOG_USER_ID=?", arrayOf(userId.toString()))
        db.delete(TABLE_USERS, "$COLUMN_USER_ID=?", arrayOf(userId.toString()))
    }

    fun getCommunityImpact(): Map<String, Double> {
        // Compute avg daily eco score per category:
        // For each (category, date) pair, eco score = 100 - SUM(penalties).
        // Then average those daily scores per category.
        val impact = mutableMapOf<String, Double>()
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_CATEGORY, AVG(daily_score) FROM (" +
            "  SELECT $COLUMN_CATEGORY, MAX(0, 100 - SUM($COLUMN_SCORE)) AS daily_score " +
            "  FROM $TABLE_LOG GROUP BY $COLUMN_CATEGORY, $COLUMN_DATE" +
            ") GROUP BY $COLUMN_CATEGORY", null
        )
        if (cursor.moveToFirst()) {
            do { impact[cursor.getString(0)] = cursor.getDouble(1) } while (cursor.moveToNext())
        }
        cursor.close()
        return impact
    }

    fun getMostCommonWasteCategory(): String {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_CATEGORY, COUNT(*) as cnt FROM $TABLE_LOG GROUP BY $COLUMN_CATEGORY ORDER BY cnt DESC LIMIT 1",
            null
        )
        var category = "None"
        if (cursor.moveToFirst()) { category = cursor.getString(0) }
        cursor.close()
        return category
    }

    fun getCategoryDistribution(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_CATEGORY, COUNT(*) FROM $TABLE_LOG GROUP BY $COLUMN_CATEGORY", null
        )
        if (cursor.moveToFirst()) {
            do { map[cursor.getString(0)] = cursor.getInt(1) } while (cursor.moveToNext())
        }
        cursor.close()
        return map
    }

    fun getUserScoreSummary(): List<Triple<Int, String, Int>> {
        // Compute avg daily eco score per user:
        // For each (user, date) pair, eco score = 100 - SUM(penalties).
        // Then average those daily scores per user.
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT u.$COLUMN_USER_ID, u.$COLUMN_USER_NAME, COALESCE(AVG(ds.daily_score), 0) " +
            "FROM $TABLE_USERS u LEFT JOIN (" +
            "  SELECT $COLUMN_LOG_USER_ID, MAX(0, 100 - SUM($COLUMN_SCORE)) AS daily_score " +
            "  FROM $TABLE_LOG GROUP BY $COLUMN_LOG_USER_ID, $COLUMN_DATE" +
            ") ds ON u.$COLUMN_USER_ID = ds.$COLUMN_LOG_USER_ID " +
            "GROUP BY u.$COLUMN_USER_ID", null
        )
        val result = mutableListOf<Triple<Int, String, Int>>()
        if (cursor.moveToFirst()) {
            do { result.add(Triple(cursor.getInt(0), cursor.getString(1), cursor.getInt(2))) } while (cursor.moveToNext())
        }
        cursor.close()
        return result
    }
}

data class EcoEntry(val id: Int, val userId: Int, val category: String, val item: String, val quantity: String, val date: String, val score: Int)
data class User(val id: Int, val username: String, val role: String)
