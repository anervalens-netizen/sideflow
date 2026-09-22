package eu.astancu.sideflow

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import eu.astancu.sideflow.databinding.ActivityShelfEditorBinding

class ShelfEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShelfEditorBinding
    private lateinit var prefs: PanelPreferences
    private lateinit var adapter: SectionAdapter

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShelfEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PanelPreferences(this)
        adapter = SectionAdapter()

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.rvSections.layoutManager = LinearLayoutManager(this)
        binding.rvSections.adapter = adapter

        binding.btnAddSection.setOnClickListener {
            val index = prefs.getShelfConfig().sections.size + 1
            val sectionId = prefs.addSection("Section $index")
            refresh()
            prefs.getShelfConfig().sections.firstOrNull { it.id == sectionId }?.let(::showEditDialog)
        }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        adapter.submit(prefs.getShelfConfig().sections)
    }

    private fun notifyShelfChanged() {
        val intent = Intent(this, FloatingPanelService::class.java).apply {
            action = FloatingPanelService.ACTION_REFRESH
        }
        runCatching { startService(intent) }
    }

    private fun showEditDialog(section: ShelfSection) {
        val density = resources.displayMetrics.density
        val padding = (20 * density).toInt()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, 0, padding, 0)
        }

        val titleInput = EditText(this).apply {
            setText(section.title)
            hint = "Section title"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            selectAll()
        }

        val columnsLabel = TextView(this).apply {
            text = "Columns: ${section.columns}"
            setPadding(0, padding, 0, 0)
        }

        val columnsSlider = Slider(this).apply {
            valueFrom = SideFlowPolicy.MIN_COLUMNS.toFloat()
            valueTo = SideFlowPolicy.MAX_COLUMNS.toFloat()
            stepSize = 1f
            value = section.columns.toFloat()
            addOnChangeListener { _, value, _ ->
                columnsLabel.text = "Columns: ${value.toInt()}"
            }
        }

        val showTitleSwitch = MaterialSwitch(this).apply {
            text = "Show section title"
            isChecked = section.showTitle
        }

        container.addView(titleInput)
        container.addView(columnsLabel)
        container.addView(columnsSlider)
        container.addView(showTitleSwitch)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Edit section")
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Save", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = titleInput.text?.toString()?.trim().orEmpty()
                if (title.isBlank()) {
                    titleInput.error = "Title cannot be empty"
                    return@setOnClickListener
                }

                prefs.updateSection(
                    sectionId = section.id,
                    title = title,
                    columns = columnsSlider.value.toInt(),
                    showTitle = showTitleSwitch.isChecked
                )
                dialog.dismiss()
                refresh()
                notifyShelfChanged()
            }
        }

        dialog.show()
    }

    private fun deleteSection(section: ShelfSection) {
        val config = prefs.getShelfConfig()
        if (config.sections.size <= 1) {
            binding.root.showModernToast("At least one section is required")
            return
        }

        val message = if (section.items.isEmpty()) {
            "Delete “${section.title}”?"
        } else {
            "Delete “${section.title}”? Its ${section.items.size} item(s) will be moved to a neighbouring section."
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Delete section")
            .setMessage(message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Delete") { _, _ ->
                prefs.removeSection(section.id)
                refresh()
                notifyShelfChanged()
            }
            .show()
    }

    private inner class SectionAdapter : RecyclerView.Adapter<SectionAdapter.ViewHolder>() {
        private var sections: List<ShelfSection> = emptyList()

        fun submit(newSections: List<ShelfSection>) {
            sections = newSections
            notifyDataSetChanged()
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val title: TextView = view.findViewById(R.id.tvTitle)
            private val summary: TextView = view.findViewById(R.id.tvSummary)
            private val edit: MaterialButton = view.findViewById(R.id.btnEdit)
            private val up: MaterialButton = view.findViewById(R.id.btnUp)
            private val down: MaterialButton = view.findViewById(R.id.btnDown)
            private val delete: MaterialButton = view.findViewById(R.id.btnDelete)

            fun bind(section: ShelfSection, position: Int) {
                title.text = section.title
                summary.text = "${section.items.size} item(s) • ${section.columns} columns" +
                    if (section.showTitle) "" else " • title hidden"

                up.isEnabled = position > 0
                down.isEnabled = position < sections.lastIndex
                delete.isEnabled = sections.size > 1

                edit.setOnClickListener { showEditDialog(section) }
                itemView.setOnClickListener { showEditDialog(section) }

                up.setOnClickListener {
                    if (position > 0) {
                        prefs.moveSection(position, position - 1)
                        refresh()
                        notifyShelfChanged()
                    }
                }

                down.setOnClickListener {
                    if (position < sections.lastIndex) {
                        prefs.moveSection(position, position + 1)
                        refresh()
                        notifyShelfChanged()
                    }
                }

                delete.setOnClickListener { deleteSection(section) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_shelf_section, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(sections[position], position)
        }

        override fun getItemCount(): Int = sections.size
    }
}
