@file:Suppress("DEPRECATION")

package soko.ekibun.bangumi.ui.topic

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.provider.OpenableColumns
import android.text.Html
import android.text.style.ImageSpan
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.forEach
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.awarmisland.android.richedittext.view.RichEditText
import kotlinx.android.synthetic.main.dialog_reply.view.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.jsoup.Jsoup
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.ui.view.BaseDialog
import soko.ekibun.bangumi.util.*
import java.lang.ref.WeakReference

/**
 * 回复对话框
 * @property hint String
 * @property callback Function3<String?, String?, Boolean, Unit>
 * @property draft String?
 * @property postTitle String?
 * @property bbCode Boolean
 * @property title String
 * @property onClickImage Function1<ImageSpan, Unit>
 * @property onClickUrl Function1<String, Unit>
 */
class ReplyDialog : BaseDialog(R.layout.dialog_reply) {

    var hint: String = ""
    var callback: (String?, String?, Boolean) -> Unit = { _, _, _ -> }
    var draft: String? = null
    var postTitle: String? = null

    var bbCode: Boolean = false

    private fun insertCode(code: String) {
        val contentView = contentView ?: return
        contentView.item_input.editableText.insert(contentView.item_input.selectionStart, "[$code]")
        contentView.item_input.editableText.insert(contentView.item_input.selectionEnd, "[/$code]")
        contentView.item_input.setSelection(contentView.item_input.selectionEnd - code.length - 3)
    }

    private fun showFormatPop(editText: RichEditText, anchor: View) {
        val popup = PopupMenu(editText.context, anchor)
        popup.menuInflater.inflate(R.menu.list_format, popup.menu)
        // update Selection
        var currentFontStyle = editText.fontStyle
        val updateCheck = {
            currentFontStyle = editText.fontStyle
            popup.menu.forEach {
                it.isCheckable = !bbCode
            }
            popup.menu.findItem(R.id.format_bold)?.isChecked = currentFontStyle?.isBold ?: false
            popup.menu.findItem(R.id.format_italic)?.isChecked = currentFontStyle?.isItalic ?: false
            popup.menu.findItem(R.id.format_strike)?.isChecked = currentFontStyle?.isStrike ?: false
            popup.menu.findItem(R.id.format_underline)?.isChecked = currentFontStyle?.isUnderline ?: false
            popup.menu.findItem(R.id.format_mask)?.isChecked = currentFontStyle?.isMask ?: false
        }
        updateCheck()

        popup.setOnMenuItemClickListener {
            if (bbCode) {
                insertCode(
                    when (it.itemId) {
                        R.id.format_bold -> "b"
                        R.id.format_italic -> "i"
                        R.id.format_strike -> "s"
                        R.id.format_underline -> "u"
                        R.id.format_mask -> "mask"
                        else -> return@setOnMenuItemClickListener true
                    }
                )
                return@setOnMenuItemClickListener true
            }
            when (it.itemId) {
                R.id.format_bold -> {
                    editText.setBold(!(currentFontStyle?.isBold ?: false))
                }
                R.id.format_italic -> {
                    editText.setItalic(!(currentFontStyle?.isItalic ?: false))
                }
                R.id.format_strike -> {
                    editText.setStrike(!(currentFontStyle?.isStrike ?: false))
                }
                R.id.format_underline -> {
                    editText.setUnderline(!(currentFontStyle?.isUnderline ?: false))
                }
                R.id.format_mask -> {
                    editText.setMask(!(currentFontStyle?.isMask ?: false))
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

    override val title = ""
    override fun onViewCreated(view: View) {
        bbCode = PreferenceManager.getDefaultSharedPreferences(view.context).getBoolean("use_bbcode", false)

        view.item_title_container.visibility = if (postTitle == null) View.GONE else View.VISIBLE
        view.item_title.setText(postTitle)

        view.item_expand.setOnClickListener {
            it.rotation = -it.rotation
            val isExpand = it.rotation > 0
            view.item_content.layoutParams = view.item_content.layoutParams.also { lp ->
                lp.height = if (isExpand) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
            }
            view.item_input.maxHeight =
                if (it.rotation > 0) Int.MAX_VALUE else ResourceUtil.toPixels(view.context.resources, 200f)
            view.item_input.layoutParams = view.item_input.layoutParams.also { lp ->
                lp.height = if (isExpand) 0 else ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }

        view.item_bbcode.isSelected = bbCode
        view.item_bbcode.setOnClickListener {
            bbCode = !bbCode
            view.item_bbcode.isSelected = bbCode
            if (bbCode) {
                view.item_input.setText(TextUtil.span2bbcode(view.item_input.editableText))
            } else {
                view.item_input.setText(
                    TextUtil.setTextUrlCallback(
                        Html.fromHtml(
                            parseHtml(TextUtil.bbcode2html(view.item_input.editableText.toString())),
                            CollapseHtmlHttpImageGetter(view.item_input),
                            HtmlTagHandler(view.item_input, onClick = onClickImage)
                        ), onClickUrl
                    )
                )
                view.item_input.post { view.item_input.text = view.item_input.text }
            }
        }

        val emojiAdapter = EmojiAdapter(emojiList)
        emojiAdapter.setOnItemChildClickListener { _, _, position ->
            if (bbCode) {
                view.item_input.editableText.replace(
                    view.item_input.selectionStart,
                    view.item_input.selectionEnd,
                    emojiList[position].first
                )
                return@setOnItemChildClickListener
            }
            val drawable = CollapseUrlDrawable(WeakReference(view.item_input))
            drawable.url = emojiList[position].second
            drawable.loadImage()
            view.item_input.setImage(HtmlTagHandler.ClickableImage(ImageSpan(drawable, emojiList[position].first)) {})
        }
        view.item_btn_format.setOnClickListener { v ->
            showFormatPop(view.item_input, v)
        }
        view.item_emoji_list.adapter = emojiAdapter
        view.item_emoji_list.layoutManager = GridLayoutManager(context, 7)

        var insetsBottom = 0
        val paddingBottom = view.item_emoji_list.paddingBottom
        val updateEmojiList = {
            if (insetsBottom > 200) {
                view.item_emoji_list.layoutParams.height = insetsBottom
                view.item_emoji_list.layoutParams = view.item_emoji_list.layoutParams
            } else {
                view.item_emoji_list.let {
                    it.setPadding(it.paddingLeft, it.paddingTop, it.paddingRight, paddingBottom + insetsBottom)
                }
                view.item_nav_padding.layoutParams.height = insetsBottom
                view.item_nav_padding.layoutParams = view.item_nav_padding.layoutParams
            }
            view.item_emoji_list.visibility =
                if (insetsBottom > 200) View.INVISIBLE else if (view.item_btn_emoji.isSelected) View.VISIBLE else View.GONE
            (view.item_lock.layoutParams as? LinearLayout.LayoutParams)?.weight = 1f
        }

        view.item_btn_image.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(Intent.createChooser(intent, "选择图片"), 1)
        }
        view.item_btn_send.setOnClickListener {
            callback(TextUtil.span2bbcode(view.item_input.editableText), view.item_title.editableText.toString(), true)
            callback = { _, _, _ -> }
            dismiss()
        }

        view.setOnApplyWindowInsetsListener { _, insets ->
            view.setPadding(0, insets.systemWindowInsetTop, 0, 0)
            insetsBottom = insets.systemWindowInsetBottom
            updateEmojiList()
            if (insetsBottom > 200) {
                view.item_btn_emoji.isSelected = false
            }
            insets
        }

        val inputMethodManager =
            view.context.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        view.item_btn_emoji.setOnClickListener {
            view.item_btn_emoji.isSelected = !view.item_btn_emoji.isSelected
            if (insetsBottom > 200 == view.item_btn_emoji.isSelected) {
                inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
            }
            updateEmojiList()
            if (view.item_emoji_list.visibility == View.GONE && insetsBottom < 200) {
                val layoutParams = view.item_lock.layoutParams as LinearLayout.LayoutParams
                layoutParams.height = view.item_lock.height
                layoutParams.weight = 0f
            }
        }

        dialog?.setOnKeyListener { _, keyCode, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK && view.item_btn_emoji.isSelected) {
                view.item_btn_emoji.isSelected = false
                updateEmojiList()
                true
            } else false
        }


        view.item_lock.setOnClickListener { dismiss() }
        view.item_hint.text = hint

        if (!draft.isNullOrEmpty()) {
            if (bbCode) {
                view.item_input.setText(draft)
            } else {
                view.item_input.setText(
                    TextUtil.setTextUrlCallback(
                        Html.fromHtml(
                            parseHtml(TextUtil.bbcode2html(draft!!)),
                            CollapseHtmlHttpImageGetter(view.item_input),
                            HtmlTagHandler(view.item_input, onClick = onClickImage)
                        ), onClickUrl
                    )
                )
                view.item_input.post { view.item_input.text = view.item_input.text }
            }
        }
    }

    val onClickImage = { _: ImageSpan ->
        //Toast.makeText(context?:return@click, (it.drawable as? UrlDrawable)?.url?:"", Toast.LENGTH_LONG).show()
    }

    val onClickUrl = { _: String ->
        //Toast.makeText(context?:return@click, url, Toast.LENGTH_LONG).show()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        callback(
            contentView?.item_input?.editableText?.let { TextUtil.span2bbcode(it) },
            contentView?.item_title?.text?.toString(),
            false
        )
        callback = { _, _, _ -> }
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
        val requestBody = RequestBody.create(mimeType.toMediaTypeOrNull(), inputStream.readBytes())
        var span: HtmlTagHandler.ClickableImage? = null
        val drawable = UploadDrawable(requestBody, fileName, WeakReference(item_input), data?.data ?: return) {
            if (bbCode) {
                val start = item_input.editableText.getSpanStart(span)
                val end = item_input.editableText.getSpanEnd(span)
                if (start < 0 || end < 0) return@UploadDrawable
                item_input.editableText.removeSpan(span)
                item_input.editableText.removeSpan(span?.image)
                item_input.editableText.replace(start, end, "[img]$it[/img]")
            }
        }
        drawable.loadImage()
        span = HtmlTagHandler.ClickableImage(ImageSpan(drawable), onClickImage)
        item_input.setImage(span)
    }

    /**
     * 限制最大高度url drawable ImageGetter
     * @property container WeakReference<(android.widget.TextView..android.widget.TextView?)>
     * @constructor
     */
    class CollapseHtmlHttpImageGetter(container: TextView) : Html.ImageGetter {
        private val container = WeakReference(container)
        override fun getDrawable(source: String): Drawable {
            val urlDrawable = CollapseUrlDrawable(container)
            urlDrawable.url = Bangumi.parseUrl(source)
            urlDrawable.loadImage()
            return urlDrawable
        }
    }

    companion object {
        /**
         * 显示对话框
         * @param fragmentManager FragmentManager
         * @param hint String
         * @param draft String?
         * @param title String?
         * @param callback Function3<String?, String?, Boolean, Unit>
         */
        fun showDialog(
            fragmentManager: FragmentManager,
            hint: String,
            draft: String?,
            title: String? = null,
            callback: (String?, String?, Boolean) -> Unit = { _, _, _ -> }
        ) {
            val dialog = ReplyDialog()
            dialog.hint = hint
            dialog.draft = draft
            dialog.postTitle = title
            dialog.callback = callback
            dialog.show(fragmentManager, "reply")
        }

        /**
         * 转换html
         * @param html String
         * @return String
         */
        fun parseHtml(html: String): String {
            val doc = Jsoup.parse(html.replace(Regex("</?noscript>"), ""), Bangumi.SERVER)
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
                        appendBefore = "$appendBefore[size=$size]"
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
            doc.select("div.codeHighlight").forEach {
                it.html("[code]${it.html()}[/code]")
            }
            return doc.body().html()
        }

        val emojiList by lazy {
            val emojiList = ArrayList<Pair<String, String>>()
            for (i in 1..23)
                emojiList.add(
                    String.format(
                        "(bgm%02d)",
                        i
                    ) to String.format(
                        "${Bangumi.SERVER}/img/smiles/bgm/%02d${if (i == 11 || i == 23) ".gif" else ".png"}",
                        i
                    )
                )
            for (i in 1..100)
                emojiList.add(
                    String.format(
                        "(bgm%02d)",
                        i + 23
                    ) to String.format("${Bangumi.SERVER}/img/smiles/tv/%02d.gif", i)
                )
            "(=A=)|(=w=)|(-w=)|(S_S)|(=v=)|(@_@)|(=W=)|(TAT)|(T_T)|(='=)|(=3=)|(= =')|(=///=)|(=.,=)|(:P)|(LOL)".split("|")
                .forEachIndexed { i, s ->
                    emojiList.add(s to "${Bangumi.SERVER}/img/smiles/${i + 1}.gif")
                }
            emojiList
        }
    }
}