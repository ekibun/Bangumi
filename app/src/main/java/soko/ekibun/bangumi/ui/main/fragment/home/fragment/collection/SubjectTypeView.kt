package soko.ekibun.bangumi.ui.main.fragment.home.fragment.collection

import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Subject

/**
 * 条目类型
 */
class SubjectTypeView(view: TextView, onChange: () -> Unit) {
    private val typeList = mapOf(
            R.id.collection_type_anime to Subject.TYPE_ANIME,
            R.id.collection_type_book to Subject.TYPE_BOOK,
            R.id.collection_type_game to Subject.TYPE_GAME,
            R.id.collection_type_music to Subject.TYPE_MUSIC,
            R.id.collection_type_real to Subject.TYPE_REAL)
    var selectedType = R.id.collection_type_anime

    /**
     * 获取当前条目类型
     */
    fun getType(): String {
        return typeList[selectedType] ?: Subject.TYPE_ANY
    }

    init {
        val popup = PopupMenu(view.context, view)
        popup.menuInflater.inflate(R.menu.list_collection_type, popup.menu)
        view.text = popup.menu.findItem(selectedType)?.title
        view.setOnClickListener {
            popup.setOnMenuItemClickListener {
                selectedType = it.itemId
                view.text = it.title
                onChange()
                true
            }
            popup.show()
        }
    }
}