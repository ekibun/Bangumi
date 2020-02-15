package soko.ekibun.bangumi.ui.main.fragment.home.fragment.collection

import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Collection

/**
 * 收藏类型
 * @property selectedType Int
 * @constructor
 */
class CollectTypeView(view: TextView, onChange: () -> Unit) {
    var selectedType = Collection.statusArray.indexOf(Collection.STATUS_DO)

    /**
     * 获取当前收藏类型
     * @return String
     */
    fun getType(): String {
        return Collection.statusArray[selectedType]
    }

    init {
        val popup = PopupMenu(view.context, view)
        val stringArray = view.context.resources.getStringArray(R.array.collection_status_anime)
        stringArray.forEach {
            popup.menu.add(it)
        }
        view.text = stringArray[selectedType]
        view.setOnClickListener {
            popup.setOnMenuItemClickListener {
                selectedType = stringArray.indexOf(it.title)
                view.text = it.title
                onChange()
                true
            }
            popup.show()
        }
    }
}