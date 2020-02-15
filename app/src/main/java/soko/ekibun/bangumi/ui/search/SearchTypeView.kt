package soko.ekibun.bangumi.ui.search

import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Subject

/**
 * 搜索类型
 * @property subjectTypeList Map<Int, String>
 * @property monoTypeList Map<Int, String>
 * @property selectedType Int
 * @constructor
 */
class SearchTypeView(view: TextView, onChange:()->Unit){
    val subjectTypeList = mapOf(
            R.id.collection_type_all to Subject.TYPE_ANY,
            R.id.collection_type_anime to Subject.TYPE_ANIME,
            R.id.collection_type_book to Subject.TYPE_BOOK,
            R.id.collection_type_game to Subject.TYPE_GAME,
            R.id.collection_type_music to Subject.TYPE_MUSIC,
            R.id.collection_type_real to Subject.TYPE_REAL)
    val monoTypeList = mapOf(
            R.id.collection_type_mono to "all",
            R.id.collection_type_ctr to "crt",
            R.id.collection_type_psn to "prsn"
    )
    var selectedType = R.id.collection_type_all

    init{
        val context = ContextThemeWrapper(view.context, R.style.AppTheme_PopupOverlay)
        val popup = PopupMenu(context, view)
        popup.menuInflater.inflate(R.menu.list_search_type, popup.menu)
        view.text = popup.menu.findItem(selectedType)?.title
        view.setOnClickListener {
            popup.setOnMenuItemClickListener{
                selectedType = it.itemId
                view.text = it.title
                onChange()
                true
            }
            popup.show()
        }
    }
}