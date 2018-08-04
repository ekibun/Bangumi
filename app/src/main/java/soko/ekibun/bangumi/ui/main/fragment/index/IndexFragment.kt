package soko.ekibun.bangumi.ui.main.fragment.index

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.content_index.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.main.fragment.DrawerFragment
import java.util.*

class IndexFragment: DrawerFragment(R.layout.content_index){
    override val titleRes: Int = R.string.index

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        item_pager?.adapter = IndexPagerAdapter(this, item_pager)
        item_tabs?.setUpWithViewPager(item_pager)

        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        item_pager?.currentItem = (year - 1000) * 12 + month
    }
}