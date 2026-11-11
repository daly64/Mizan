package tn.mizan.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface MizanDao {
    @Query("SELECT * FROM expenses WHERE dateMillis BETWEEN :start AND :end ORDER BY dateMillis DESC, id DESC")
    fun expensesBetween(start: Long, end: Long): Flow<List<Expense>>

    @Insert
    suspend fun insert(expense: Expense): Long

    @Delete
    suspend fun delete(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM budgets")
    fun budgets(): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: Budget)

    // --- Sauvegarde et restauration ---

    @Query("SELECT * FROM expenses ORDER BY dateMillis, id")
    suspend fun allExpenses(): List<Expense>

    @Query("SELECT * FROM budgets")
    suspend fun allBudgets(): List<Budget>

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<Expense>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudgets(budgets: List<Budget>)

    // --- Assistant : lectures ponctuelles et notes ---

    @Query("SELECT * FROM expenses WHERE dateMillis BETWEEN :start AND :end ORDER BY dateMillis DESC, id DESC")
    suspend fun expensesBetweenNow(start: Long, end: Long): List<Expense>

    @Insert
    suspend fun insertNote(note: Note): Long

    @Query("SELECT * FROM notes WHERE text LIKE '%' || :query || '%' ORDER BY dateMillis DESC LIMIT :limit")
    suspend fun searchNotes(query: String, limit: Int): List<Note>

    @Query("SELECT * FROM notes ORDER BY dateMillis, id")
    suspend fun allNotes(): List<Note>

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<Note>)
}

@Database(entities = [Expense::class, Budget::class, Note::class], version = 2, exportSchema = false)
abstract class MizanDatabase : RoomDatabase() {
    abstract fun dao(): MizanDao

    companion object {
        /** Budgets proposés au premier lancement (total : 2 500 dinars). */
        private val DEFAULT_BUDGETS = mapOf(
            Category.COURSES to 1_000_000L,
            Category.TRANSPORT to 400_000L,
            Category.LOISIRS to 300_000L,
            Category.FACTURES to 800_000L
        )

        /** v2 : table des notes de l'assistant (les dépenses et budgets ne bougent pas). */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`text` TEXT NOT NULL, `dateMillis` INTEGER NOT NULL)"
                )
            }
        }

        @Volatile
        private var instance: MizanDatabase? = null

        fun get(context: Context): MizanDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MizanDatabase::class.java,
                    "mizan.db"
                ).addMigrations(MIGRATION_1_2).addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        DEFAULT_BUDGETS.forEach { (category, limit) ->
                            db.execSQL(
                                "INSERT INTO budgets (category, limitMillimes) VALUES (?, ?)",
                                arrayOf<Any?>(category.name, limit)
                            )
                        }
                    }
                }).build().also { instance = it }
            }
    }
}
