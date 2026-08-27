package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Expense
import com.example.data.model.KeywordRule

@Database(entities = [Expense::class, KeywordRule::class], version = 1, exportSchema = false)
abstract class FireCashDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun keywordRuleDao(): KeywordRuleDao

    companion object {
        @Volatile
        private var INSTANCE: FireCashDatabase? = null

        fun getDatabase(context: Context): FireCashDatabase = getInstance(context)

        fun getInstance(context: Context): FireCashDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FireCashDatabase::class.java,
                    "firecash_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
