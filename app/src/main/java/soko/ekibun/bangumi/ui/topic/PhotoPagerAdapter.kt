package soko.ekibun.bangumi.ui.topic

import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.request.RequestOptions
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.view.DragPhotoView
import soko.ekibun.bangumi.ui.view.FixMultiViewPager
import soko.ekibun.bangumi.util.AppUtil
import soko.ekibun.bangumi.util.GlideUtil

/**
 * 图片浏览页 Adapter
 * @property items List<String>
 * @property thumbs List<Drawable>?
 * @property onDismiss Function0<Unit>
 * @constructor
 */
class PhotoPagerAdapter(private val items: List<String>, private val thumbs: List<Drawable>?, private val onDismiss: () -> Unit) : androidx.viewpager.widget.PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val photoView = DragPhotoView(container.context)
        photoView.isEnabled = true

        GlideUtil.loadWithProgress(items[position], photoView,
                RequestOptions().placeholder(thumbs?.getOrNull(position)),
                photoView.circularProgressDrawable) { type, drawable ->
            photoView.circularProgressDrawable.setVisible(type == GlideUtil.TYPE_PLACEHOLDER, false)
            photoView.updateDrawable(drawable)
        }

        photoView.mExitListener = { onDismiss() }
        photoView.mTapListener = { onDismiss() }
        photoView.mLongClickListener = {
            val systemUiVisibility = container.systemUiVisibility
            val dialog = AlertDialog.Builder(container.context)
                    .setTitle(items[position])
                    .setItems(arrayOf(container.context.getString(R.string.share)))
                    { _, _ ->
                        AppUtil.shareDrawable(container.context, photoView.drawable ?: return@setItems)
                    }.setOnDismissListener {
                        container.systemUiVisibility = systemUiVisibility
                    }.create()
            dialog.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
            dialog.show()
        }
        container.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        return photoView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView((`object` as? View) ?: return)
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    companion object {
        /**
         * 显示对话框
         * @param view View
         * @param items List<String>
         * @param thumbs List<Drawable>?
         * @param index Int
         */
        fun showWindow(view: View, items: List<String>, thumbs: List<Drawable>? = null, index: Int = 0) {
            val popWindow =
                PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true)
            val viewPager = FixMultiViewPager(view.context)
            popWindow.contentView = viewPager
            viewPager.adapter = PhotoPagerAdapter(items, thumbs) {
                popWindow.dismiss()
            }
            viewPager.currentItem = index
            popWindow.isClippingEnabled = false
            popWindow.animationStyle = R.style.AppTheme_FadeInOut
            popWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
            popWindow.contentView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }
}