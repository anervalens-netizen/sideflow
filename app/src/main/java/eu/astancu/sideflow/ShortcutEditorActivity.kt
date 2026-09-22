package eu.astancu.sideflow

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.astancu.sideflow.databinding.ActivityShortcutEditorBinding
import java.util.UUID

class ShortcutEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ITEM_ID = "extra_shelf_item_id"
        private const val STATE_EDITING_ITEM_ID = "state_editing_item_id"
        private const val STATE_SECTION_ID = "state_section_id"
        private const val STATE_ICON_PACKAGE = "state_icon_package"
        private const val STATE_TITLE_AUTOFILLED = "state_title_autofilled"
    }

    private lateinit var binding: ActivityShortcutEditorBinding
    private lateinit var prefs: PanelPreferences

    private var editingItemId: String? = null
    private var selectedSectionId: String? = null
    private var selectedIconPackage: String? = null
    private var titleWasAutofilled = false

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShortcutEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PanelPreferences(this)

        binding.toolbar.setNavigationOnClickListener { finish() }

        editingItemId = if (savedInstanceState?.containsKey(STATE_EDITING_ITEM_ID) == true) {
            savedInstanceState.getString(STATE_EDITING_ITEM_ID)
        } else {
            intent.getStringExtra(EXTRA_ITEM_ID)
        }
        val existing = editingItemId?.let(prefs::getShelfItem)
        selectedSectionId = if (savedInstanceState?.containsKey(STATE_SECTION_ID) == true) {
            savedInstanceState.getString(STATE_SECTION_ID)
        } else {
            findSectionId(editingItemId)
                ?: prefs.getShelfConfig().sections.firstOrNull()?.id
        }
        selectedIconPackage = if (savedInstanceState?.containsKey(STATE_ICON_PACKAGE) == true) {
            savedInstanceState.getString(STATE_ICON_PACKAGE)
        } else {
            existing?.iconPackage
        }

        val sharedTarget = if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            ShortcutPolicy.extractTarget(intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString())
        } else {
            null
        }

        val initialTarget = existing?.reference ?: sharedTarget.orEmpty()
        binding.etTarget.setText(initialTarget)

        val sharedTitle = intent.getStringExtra(Intent.EXTRA_TITLE)
            ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
        val initialTitle = existing?.label
            ?: sharedTitle?.trim()?.takeIf { it.isNotBlank() }
            ?: initialTarget.takeIf { it.isNotBlank() }?.let(ShortcutPolicy::suggestedLabel)
            ?: ""

        binding.etTitle.setText(initialTitle)
        titleWasAutofilled = if (savedInstanceState?.containsKey(STATE_TITLE_AUTOFILLED) == true) {
            savedInstanceState.getBoolean(STATE_TITLE_AUTOFILLED)
        } else {
            existing == null && initialTitle.isNotBlank() && sharedTitle.isNullOrBlank()
        }

        binding.toolbar.title = if (existing == null) "Add shortcut" else "Edit shortcut"
        binding.btnSave.text = if (existing == null) "Save shortcut" else "Update shortcut"
        binding.btnDelete.visibility = if (existing == null) View.GONE else View.VISIBLE

        updateSectionButton()
        updateIconButton()
        updateTargetType()

        binding.etTarget.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateTargetType()
                val normalized = ShortcutPolicy.normalizeTarget(s?.toString())
                if (normalized != null && (binding.etTitle.text.isNullOrBlank() || titleWasAutofilled)) {
                    binding.etTitle.setText(ShortcutPolicy.suggestedLabel(normalized))
                    binding.etTitle.setSelection(binding.etTitle.text?.length ?: 0)
                    titleWasAutofilled = true
                }
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        binding.etTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (binding.etTitle.hasFocus()) titleWasAutofilled = false
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        binding.btnSection.setOnClickListener { chooseSection() }
        binding.btnIcon.setOnClickListener { chooseIconSource() }
        binding.btnSave.setOnClickListener { save() }
        binding.btnDelete.setOnClickListener { deleteCurrentShortcut() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_EDITING_ITEM_ID, editingItemId)
        outState.putString(STATE_SECTION_ID, selectedSectionId)
        outState.putString(STATE_ICON_PACKAGE, selectedIconPackage)
        outState.putBoolean(STATE_TITLE_AUTOFILLED, titleWasAutofilled)
        super.onSaveInstanceState(outState)
    }

    private fun findSectionId(itemId: String?): String? {
        if (itemId == null) return null
        return prefs.getShelfConfig().sections.firstOrNull { section ->
            section.items.any { it.id == itemId }
        }?.id
    }

    private fun updateTargetType() {
        val target = ShortcutPolicy.normalizeTarget(binding.etTarget.text?.toString())
        binding.layoutTarget.error = if (binding.etTarget.text.isNullOrBlank() || target != null) {
            null
        } else {
            "Enter a valid URL or deep link"
        }

        binding.tvTargetType.text = when (target?.let(ShortcutPolicy::classify)) {
            ShelfItemType.URL -> "Web link • opens with Android's matching app"
            ShelfItemType.DEEP_LINK -> "Deep link • opens with Android's matching app"
            else -> "Paste or share a URL/deep link"
        }
    }

    private fun updateSectionButton() {
        val section = prefs.getShelfConfig().sections.firstOrNull { it.id == selectedSectionId }
            ?: prefs.getShelfConfig().sections.firstOrNull()
        if (section != null) selectedSectionId = section.id
        binding.btnSection.text = "Section: ${section?.title ?: "Apps"}"
    }

    private fun updateIconButton() {
        if (selectedIconPackage == null) {
            binding.btnIcon.text = "Icon: Automatic"
            return
        }

        val label = runCatching {
            val info = packageManager.getApplicationInfo(selectedIconPackage!!, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrNull() ?: selectedIconPackage

        binding.btnIcon.text = "Icon: $label"
    }

    private fun chooseSection() {
        val sections = prefs.getShelfConfig().sections
        if (sections.isEmpty()) return
        val selected = sections.indexOfFirst { it.id == selectedSectionId }.coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle("Choose section")
            .setSingleChoiceItems(sections.map { it.title }.toTypedArray(), selected) { dialog, which ->
                selectedSectionId = sections[which].id
                updateSectionButton()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun chooseIconSource() {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = packageManager.queryIntentActivities(launcherIntent, 0)
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }

        val labels = buildList {
            add("Automatic")
            apps.forEach { resolveInfo ->
                val label = resolveInfo.loadLabel(packageManager).toString()
                add("$label  ·  ${resolveInfo.activityInfo.packageName}")
            }
        }.toTypedArray()

        val currentIndex = selectedIconPackage?.let { pkg ->
            apps.indexOfFirst { it.activityInfo.packageName == pkg }
                .takeIf { it >= 0 }
                ?.plus(1)
        } ?: 0

        MaterialAlertDialogBuilder(this)
            .setTitle("Icon source")
            .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                selectedIconPackage = if (which == 0) null else apps[which - 1].activityInfo.packageName
                updateIconButton()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun deleteCurrentShortcut() {
        val itemId = editingItemId ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete shortcut")
            .setMessage("Remove this shortcut from SideFlow?")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Delete") { _, _ ->
                prefs.removeShelfItem(itemId)
                notifyShelfChanged()
                finish()
            }
            .show()
    }

    private fun notifyShelfChanged() {
        val refreshIntent = Intent(this, FloatingPanelService::class.java).apply {
            action = FloatingPanelService.ACTION_REFRESH
        }
        runCatching { startService(refreshIntent) }
    }

    private fun save() {
        val target = ShortcutPolicy.normalizeTarget(binding.etTarget.text?.toString())
        if (target == null) {
            binding.layoutTarget.error = "Enter a valid URL or deep link"
            binding.etTarget.requestFocus()
            return
        }

        val title = binding.etTitle.text?.toString()?.trim().orEmpty()
            .ifBlank { ShortcutPolicy.suggestedLabel(target) }

        val type = ShortcutPolicy.classify(target)
        val itemId = editingItemId ?: UUID.randomUUID().toString()
        val item = ShelfItem(
            id = itemId,
            type = type,
            reference = target,
            label = title,
            iconPackage = selectedIconPackage
        )

        if (editingItemId == null) {
            prefs.addShelfItem(item, selectedSectionId)
        } else {
            prefs.updateShelfItem(itemId, item, selectedSectionId)
        }

        notifyShelfChanged()

        binding.root.showModernToast(if (editingItemId == null) "Shortcut added" else "Shortcut updated")
        finish()
    }
}
