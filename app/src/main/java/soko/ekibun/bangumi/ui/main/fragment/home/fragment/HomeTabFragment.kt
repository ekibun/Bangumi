package soko.ekibun.bangumi.ui.main.fragment.home.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

/**
 * 主页fragment
 */
abstract class HomeTabFragment(@LayoutRes private val resId: Int): androidx.fragment.app.Fragment(){
    abstract val titleRes: Int
    abstract val iconRes: Int

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(resId, container, false)
    }

    /**
     * 选中
     */
    abstract fun onSelect()

    var savedInstanceState:Bundle? = null
}