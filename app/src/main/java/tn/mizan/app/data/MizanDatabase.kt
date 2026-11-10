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
}

@Database(entities = [Expense::class, Budget::class], version = 1, exportSchema = false)
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

        @Volatile
        private var instance: MizanDatabase? = null

        fun get(context: Context): MizanDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MizanDatabase::class.java,
                    "mizan.db"
                ).addCallback(object : RoomDatabase.Callback() {
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
