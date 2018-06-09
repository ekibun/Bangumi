package soko.ekibun.bangumi.ui.video

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.support.design.widget.AppBarLayout
import android.view.View
import kotlinx.android.synthetic.main.activity_video.*

class SystemUIPresenter(private val context: VideoActivity){
    fun init(){}

    init{
        context.window.statusBarColor = Color.BLACK
        context.window.navigationBarColor = Color.BLACK
        context.window.decorView.setOnSystemUiVisibilityChangeListener{
            if(it == 0)
                if(isLandscape) setSystemUiVisibility(Visibility.FULLSCREEN)
                else setSystemUiVisibility(Visibility.NORMAL)
        }
    }

    fun appbarCollapsible(enable:Boolean){
        //context.nested_scroll.tag = true
        if(enable){
            //reactive appbar
            val params = context.toolbar_layout.layoutParams as AppBarLayout.LayoutParams
            params.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
            context.toolbar_layout.layoutParams = params
        }else{
            //expand appbar
            context.app_bar.setExpanded(true)
            (context.toolbar_layout.layoutParams as AppBarLayout.LayoutParams).scrollFlags = 0
            context.toolbar_layout.isTitleEnabled = false
        }
    }

    fun onWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration?) {
        if (newConfig?.orientation == Configuration.ORIENTATION_LANDSCAPE && (Build.VERSION.SDK_INT <24 || !isInMultiWindowMode)) {
            isLandscape = true
            setSystemUiVisibility(SystemUIPresenter.Visibility.FULLSCREEN)
        }else if (newConfig?.orientation == Configuration.ORIENTATION_PORTRAIT){
            isLandscape = false
            setSystemUiVisibility(SystemUIPresenter.Visibility.NORMAL)
        }
    }

    var isLandscape = false
    private fun setSystemUiVisibility(visibility: Visibility){
        when(visibility){
            SystemUIPresenter.Visibility.FULLSCREEN -> {
                //context.window.statusBarColor = Color.TRANSPARENT
                //context.window.navigationBarColor = Color.TRANSPARENT
                context.window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        //or View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }
            SystemUIPresenter.Visibility.IMMERSIVE -> {
                //context.window.statusBarColor = Color.TRANSPARENT
                //context.window.navigationBarColor = Color.TRANSPARENT
                context.window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE)
            }
            SystemUIPresenter.Visibility.NORMAL -> {
                //context.window.statusBarColor = Color.BLACK
                //context.window.navigationBarColor = Color.BLACK
                context.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    enum class Visibility{
        FULLSCREEN, IMMERSIVE, NORMAL
    }
}