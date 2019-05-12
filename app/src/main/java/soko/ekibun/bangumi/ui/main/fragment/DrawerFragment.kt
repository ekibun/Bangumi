package soko.ekibun.bangumi.ui.main.fragment

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.*
import soko.ekibun.bangumi.R

abstract class DrawerFragment(@LayoutRes private val resId: Int): androidx.fragment.app.Fragment(){
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

    open fun processBack(): Boolean{
        return false
    }
}