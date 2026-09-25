package com.example.todo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.get(application)
    val tasks = db.taskDao().observeAll()
    val categories = db.categoryDao().observeAll()

    fun saveTask(task: Task, onSaved: () -> Unit = {}) = viewModelScope.launch(Dispatchers.IO) {
        if (task.id == 0L) db.taskDao().insert(task) else db.taskDao().update(task)
        onSaved()
    }
    fun deleteTask(task: Task, onDone: () -> Unit = {}) = viewModelScope.launch(Dispatchers.IO) { db.taskDao().delete(task); onDone() }
    fun toggle(task: Task) = saveTask(task.copy(completed = !task.completed))
    fun saveCategory(category: Category) = viewModelScope.launch(Dispatchers.IO) { if (category.id == 0L) db.categoryDao().insert(category) else db.categoryDao().update(category) }
    fun deleteCategory(category: Category) = viewModelScope.launch(Dispatchers.IO) { db.categoryDao().delete(category) }
}
