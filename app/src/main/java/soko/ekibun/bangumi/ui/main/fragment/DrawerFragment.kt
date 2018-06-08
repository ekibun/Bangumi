package soko.ekibun.bangumi.ui.main.fragment

import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.view.*
import soko.ekibun.bangumi.R

abstract class DrawerFragment(@LayoutRes private val resId: Int): Fragment(){
    abstract val titleRes: Int
    abstract val showTab: Boolean
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(resId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.findViewById<TabLayout>(R.id.tab_layout)?.visibility =
                if(showTab) View.VISIBLE else View.GONE
        activity?.setTitle(titleRes)
    }

    var savedInstanceState:Bundle? = null
    fun onRestoreInstanceState(savedInstanceState: Bundle){
        this.savedInstanceState = savedInstanceState
    }
}