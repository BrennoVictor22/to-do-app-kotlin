package com.example.todo

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*

@Entity(tableName = "categories")
data class Category(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String)

@Entity(tableName = "tasks", foreignKeys = [ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.SET_NULL)], indices = [Index("categoryId")])
data class Task(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val description: String = "", val completed: Boolean = false, val dueDateTime: Long? = null, val createdAt: Long = System.currentTimeMillis(), val categoryId: Long? = null)

data class TaskWithCategory(@Embedded val task: Task, @ColumnInfo(name = "category_name") val categoryName: String?)

@Dao
interface TaskDao {
    @Query("SELECT tasks.*, categories.name AS category_name FROM tasks LEFT JOIN categories ON tasks.categoryId = categories.id ORDER BY completed ASC, dueDateTime IS NULL, dueDateTime ASC, createdAt DESC")
    fun observeAll(): LiveData<List<TaskWithCategory>>
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: Long): Task?
    @Insert suspend fun insert(task: Task): Long
    @Update suspend fun update(task: Task)
    @Delete suspend fun delete(task: Task)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    fun observeAll(): LiveData<List<Category>>
    @Insert suspend fun insert(category: Category)
    @Update suspend fun update(category: Category)
    @Delete suspend fun delete(category: Category)
}

@Database(entities = [Task::class, Category::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "todo.db").build().also { instance = it }
        }
    }
}
