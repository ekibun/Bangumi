package soko.ekibun.bangumi.ui.topic

import androidx.appcompat.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.ui.view.DragPhotoView
import soko.ekibun.bangumi.util.GlideUtil

class PhotoPagerAdapter(private val items: List<String>, private val onDismiss: ()->Unit): androidx.viewpager.widget.PagerAdapter(){

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val photoView = DragPhotoView(container.context)
        photoView.isEnabled = true
        GlideUtil.with(photoView)?.load(items[position])?.into(photoView.glideTarget)
        photoView.mExitListener = { onDismiss() }
        photoView.mTapListener = { onDismiss() }
        photoView.mLongClickListener = {
            val systemUiVisibility = container.systemUiVisibility
            AlertDialog.Builder(container.context)
                    .setItems(arrayOf(container.context.getString(R.string.share)))
                    { _, _ ->
                        //AppUtil.shareDrawable(container.context, photoView.drawable)
                    }.setOnDismissListener {
                        container.systemUiVisibility = systemUiVisibility
                    }.show()
        }
        container.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        return photoView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }
}