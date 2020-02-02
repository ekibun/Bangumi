package soko.ekibun.bangumi.ui.main.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

/**
 * 抽屉fragment
 */
abstract class DrawerFragment(@LayoutRes private val resId: Int): androidx.fragment.app.Fragment(){
    abstract val titleRes: Int

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(resId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.setTitle(titleRes)
    }

    var savedInstanceState:Bundle? = null
    /**
     * 恢复状态
     */
    fun onRestoreInstanceState(savedInstanceState: Bundle){
        this.savedInstanceState = savedInstanceState
    }

    /**
     * 返回处理
     */
    open fun processBack(): Boolean{
        return false
    }
}