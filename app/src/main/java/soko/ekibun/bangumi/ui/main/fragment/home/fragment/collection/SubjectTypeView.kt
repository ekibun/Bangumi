package soko.ekibun.bangumi.ui.main.fragment.home.fragment.collection

import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Subject

class SubjectTypeView(view: TextView, onChange: () -> Unit) {
    private val typeList = mapOf(
            R.id.collection_type_anime to Pair(Subject.TYPE_ANIME, Subject.TYPE_NAME_ANIME),
            R.id.collection_type_book to Pair(Subject.TYPE_BOOK, Subject.TYPE_NAME_BOOK),
            R.id.collection_type_game to Pair(Subject.TYPE_GAME, Subject.TYPE_NAME_GAME),
            R.id.collection_type_music to Pair(Subject.TYPE_MUSIC, Subject.TYPE_NAME_MUSIC),
            R.id.collection_type_real to Pair(Subject.TYPE_REAL, Subject.TYPE_NAME_REAL))
    var selectedType = R.id.collection_type_anime

    fun getTypeName(): String {
        return typeList[selectedType]?.second ?: Subject.TYPE_NAME_ANIME
    }

    fun getType(): Int {
        return typeList[selectedType]?.first ?: 0
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