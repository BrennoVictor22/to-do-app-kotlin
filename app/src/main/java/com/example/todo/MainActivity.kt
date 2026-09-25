package com.example.todo

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import java.text.DateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val model by viewModels<TodoViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); if (Build.VERSION.SDK_INT >= 33) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10); showList() }
    fun showList() { supportFragmentManager.beginTransaction().replace(android.R.id.content, TaskListFragment()).commit() }
    fun showEditor(task: Task? = null) { supportFragmentManager.beginTransaction().replace(android.R.id.content, TaskEditorFragment.newInstance(task)).addToBackStack(null).commit() }
    fun showCategories() { supportFragmentManager.beginTransaction().replace(android.R.id.content, CategoryFragment()).addToBackStack(null).commit() }
    fun viewModel() = model
}

fun text(context: android.content.Context, value: String, size: Float = 16f) = TextView(context).apply { text = value; textSize = size; setPadding(20, 14, 20, 14) }
fun root(context: android.content.Context) = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(18, 16, 18, 10) }

class TaskListFragment : Fragment() {
    private var status = 0
    override fun onCreateView(i: LayoutInflater, c: android.view.ViewGroup?, b: Bundle?): View {
        val ctx = requireContext(); val column = root(ctx); val header = LinearLayout(ctx); val title = text(ctx, "Minhas tarefas", 28f); header.addView(title, LinearLayout.LayoutParams(0, -2, 1f)); val categories = Button(ctx).apply { text = "Categorias"; setOnClickListener { (activity as MainActivity).showCategories() } }; header.addView(categories); column.addView(header)
        val filters = Spinner(ctx); filters.adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, listOf("Todas", "Pendentes", "Concluídas")); filters.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener { override fun onNothingSelected(p: android.widget.AdapterView<*>?) {} override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) { status = pos; render(list, column) } }; column.addView(filters)
        val categoryFilter = Spinner(ctx); column.addView(categoryFilter); model().categories.observe(viewLifecycleOwner) { cats -> categoryFilter.adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, listOf("Todas as categorias") + cats.map { it.name }); categoryFilter.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener { override fun onNothingSelected(p: android.widget.AdapterView<*>?) {} override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) { categoryId = cats.getOrNull(pos - 1)?.id; render(list, column) } } }
        val add = Button(ctx).apply { text = "+ Nova tarefa"; setOnClickListener { (activity as MainActivity).showEditor() } }; column.addView(add)
        val listContainer = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }; column.addView(ScrollView(ctx).apply { addView(listContainer) }, LinearLayout.LayoutParams(-1, 0, 1f));
        model().tasks.observe(viewLifecycleOwner) { list = it; render(it, column) }; return column
    }
    private var list: List<TaskWithCategory> = emptyList()
    private var categoryId: Long? = null
    private fun model(): TodoViewModel = (activity as MainActivity).viewModel()
    private fun render(source: List<TaskWithCategory>, parent: LinearLayout) { val scroll = parent.getChildAt(parent.childCount - 1) as? ScrollView ?: return; val box = scroll.getChildAt(0) as? LinearLayout ?: return; box.removeAllViews(); source.filter { (status == 0 || (status == 1 && !it.task.completed) || (status == 2 && it.task.completed)) && (categoryId == null || it.task.categoryId == categoryId) }.forEach { item -> val task = item.task; val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(8, 10, 8, 10) }; val check = CheckBox(requireContext()).apply { text = task.title; isChecked = task.completed; textSize = 18f; setOnClickListener { if (task.completed != isChecked) { model().toggle(task); if (isChecked) ReminderScheduler.cancel(requireContext(), task.id) else ReminderScheduler.schedule(requireContext(), task) } } }; row.addView(check); val details = listOfNotNull(item.categoryName?.let { "Categoria: $it" }, task.dueDateTime?.let { "Vence: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))}" }, task.description.takeIf { it.isNotBlank() }).joinToString("  •  "); if (details.isNotBlank()) row.addView(text(requireContext(), details, 13f)); row.setOnClickListener { (activity as MainActivity).showEditor(task) }; box.addView(row); box.addView(View(requireContext()).apply { setBackgroundColor(0xFFE0E5E1.toInt()) }, LinearLayout.LayoutParams(-1, 1)) } }
}
