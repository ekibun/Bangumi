package soko.ekibun.bangumi.ui.main.fragment.home.fragment

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import android.view.*

abstract class HomeTabFragment(@LayoutRes private val resId: Int): androidx.fragment.app.Fragment(){
    abstract val titleRes: Int
    abstract val iconRes: Int

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(resId, container, false)
    }

    abstract fun onSelect()

    var savedInstanceState:Bundle? = null
}