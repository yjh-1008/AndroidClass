package com.example.repository_pattern

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*


@Entity
data class RepoD(
    @PrimaryKey val login: String,
    val contributions: Int
    )

@Dao
interface MyDao {
    @Query("SELECT * FROM RepoD")
    fun getAll(): LiveData<List<RepoD>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(repos: List<RepoD>)
}

@Database(entities = [RepoD::class], version = 1)
abstract class MyDatabase : RoomDatabase() {
    abstract val myDao : MyDao

    companion object {
        private var INSTANCE: MyDatabase? = null
        fun getDatabase(context: Context) : MyDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(
                    context, MyDatabase::class.java, "contibutor_database")
                    .build()
            }
            return INSTANCE as MyDatabase
        }
    }
}

