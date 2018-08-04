package soko.ekibun.bangumi.ui.main.fragment

import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v4.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.*
import soko.ekibun.bangumi.R

abstract class DrawerFragment(@LayoutRes private val resId: Int): Fragment(){
    abstract val titleRes: Int

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(resId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (view.findViewById(R.id.toolbar) as? Toolbar)?.let{
            (activity as? AppCompatActivity)?.setSupportActionBar(it)
            val toggle = ActionBarDrawerToggle(activity, activity?.drawer_layout, it,
                    R.string.navigation_drawer_open, R.string.navigation_drawer_close)
            activity?.drawer_layout?.addDrawerListener(toggle)
            toggle.syncState()
        }

        activity?.setTitle(titleRes)
    }

    var savedInstanceState:Bundle? = null
    fun onRestoreInstanceState(savedInstanceState: Bundle){
        this.savedInstanceState = savedInstanceState
    }
}