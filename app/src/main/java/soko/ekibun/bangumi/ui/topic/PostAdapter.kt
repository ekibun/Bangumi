package soko.ekibun.bangumi.ui.topic

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.text.*
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_reply.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.TopicPost
import soko.ekibun.bangumi.ui.view.FixMultiViewPager
import soko.ekibun.bangumi.util.HtmlHttpImageGetter
import soko.ekibun.bangumi.util.HtmlTagHandler
import java.net.URI
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.util.Log
import android.util.SparseIntArray
import android.widget.TextView
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import org.jsoup.Jsoup
import soko.ekibun.bangumi.ui.web.WebActivity
import soko.ekibun.bangumi.util.HttpUtil
import java.util.concurrent.Executors

class PostAdapter(private val recyclerView: FastScrollRecyclerView, data: MutableList<TopicPost>? = null) :
        BaseQuickAdapter<TopicPost, BaseViewHolder>(R.layout.item_reply, data), FastScrollRecyclerView.MeasurableAdapter<BaseViewHolder>, FastScrollRecyclerView.SectionedAdapter {
    private fun updateScrollOffsets(item: TopicPost, height: Int){
        try{
            val soField = FastScrollRecyclerView::class.java.getDeclaredField("mScrollOffsets")
            soField.isAccessible = true
            val mScrollOffsets = soField.get(recyclerView) as SparseIntArray
            val position = data.indexOf(item)
            val index = mScrollOffsets.indexOfKey(position)
            if(index >= 0){
                val delta = height - (itemHeight.get(getItemViewType(position),0))
                itemHeight.put(getItemViewType(position), height)
                Log.v("delta", delta.toString())
                for(i in index + 1 until mScrollOffsets.size()){
                    Log.v("key", mScrollOffsets.keyAt(i).toString())
                    mScrollOffsets.put(mScrollOffsets.keyAt(i), mScrollOffsets.valueAt(i) + delta)
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private val itemHeight = SparseIntArray()
    override fun getViewTypeHeight(recyclerView: RecyclerView?, viewHolder: BaseViewHolder?, viewType: Int): Int {
        var height = viewHolder?.itemView?.height?:-1
        if(height != -1){
            itemHeight.put(viewType, height)
        }else{
            height = itemHeight.get(viewType, -1)
            if(height == -1){
                height = if(viewType < 0x00001000) 0 else recyclerView?.context?.resources?.getDimensionPixelSize(R.dimen.reply_def_height)?:100
                itemHeight.put(viewType, height)
            }
        }
        return height
    }

    override fun getSectionName(position: Int): String {
        val item = data.getOrNull(position)?:data.last()
        return "#${item.floor}"
    }

    override fun getItemViewType(position: Int): Int {
        return if(position >= data.size)super.getItemViewType(position) else 0x00001000 + position
    }

    private val drawableMap  = HashMap<BaseViewHolder, LinkedHashMap<String, HtmlHttpImageGetter.UrlDrawable>>()
    @SuppressLint("SetTextI18n")
    override fun convert(helper: BaseViewHolder, item: TopicPost) {
        helper.addOnClickListener(R.id.item_del)
        helper.addOnClickListener(R.id.item_reply)
        helper.addOnClickListener(R.id.item_edit)
        helper.addOnClickListener(R.id.item_avatar)
        helper.itemView.item_user.text = "${item.nickname}@${item.username}"
        val subFloor = if (item.sub_floor > 0) "-${item.sub_floor}" else ""
        helper.itemView.item_time.text = "#${item.floor}$subFloor - ${item.dateline}"
        helper.itemView.offset.visibility = if (item.isSub) View.VISIBLE else View.GONE
        helper.itemView.item_reply.visibility = if (item.relate.toIntOrNull() ?: 0 > 0) View.VISIBLE else View.GONE
        helper.itemView.item_del.visibility = if (item.editable) View.VISIBLE else View.GONE
        helper.itemView.item_edit.visibility = helper.itemView.item_del.visibility
        val drawables = drawableMap.getOrPut(helper) { LinkedHashMap() }
        helper.itemView.item_message.let { item_message ->
            val htmlText = setTextLinkOpenByWebView(
                    Html.fromHtml(parseHtml(item.pst_content), HtmlHttpImageGetter(item_message, URI.create(Bangumi.SERVER), drawables), HtmlTagHandler(item_message) {
                        val imageList = drawables.filter { (it.key.startsWith("http") || !it.key.contains("smile")) }.toList()
                        val index = imageList.indexOfFirst { d -> d.first == it.source }
                        if (index < 0) return@HtmlTagHandler
                        val popWindow = PopupWindow(helper.itemView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true)
                        val viewPager = FixMultiViewPager(helper.itemView.context)
                        popWindow.contentView = viewPager
                        viewPager.adapter = PhotoPagerAdapter(imageList.map { HttpUtil.getUrl(it.first, URI.create(Bangumi.SERVER)) }) {
                            popWindow.dismiss()
                        }
                        viewPager.currentItem = index
                        popWindow.isClippingEnabled = false
                        popWindow.animationStyle = R.style.AppTheme_FadeInOut
                        popWindow.showAtLocation(helper.itemView, Gravity.CENTER, 0, 0)
                    })) {
                WebActivity.launchUrl(helper.itemView.context, it, "")
            }
            item_message.post {
                item_message.text = htmlText
            }
        }
        helper.itemView.item_message.setOnFocusChangeListener { view, focus ->
            if(!focus){
                view.tag = null
                (view as TextView).text = view.text
            } }
        helper.itemView.item_message.movementMethod = LinkMovementMethod.getInstance()
        Glide.with(helper.itemView)
                .load(HttpUtil.getUrl(item.avatar, URI.create(Bangumi.SERVER)))
                .apply(RequestOptions.errorOf(R.drawable.ic_404))
                .apply(RequestOptions.circleCropTransform())
                .into(helper.itemView.item_avatar)

        helper.itemView.tag = item
        helper.itemView.addOnLayoutChangeListener (object: View.OnLayoutChangeListener{
            override fun onLayoutChange(view: View, l: Int, t: Int, r: Int, b: Int, ol: Int, ot: Int, or: Int, ob: Int) {
                if(view.tag == item && b-t != ob-ot) {
                    Thread {
                        updateScrollOffsets(item, b - t)
                    }.start()
                } else if(view.tag != item)
                    view.removeOnLayoutChangeListener(this)
            }
        })
    }

    override fun onViewAttachedToWindow(holder: BaseViewHolder) {
        super.onViewAttachedToWindow(holder)

        // Bug workaround for losing text selection ability, see:
        // https://code.google.com/p/android/issues/detail?id=208169
        holder.itemView.item_message?.isEnabled = false
        holder.itemView.item_message?.isEnabled = true
    }

    companion object {
        fun parseHtml(html: String): String{
            val doc= Jsoup.parse(html, Bangumi.SERVER)
            doc.body().children().forEach {
                var appendBefore = ""
                var appendEnd = ""
                val style = it.attr("style")
                if(style.contains("font-weight:bold")) {
                    appendBefore = "$appendBefore<b>"
                    appendEnd = "</b>$appendEnd"
                } //it.html("<b>${parseHtml(it.html())}</b>")
                if(style.contains("font-style:italic")) {
                    appendBefore = "$appendBefore<i>"
                    appendEnd = "</i>$appendEnd"
                } //it.html("<i>${parseHtml(it.html())}</i>")
                if(style.contains("text-decoration: underline")) {
                    appendBefore = "$appendBefore<u>"
                    appendEnd = "</u>$appendEnd"
                } //it.html("<u>${parseHtml(it.html())}</u>")
                if(style.contains("font-size:")) { Regex("""font-size:([0-9]*)px""").find(style)?.groupValues?.get(1)?.let{size->
                    appendBefore = "$appendBefore<size size='${size}px'>"
                    appendEnd = "</size>$appendEnd"
                } }//it.html("<size size='${size}px'>${parseHtml(it.html())}</size>")
                if(style.contains("background-color:")) {
                    appendBefore = "$appendBefore<mask>"
                    appendEnd = "</mask>$appendEnd"
                } //it.html("<mask>${parseHtml(it.html())}</mask>")
                it.html("$appendBefore${parseHtml(it.html())}$appendEnd")
            }
            doc.select("div.quote").forEach {
                it.html("“${it.html()}”")
            }
            return doc.body().html()
        }

        fun setTextLinkOpenByWebView(htmlString: Spanned, onCLick:(String)->Unit): Spanned {
            if (htmlString is SpannableStringBuilder) {
                val objs = htmlString.getSpans(0, htmlString.length, URLSpan::class.java)
                if (null != objs && objs.isNotEmpty()) {
                    for (obj in objs) {
                        val start = htmlString.getSpanStart(obj)
                        val end = htmlString.getSpanEnd(obj)
                        if (obj is URLSpan) {
                            val url = obj.url
                            htmlString.removeSpan(obj)
                            htmlString.setSpan(object : ClickableSpan() {
                                override fun onClick(widget: View) {
                                    onCLick(url)
                                }
                            }, start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                        }
                    }
                }
            }
            return htmlString
        }
    }
}