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

    /** Toutes les transactions, pour le bilan par jour et par mois. */
    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC, id DESC")
    fun allExpensesFlow(): Flow<List<Expense>>

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
}

@Database(entities = [Expense::class, Budget::class], version = 3, exportSchema = false)
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

        // Les versions de test avec assistant IA (0.11.x) avaient passé la base en v2 avec une table « notes ».
        // On revient à la version simple : v3 = mêmes tables que v1, la table des notes est retirée.
        private val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `notes`")
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
                ).addMigrations(MIGRATION_1_3, MIGRATION_2_3).addCallback(object : RoomDatabase.Callback() {
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
