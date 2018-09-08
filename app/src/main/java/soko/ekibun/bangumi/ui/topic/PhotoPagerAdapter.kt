package soko.ekibun.bangumi.ui.topic

import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.gif.GifDrawable
import soko.ekibun.bangumi.ui.view.DragPhotoView

class PhotoPagerAdapter(private val items: List<String>, private val onDismiss: ()->Unit): PagerAdapter(){

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val photoView = DragPhotoView(container.context)
        photoView.isEnabled = true
        Glide.with(container).load(items[position]).into(photoView)
        //photoView.setImageDrawable(items[position])
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