@file:Suppress("DEPRECATION")

package soko.ekibun.bangumi.ui.topic

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Canvas
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Html
import android.text.style.ImageSpan
import android.util.Size
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.GridLayoutManager
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.awarmisland.android.richedittext.handle.CustomHtml
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.Headers
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.android.synthetic.main.dialog_reply.view.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.jsoup.Jsoup
import pl.droidsonroids.gif.GifDrawable
import retrofit2.Call
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.uploadcc.UploadCC
import soko.ekibun.bangumi.api.uploadcc.bean.Response
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.ui.topic.PostAdapter.Companion.setTextLinkOpenByWebView
import soko.ekibun.bangumi.util.*
import java.lang.ref.WeakReference
import java.net.URI

class ReplyDialog: androidx.fragment.app.DialogFragment() {
    private var contentView: View? = null

    var hint: String = ""
    var callback: (String, Boolean) -> Unit = { _, _ -> }
    var draft: String = ""
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val contentView = contentView ?: inflater.inflate(R.layout.dialog_reply, container)
        this.contentView = contentView
        val emojiAdapter = EmojiAdapter(emojiList)
        emojiAdapter.setOnItemChildClickListener { _, _, position ->
            val drawable = UrlDrawable(WeakReference(contentView.item_input))
            drawable.url = emojiList[position].second
            drawable.loadImage()
            contentView.item_input.setImage(HtmlTagHandler.ClickableImage(ImageSpan(drawable, emojiList[position].first)) {})
        }
        contentView.item_btn_format.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.list_format, popup.menu)
            // update Selection
            var currentFontStyle = contentView.item_input.fontStyle
            val updateCheck = {
                currentFontStyle = contentView.item_input.fontStyle
                popup.menu.findItem(R.id.format_bold)?.isChecked = currentFontStyle?.isBold ?: false
                popup.menu.findItem(R.id.format_italic)?.isChecked = currentFontStyle?.isItalic ?: false
                popup.menu.findItem(R.id.format_strike)?.isChecked = currentFontStyle?.isStrike ?: false
                popup.menu.findItem(R.id.format_underline)?.isChecked = currentFontStyle?.isUnderline ?: false
                popup.menu.findItem(R.id.format_mask)?.isChecked = currentFontStyle?.isMask ?: false
            }
            updateCheck()

            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.format_bold -> {
                        contentView.item_input.setBold(!(currentFontStyle?.isBold ?: false))
                    }
                    R.id.format_italic -> {
                        contentView.item_input.setItalic(!(currentFontStyle?.isItalic ?: false))
                    }
                    R.id.format_strike -> {
                        contentView.item_input.setStrike(!(currentFontStyle?.isStrike ?: false))
                    }
                    R.id.format_underline -> {
                        contentView.item_input.setUnderline(!(currentFontStyle?.isUnderline ?: false))
                    }
                    R.id.format_mask -> {
                        contentView.item_input.setMask(!(currentFontStyle?.isMask ?: false))
                    }
                    else -> return@setOnMenuItemClickListener true
                }
                updateCheck()
                // Keep the popup menu open
                it.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
                it.actionView = View(context)
                it.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                        return false
                    }

                    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                        return false
                    }
                })
                false
            }
            popup.show()
        }
        contentView.item_emoji_list.adapter = emojiAdapter
        contentView.item_emoji_list.layoutManager = GridLayoutManager(context, 7)

        var insetsBottom = 0
        val paddingBottom = contentView.item_emoji_list.paddingBottom
        val updateEmojiList = {
            if (insetsBottom > 200) {
                contentView.item_emoji_list.layoutParams.height = insetsBottom
                contentView.item_emoji_list.layoutParams = contentView.item_emoji_list.layoutParams
            } else {
                contentView.item_emoji_list.setPadding(contentView.item_emoji_list.paddingLeft, contentView.item_emoji_list.paddingTop, contentView.item_emoji_list.paddingRight, paddingBottom + insetsBottom)
                contentView.item_nav_padding.layoutParams.height = insetsBottom
                contentView.item_nav_padding.layoutParams = contentView.item_nav_padding.layoutParams
            }
            contentView.item_emoji_list.visibility = if (insetsBottom > 200) View.INVISIBLE else if (contentView.item_btn_emoji.isSelected) View.VISIBLE else View.GONE
            (contentView.item_lock.layoutParams as LinearLayout.LayoutParams).weight = 1f
        }

        contentView.item_btn_image.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }
        contentView.item_btn_send.setOnClickListener {
            callback(CustomHtml.toHtml(contentView.item_input.editableText), true)
            callback = { _, _ -> }
            dismiss()
        }

        contentView.setOnApplyWindowInsetsListener { v, insets ->
            insetsBottom = insets.systemWindowInsetBottom
            updateEmojiList()
            if (insetsBottom > 200) {
                contentView.item_btn_emoji.isSelected = false
            }
            insets
        }
        val inputMethodManager = inflater.context.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        contentView.item_btn_emoji.setOnClickListener {
            contentView.item_btn_emoji.isSelected = !contentView.item_btn_emoji.isSelected
            if (insetsBottom > 200 == contentView.item_btn_emoji.isSelected) {
                inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
            }
            updateEmojiList()
            if (contentView.item_emoji_list.visibility == View.GONE && insetsBottom < 200) {
                val layoutParams = contentView.item_lock.layoutParams as LinearLayout.LayoutParams
                layoutParams.height = contentView.item_lock.height
                layoutParams.weight = 0f
            }
        }

        dialog?.setOnKeyListener { _, keyCode, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK && contentView.item_btn_emoji.isSelected) {
                contentView.item_btn_emoji.isSelected = false
                updateEmojiList()
                true
            } else false
        }


        contentView.item_lock.setOnClickListener { dismiss() }
        contentView.item_hint.text = hint

        contentView.item_input.setText(setTextLinkOpenByWebView(Html.fromHtml(parseHtml(draft), HtmlHttpImageGetter(contentView.item_input, URI.create(Bangumi.SERVER)), HtmlTagHandler(contentView.item_input) {
            //Toast.makeText(contentView.context, "click Image: ${(it.drawable as? UrlDrawable)?.url}", Toast.LENGTH_LONG).show()
        })) {
            //Toast.makeText(contentView.context, "click URL: $it", Toast.LENGTH_LONG).show()
        })

        dialog?.window?.let { ThemeModel.updateNavigationTheme(it, contentView.context) }

        dialog?.window?.attributes?.let {
            it.dimAmount = 0.6f
            dialog?.window?.attributes = it
        }
        dialog?.window?.setWindowAnimations(R.style.AnimDialog)
        dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return contentView
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        callback(contentView?.item_input?.text.toString(), false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_Dialog)
    }

    @SuppressLint("Recycle")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val item_input = contentView?.item_input ?: return
        if (contentView?.item_btn_emoji?.isSelected == false) {
            contentView?.item_input?.postDelayed({
                contentView?.item_input?.requestFocus()
                val inputMethodManager = (context
                        ?: return@postDelayed).applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
            }, 200)
        }
        val inputStream = activity?.contentResolver?.openInputStream(data?.data ?: return) ?: return
        val mimeType = activity?.contentResolver?.getType(data?.data ?: return) ?: "image/jpeg"
        val fileName = data?.data?.let { returnUri ->
            activity?.contentResolver?.query(returnUri, null, null, null, null)
        }?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        } ?: "image.jpg"
        val requestBody = RequestBody.create(MediaType.parse(mimeType), inputStream.readBytes())
        val drawable = UploadDrawable(requestBody, fileName, WeakReference(item_input))
        drawable.loadImage()
        item_input.setImage(HtmlTagHandler.ClickableImage(ImageSpan(drawable)) {
            //TODO
        })
    }

    class HtmlHttpImageGetter(container: TextView, private val baseUri: URI?) : Html.ImageGetter {
        private val container = WeakReference(container)
        override fun getDrawable(source: String): Drawable {
            val urlDrawable = UrlDrawable(container)
            urlDrawable.url = HttpUtil.getUrl(source, baseUri)
            urlDrawable.loadImage()
            return urlDrawable
        }
    }

    open class UrlDrawable(val container: WeakReference<TextView>) : AnimationDrawable() {
        var drawable: Drawable? = null
        var error: Boolean? = null
        var url: String? = null

        init {
            container.get()?.let {
                setBounds(0, 0, it.textSize.toInt(), it.textSize.toInt())
            }
        }

        fun update(resource: Drawable, defSize: Int) {
            val drawable = when (resource) {
                is com.bumptech.glide.load.resource.gif.GifDrawable -> GifDrawable(resource.buffer)
                else -> resource
            }
            val size = if (defSize > 0) Size(defSize, defSize) else Size(resource.intrinsicWidth, resource.intrinsicHeight)
            setBounds(0, 0, size.width, size.height)

            drawable.setBounds(0, 0, size.width, size.height)
            this.drawable?.callback = null
            this.drawable = drawable
            //}
            //container.get()?.text = container.get()?.text
            container.get()?.let {
                it.editableText.getSpans(0, it.editableText.length, ImageSpan::class.java).filter { it.drawable == this }.forEach { span ->
                    val start = it.editableText.getSpanStart(span)
                    val end = it.editableText.getSpanEnd(span)
                    val flags = it.editableText.getSpanFlags(span)

                    it.editableText.removeSpan(span)
                    it.editableText.setSpan(span, start, end, flags)
                }
                it.invalidate()
            }
        }

        open fun loadImage() {
            val view = container.get()
            val url = this.url ?: return
            view?.post {
                val textSize = view.textSize
                val circularProgressDrawable = CircularProgressDrawable(view.context)
                circularProgressDrawable.setColorSchemeColors(ResourceUtil.resolveColorAttr(view.context, android.R.attr.textColorSecondary))
                circularProgressDrawable.strokeWidth = 5f
                circularProgressDrawable.centerRadius = textSize / 2 - circularProgressDrawable.strokeWidth - 1f
                circularProgressDrawable.progressRotation = 0.75f
                circularProgressDrawable.start()
                ProgressAppGlideModule.expect(url, object : ProgressAppGlideModule.UIonProgressListener {
                    override fun onProgress(bytesRead: Long, expectedLength: Long) {
                        if (circularProgressDrawable.isRunning) circularProgressDrawable.stop()
                        circularProgressDrawable.setStartEndTrim(0f, bytesRead * 1f / expectedLength)
                        circularProgressDrawable.progressRotation = 0.75f
                        circularProgressDrawable.invalidateSelf()
                    }

                    override fun getGranualityPercentage(): Float {
                        return 1.0f
                    }
                })
                GlideUtil.with(view)
                        ?.asDrawable()?.load(GlideUrl(url, Headers {
                            mapOf("referer" to url,
                                    "user-agent" to App.getUserAgent(view.context)
                            )
                        }))
                        ?.apply(RequestOptions().transform(SizeTransformation { width, _ ->
                            val maxWidth = view.width - view.paddingLeft - view.paddingRight - 10f
                            val minWidth = view.textSize
                            Math.min(maxWidth, Math.max(minWidth, width.toFloat())) / width
                        }).placeholder(circularProgressDrawable).error(R.drawable.ic_broken_image))
                        ?.into(object : SimpleTarget<Drawable>() {
                            override fun onLoadStarted(placeholder: Drawable?) {
                                error = null
                                placeholder?.let { update(it, textSize.toInt()) }
                            }

                            override fun onLoadFailed(errorDrawable: Drawable?) {
                                error = true
                                if (circularProgressDrawable.isRunning) circularProgressDrawable.stop()
                                errorDrawable?.let { update(it, textSize.toInt()) }
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                                error = null
                                placeholder?.let { update(it, textSize.toInt()) }
                            }

                            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                if (circularProgressDrawable.isRunning) circularProgressDrawable.stop()
                                error = false
                                update(resource, 0)
                            }

                            override fun onStart() {}
                            override fun onDestroy() {
                                ProgressAppGlideModule.forget(url)
                            }
                        })
            }
        }

        override fun draw(canvas: Canvas) {
            drawable?.callback = object : Callback {
                override fun invalidateDrawable(who: Drawable) {
                    val view = container.get() ?: return
                    view.post { view.invalidate() }
                }

                override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
                    container.get()?.postDelayed(what, `when`)
                }

                override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                    container.get()?.removeCallbacks(what)
                }
            }
            drawable?.draw(canvas)
        }
    }

    class UploadDrawable(val requestBody: RequestBody, val fileName: String, container: WeakReference<TextView>) : UrlDrawable(container) {
        override fun loadImage() {
            if (url != null) {
                super.loadImage()
                return
            }
            val view = container.get()
            view?.post {
                val textSize = view.textSize
                val circularProgressDrawable = CircularProgressDrawable(view.context)
                circularProgressDrawable.setColorSchemeColors(ResourceUtil.resolveColorAttr(view.context, android.R.attr.textColorSecondary))
                circularProgressDrawable.strokeWidth = 5f
                circularProgressDrawable.centerRadius = textSize / 2 - circularProgressDrawable.strokeWidth - 1f
                circularProgressDrawable.progressRotation = 0.75f
                update(circularProgressDrawable, textSize.toInt())

                val errorDrawable = view.context.getDrawable(R.drawable.ic_broken_image)
                val callback = object : RetrofitCallback<Response>() {
                    override fun onSuccess(call: Call<Response>?, response: retrofit2.Response<Response>) {
                        val imgUrl = response.body()?.success_image?.firstOrNull()?.url
                        if (imgUrl != null) {
                            url = "https://upload.cc/$imgUrl"
                            loadImage()
                        } else {
                            error = true
                            errorDrawable?.let { update(it, textSize.toInt()) }
                        }
                    }

                    override fun onFailure(call: Call<Response>, t: Throwable) {
                        error = true
                        errorDrawable?.let { update(it, textSize.toInt()) }
                    }

                    override fun onLoading(total: Long, progress: Long) {
                        super.onLoading(total, progress)
                        view.post {
                            circularProgressDrawable.setStartEndTrim(0f, progress * 1f / total)
                            circularProgressDrawable.progressRotation = 0.75f
                            circularProgressDrawable.invalidateSelf()
                        }
                    }
                }
                val fileRequestBody = FileRequestBody(requestBody, callback)
                val body = MultipartBody.Part.createFormData("uploaded_file[]", fileName, fileRequestBody)
                val call = UploadCC.createInstance().upload(body)
                call.enqueue(callback)
            }
        }
    }

    companion object {
        fun parseHtml(html: String): String {
            val doc = Jsoup.parse(html, Bangumi.SERVER)
            doc.outputSettings().indentAmount(0).prettyPrint(false)
            doc.select("script").remove()
            doc.select("img").forEach {
                if (!it.hasAttr("src")) it.remove()
            }
            doc.body().children().forEach {
                var appendBefore = ""
                var appendEnd = ""
                val style = it.attr("style")
                if (style.contains("font-weight:bold")) {
                    appendBefore = "$appendBefore<b>"
                    appendEnd = "</b>$appendEnd"
                } //it.html("<b>${parseHtml(it.html())}</b>")
                if (style.contains("font-style:italic")) {
                    appendBefore = "$appendBefore<i>"
                    appendEnd = "</i>$appendEnd"
                } //it.html("<i>${parseHtml(it.html())}</i>")
                if (style.contains("text-decoration: underline")) {
                    appendBefore = "$appendBefore<u>"
                    appendEnd = "</u>$appendEnd"
                } //it.html("<u>${parseHtml(it.html())}</u>")
                if (style.contains("font-size:")) {
                    Regex("""font-size:([0-9]*)px""").find(style)?.groupValues?.get(1)?.let { size ->
                        appendBefore = "$appendBefore[size='$size]'>"
                        appendEnd = "[/size]$appendEnd"
                    }
                }//it.html("<size size='${size}px'>${parseHtml(it.html())}</size>")
                if (style.contains("background-color:")) {
                    appendBefore = "$appendBefore<mask>"
                    appendEnd = "</mask>$appendEnd"
                } //it.html("<mask>${parseHtml(it.html())}</mask>")
                it.html("$appendBefore${parseHtml(it.html())}$appendEnd")
            }
            doc.select("div.quote").forEach {
                it.html("[quote]${it.html()}[/quote]")
            }
            return doc.body().html()
        }

        val emojiList by lazy {
            val emojiList = ArrayList<Pair<String, String>>()
            for (i in 1..23)
                emojiList.add(String.format("(bgm%02d)", i) to String.format("${Bangumi.SERVER}/img/smiles/bgm/%02d${if (i == 11 || i == 23) ".gif" else ".png"}", i))
            for (i in 1..100)
                emojiList.add(String.format("(bgm%02d)", i + 23) to String.format("${Bangumi.SERVER}/img/smiles/tv/%02d.gif", i))
            "(=A=)|(=w=)|(-w=)|(S_S)|(=v=)|(@_@)|(=W=)|(TAT)|(T_T)|(='=)|(=3=)|(= =')|(=///=)|(=.,=)|(:P)|(LOL)".split("|").forEachIndexed { i, s ->
                emojiList.add(s to "${Bangumi.SERVER}/img/smiles/${i + 1}.gif")
            }
            emojiList
        }
    }
}