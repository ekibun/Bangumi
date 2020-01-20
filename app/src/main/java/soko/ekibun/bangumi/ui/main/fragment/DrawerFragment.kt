package soko.ekibun.bangumi.ui.main.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import kotlinx.android.synthetic.main.activity_main.*
import soko.ekibun.bangumi.R

/**
 * 抽屉fragment
 */
abstract class DrawerFragment(@LayoutRes private val resId: Int): androidx.fragment.app.Fragment(){
    abstract val titleRes: Int

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(resId, container, false)
    }

    var toggle: ActionBarDrawerToggle? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (view.findViewById(R.id.toolbar) as? Toolbar)?.let{
            (activity as? AppCompatActivity)?.setSupportActionBar(it)
            toggle = ActionBarDrawerToggle(activity, activity?.drawer_layout, it,
                    R.string.navigation_drawer_open, R.string.navigation_drawer_close)
            activity?.drawer_layout?.addDrawerListener(toggle!!)
            toggle?.isDrawerIndicatorEnabled = activity?.resources?.configuration?.orientation != Configuration.ORIENTATION_LANDSCAPE
            toggle?.syncState()
        }

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