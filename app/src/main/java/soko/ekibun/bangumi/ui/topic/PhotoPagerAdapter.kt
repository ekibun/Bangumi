package soko.ekibun.bangumi.ui.topic

import android.graphics.drawable.Drawable
import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup
import soko.ekibun.bangumi.ui.view.DragPhotoView

class PhotoPagerAdapter(private val items: List<Drawable>, private val onDismiss: ()->Unit): PagerAdapter(){

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val photoView = DragPhotoView(container.context)
        photoView.isEnabled = true
        photoView.setImageDrawable(items[position])
        photoView.mExitListener = { onDismiss() }
        photoView.mTapListener = { onDismiss() }
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