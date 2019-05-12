package soko.ekibun.bangumi.ui.main.fragment.home.fragment.collection

import androidx.appcompat.widget.PopupMenu
import android.widget.TextView
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.SubjectType

class SubjectTypeView(view: TextView, onChange:()->Unit){
    private val typeList = mapOf(
            R.id.collection_type_anime to Pair(SubjectType.ANIME, SubjectType.NAME_ANIME),
            R.id.collection_type_book to Pair(SubjectType.BOOK, SubjectType.NAME_BOOK),
            R.id.collection_type_game to Pair(SubjectType.GAME, SubjectType.NAME_GAME),
            R.id.collection_type_music to Pair(SubjectType.MUSIC, SubjectType.NAME_MUSIC),
            R.id.collection_type_real to Pair(SubjectType.REAL, SubjectType.NAME_REAL))
    var selectedType = R.id.collection_type_anime

    fun getTypeName(): String{
        return typeList[selectedType]?.second?:SubjectType.NAME_ANIME
    }

    fun getType(): Int{
        return typeList[selectedType]?.first?:0
    }

    init{
        val popup = PopupMenu(view.context, view)
        popup.menuInflater.inflate(R.menu.list_collection_type, popup.menu)
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