package soko.ekibun.bangumi.ui.main.fragment.index

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v7.widget.PopupMenu
import android.view.Menu
import android.view.View
import com.nshmura.recyclertablayout.RecyclerTabLayout
import kotlinx.android.synthetic.main.content_collection.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.SubjectType
import soko.ekibun.bangumi.ui.main.fragment.DrawerFragment
import java.util.*

class IndexFragment: DrawerFragment(R.layout.content_collection){
    override val titleRes: Int = R.string.index
    override val showTab: Boolean = true

    val typeList = mapOf(
            R.id.collection_type_anime to Pair(SubjectType.ANIME, SubjectType.NAME_ANIME),
            R.id.collection_type_book to Pair(SubjectType.BOOK, SubjectType.NAME_BOOK),
            R.id.collection_type_game to Pair(SubjectType.GAME, SubjectType.NAME_GAME),
            R.id.collection_type_music to Pair(SubjectType.MUSIC, SubjectType.NAME_MUSIC),
            R.id.collection_type_real to Pair(SubjectType.REAL, SubjectType.NAME_REAL))
    var selectedType = R.id.collection_type_anime

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        collection_pager?.adapter = IndexPagerAdapter(this, collection_pager)
        activity?.findViewById<RecyclerTabLayout>(R.id.tab_layout)?.setUpWithViewPager(collection_pager)

        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        collection_pager.currentItem = (year - 1000) * 12 + month
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val item = menu.findItem(R.id.action_type)?:return
        item.title = SubjectType.getDescription(typeList[selectedType]?.first?:0)
        item.setOnMenuItemClickListener {
            //Log.v("it", "click")
            val context = activity?:return@setOnMenuItemClickListener true
            val popup = PopupMenu(context, context.findViewById(R.id.action_type))
            popup.menuInflater.inflate(R.menu.list_collection_type, popup.menu)
            popup.setOnMenuItemClickListener{
                selectedType = it.itemId
                item.title = it.title
                (collection_pager?.adapter as? IndexPagerAdapter)?.reset()
                true
            }
            popup.show()
            true
        }
    }
}