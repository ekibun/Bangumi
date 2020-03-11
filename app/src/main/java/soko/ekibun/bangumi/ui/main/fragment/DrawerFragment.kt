package soko.ekibun.bangumi.ui.main.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

/**
 * 抽屉fragment
 * @property resId Int
 * @property titleRes Int
 * @constructor
 */
abstract class DrawerFragment(@LayoutRes private val resId: Int): androidx.fragment.app.Fragment(){
    abstract val titleRes: Int

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(resId, container, false)
    }

    /**
     * 返回处理
     * @return Boolean
     */
    open fun processBack(): Boolean {
        return false
    }

    open fun onCreateOptionsMenu(menu: Menu) {}
}