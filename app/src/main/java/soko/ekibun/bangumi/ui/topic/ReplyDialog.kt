@file:Suppress("DEPRECATION")

package soko.ekibun.bangumi.ui.topic

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.provider.OpenableColumns
import android.text.Spanned
import android.text.style.ImageSpan
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.forEach
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.awarmisland.android.richedittext.view.RichEditText
import kotlinx.android.synthetic.main.dialog_reply.view.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.ui.view.BaseDialog
import soko.ekibun.bangumi.util.HtmlUtil
import soko.ekibun.bangumi.util.ResourceUtil
import soko.ekibun.bangumi.util.span.ClickableImageSpan
import soko.ekibun.bangumi.util.span.ClickableUrlSpan
import soko.ekibun.bangumi.util.span.CollapseUrlDrawable
import soko.ekibun.bangumi.util.span.UploadDrawable
import java.lang.ref.WeakReference

/**
 * 回复对话框
 * @property hint String
 * @property callback Function3<String?, String?, Boolean, Unit>
 * @property draft String?
 * @property postTitle String?
 * @property bbCode Boolean
 * @property title String
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

    private fun setHtml(html: String, view: TextView, collapseImageGetter: CollapseUrlDrawable.CollapseImageGetter) {
        val span = HtmlUtil.html2span(
            html, collapseImageGetter
        ).also {
            it.getSpans(0, it.length, ClickableUrlSpan::class.java)
                .forEach { span ->
                    span.onClick = { v, url ->
                        Toast.makeText(v.context, url, Toast.LENGTH_LONG).show()
                    }
                }
        }
        HtmlUtil.attachToTextView(span, view)
        view.post { view.text = view.text }
    }

    lateinit var collapseImageGetter: CollapseUrlDrawable.CollapseImageGetter

    override val title = ""
    override fun onViewCreated(view: View) {
        bbCode = PreferenceManager.getDefaultSharedPreferences(view.context).getBoolean("use_bbcode", false)
        collapseImageGetter = CollapseUrlDrawable.CollapseImageGetter(view.item_input)

        view.item_title_container.visibility = if (postTitle == null) View.GONE else View.VISIBLE
        view.item_title.setText(postTitle)


        view.item_expand.setOnClickListener {
            it.rotation = -it.rotation
            val isExpand = it.rotation > 0
            view.item_content.layoutParams = view.item_content.layoutParams.also { lp ->
                lp.height = if (isExpand) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
            }
            view.item_input.maxHeight =
                if (it.rotation > 0) Int.MAX_VALUE else ResourceUtil.toPixels(200f)
            view.item_input.layoutParams = view.item_input.layoutParams.also { lp ->
                lp.height = if (isExpand) 0 else ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }

        view.item_bbcode.isSelected = bbCode
        view.item_bbcode.setOnClickListener {
            bbCode = !bbCode
            view.item_bbcode.isSelected = bbCode
            if (bbCode) {
                view.item_input.setText(HtmlUtil.span2bbcode(view.item_input.editableText))
            } else {
                setHtml(
                    HtmlUtil.bbcode2html(view.item_input.editableText.toString()),
                    view.item_input, collapseImageGetter
                )
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
            } else {
                val drawable = collapseImageGetter.createDrawable()
                drawable.url = emojiList[position].second
                view.item_input.editableText.insert(
                    view.item_input.selectionStart,
                    HtmlUtil.createImageSpan(ImageSpan(drawable, emojiList[position].first, ImageSpan.ALIGN_BASELINE))
                )
                drawable.container = WeakReference(view.item_input)
                drawable.loadImage()
            }
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
            callback(HtmlUtil.span2bbcode(view.item_input.editableText), view.item_title.editableText.toString(), true)
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
            if (view.item_btn_emoji.isSelected) {
                inputMethodManager.hideSoftInputFromWindow(
                    view.item_input.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
            } else {
                inputMethodManager.showSoftInput(view.item_input, 0)
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
                setHtml(HtmlUtil.bbcode2html(draft!!), view.item_input, collapseImageGetter)
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        callback(
            contentView?.item_input?.editableText?.let { HtmlUtil.span2bbcode(it) },
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
        var span: ClickableImageSpan? = null
        val drawable = UploadDrawable(
            requestBody, fileName, data?.data ?: return,
            collapseImageGetter.wrapWidth, collapseImageGetter.sizeCache
        ) {
            if (bbCode) {
                val start = item_input.editableText.getSpanStart(span)
                val end = item_input.editableText.getSpanEnd(span)
                if (start < 0 || end < 0) return@UploadDrawable
                item_input.editableText.removeSpan(span)
                item_input.editableText.removeSpan(span?.image)
                item_input.editableText.replace(start, end, "[img]$it[/img]")
            }
        }
        val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_BASELINE)
        span = ClickableImageSpan(imageSpan, collapseImageGetter.onClick)
        item_input.editableText.insert(
            item_input.selectionStart,
            HtmlUtil.createImageSpan(imageSpan).also {
                it.setSpan(span, 0, it.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        )
        drawable.container = WeakReference(item_input)
        drawable.loadImage()
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