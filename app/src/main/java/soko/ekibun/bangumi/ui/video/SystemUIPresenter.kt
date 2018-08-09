package soko.ekibun.bangumi.ui.video

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.support.design.widget.AppBarLayout
import android.view.View
import android.view.WindowManager
import kotlinx.android.synthetic.main.activity_video.*

class SystemUIPresenter(private val context: VideoActivity){
    fun init(){
        setSystemUiVisibility(Visibility.IMMERSIVE)
    }

    init{
        if(Build.VERSION.SDK_INT >= 28)
            context.window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        //context.window.statusBarColor = Color.BLACK
        //context.window.navigationBarColor = Color.BLACK
        context.window.decorView.setOnSystemUiVisibilityChangeListener{
            if(it == 0)
                if(isLandscape) setSystemUiVisibility(Visibility.FULLSCREEN)
                else setSystemUiVisibility(Visibility.IMMERSIVE)
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
            setSystemUiVisibility(SystemUIPresenter.Visibility.IMMERSIVE)
        }
    }

    var isLandscape = false
    private fun setSystemUiVisibility(visibility: Visibility){
        when(visibility){
            SystemUIPresenter.Visibility.FULLSCREEN -> {
                context.root_layout.fitsSystemWindows=false
                context.app_bar.fitsSystemWindows=false
                context.toolbar_layout.fitsSystemWindows = false
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
                context.root_layout.fitsSystemWindows=true
                context.app_bar.fitsSystemWindows=true
                context.toolbar_layout.fitsSystemWindows = true
                //context.window.statusBarColor = Color.TRANSPARENT
                //context.window.navigationBarColor = Color.TRANSPARENT
                context.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
        }
        context.toolbar_layout.post{
            context.toolbar_layout.requestLayout()
        }
    }

    enum class Visibility{
        FULLSCREEN, IMMERSIVE
    }
}