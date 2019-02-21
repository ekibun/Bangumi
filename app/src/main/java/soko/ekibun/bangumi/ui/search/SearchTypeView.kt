package soko.ekibun.bangumi.ui.search

import android.support.v7.view.ContextThemeWrapper
import android.support.v7.widget.PopupMenu
import android.widget.TextView
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.SubjectType

class SearchTypeView(view: TextView, onChange:()->Unit){
    val subjectTypeList = mapOf(
            R.id.collection_type_all to SubjectType.ALL,
            R.id.collection_type_anime to SubjectType.ANIME,
            R.id.collection_type_book to SubjectType.BOOK,
            R.id.collection_type_game to SubjectType.GAME,
            R.id.collection_type_music to SubjectType.MUSIC,
            R.id.collection_type_real to SubjectType.REAL)
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