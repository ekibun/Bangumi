@file:Suppress("DEPRECATION")

package soko.ekibun.bangumi.ui.topic

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Html
import android.text.style.ImageSpan
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.GridLayoutManager
import com.awarmisland.android.richedittext.view.RichEditText
import kotlinx.android.synthetic.main.dialog_reply.view.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.jsoup.Jsoup
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.util.CollapseUrlDrawable
import soko.ekibun.bangumi.util.HtmlTagHandler
import soko.ekibun.bangumi.util.TextUtil
import soko.ekibun.bangumi.util.UploadDrawable
import java.lang.ref.WeakReference

/**
 * 回复对话框
 */
class ReplyDialog: androidx.fragment.app.DialogFragment() {
    private var contentView: View? = null

    var hint: String = ""
    var callback: (String?, Boolean) -> Unit = { _, _ -> }
    var draft: String? = null

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
            popup.menu.findItem(R.id.format_bold)?.isChecked = currentFontStyle?.isBold ?: false
            popup.menu.findItem(R.id.format_italic)?.isChecked = currentFontStyle?.isItalic ?: false
            popup.menu.findItem(R.id.format_strike)?.isChecked = currentFontStyle?.isStrike ?: false
            popup.menu.findItem(R.id.format_underline)?.isChecked = currentFontStyle?.isUnderline ?: false
            popup.menu.findItem(R.id.format_mask)?.isChecked = currentFontStyle?.isMask ?: false
        }
        updateCheck()

        popup.setOnMenuItemClickListener {
            if (bbCode) {
                insertCode(when (it.itemId) {
                    R.id.format_bold -> "b"
                    R.id.format_italic -> "i"
                    R.id.format_strike -> "s"
                    R.id.format_underline -> "u"
                    R.id.format_mask -> "mask"
                    else -> return@setOnMenuItemClickListener true
                })
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val contentView = contentView ?: inflater.inflate(R.layout.dialog_reply, container)
        this.contentView = contentView

        contentView.item_bbcode.isSelected = bbCode
        contentView.item_bbcode.setOnClickListener {
            bbCode = !bbCode
            contentView.item_bbcode.isSelected = bbCode
            if (bbCode) {
                contentView.item_input.setText(TextUtil.span2bbcode(contentView.item_input.editableText))
            } else {
                contentView.item_input.setText(TextUtil.setTextUrlCallback(Html.fromHtml(parseHtml(TextUtil.bbcode2html(contentView.item_input.editableText.toString())),
                        CollapseHtmlHttpImageGetter(contentView.item_input),
                        HtmlTagHandler(contentView.item_input, onClick = onClickImage)), onClickUrl))
                contentView.item_input.post { contentView.item_input.text = contentView.item_input.text }
            }
        }

        val emojiAdapter = EmojiAdapter(emojiList)
        emojiAdapter.setOnItemChildClickListener { _, _, position ->
            if (bbCode) {
                contentView.item_input.editableText.replace(contentView.item_input.selectionStart, contentView.item_input.selectionEnd, emojiList[position].first)
                return@setOnItemChildClickListener
            }
            val drawable = CollapseUrlDrawable(WeakReference(contentView.item_input))
            drawable.url = emojiList[position].second
            drawable.loadImage()
            contentView.item_input.setImage(HtmlTagHandler.ClickableImage(ImageSpan(drawable, emojiList[position].first)) {})
        }
        contentView.item_btn_format.setOnClickListener { view ->
            showFormatPop(contentView.item_input, view)
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
            (contentView.item_lock.layoutParams as? LinearLayout.LayoutParams)?.weight = 1f
        }

        contentView.item_btn_image.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }
        contentView.item_btn_send.setOnClickListener {
            callback(TextUtil.span2bbcode(contentView.item_input.editableText), true)
            callback = { _, _ -> }
            dismiss()
        }

        contentView.setOnApplyWindowInsetsListener { _, insets ->
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

        if (!draft.isNullOrEmpty()) {
            if (bbCode) {
                contentView.item_input.setText(draft)
            } else {
                contentView.item_input.setText(TextUtil.setTextUrlCallback(Html.fromHtml(parseHtml(TextUtil.bbcode2html(draft!!)),
                        CollapseHtmlHttpImageGetter(contentView.item_input),
                        HtmlTagHandler(contentView.item_input, onClick = onClickImage)), onClickUrl))
                contentView.item_input.post { contentView.item_input.text = contentView.item_input.text }
            }
        }

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

    val onClickImage = { _: ImageSpan ->
        //Toast.makeText(context?:return@click, (it.drawable as? UrlDrawable)?.url?:"", Toast.LENGTH_LONG).show()
    }

    val onClickUrl = { _: String ->
        //Toast.makeText(context?:return@click, url, Toast.LENGTH_LONG).show()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        callback(contentView?.item_input?.editableText?.let { TextUtil.span2bbcode(it) }, false)
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
     *  限制最大高度url drawable ImageGetter
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
         * 转换html
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