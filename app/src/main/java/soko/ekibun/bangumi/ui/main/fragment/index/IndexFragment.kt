package soko.ekibun.bangumi.ui.main.fragment.index

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.content_index.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.main.fragment.DrawerFragment
import java.util.*

class IndexFragment: DrawerFragment(R.layout.content_index){
    override val titleRes: Int = R.string.index

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        item_pager?.adapter = IndexPagerAdapter(this, item_pager){
            app_bar.isPressed = it
            item_tab_container.isPressed = it
        }
        item_tabs?.setUpWithViewPager(item_pager)

        item_pager?.currentItem = getNowIndex()
    }

    override fun processBack(): Boolean{
        val index = getNowIndex()
        if(item_pager == null || item_pager?.currentItem == index) return false
        item_pager?.currentItem = index
        return true
    }

    private fun getNowIndex(): Int{
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
       return (year - 1000) * 12 + month
    }
}