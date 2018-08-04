package soko.ekibun.bangumi.ui.main.fragment.home.fragment

import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v4.app.Fragment
import android.view.*

abstract class HomeTabFragment(@LayoutRes private val resId: Int): Fragment(){
    abstract val titleRes: Int
    abstract val iconRes: Int

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(resId, container, false)
    }

    var savedInstanceState:Bundle? = null
}