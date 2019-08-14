package soko.ekibun.bangumi.ui.topic

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.appcompat.widget.PopupMenu
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import kotlinx.android.synthetic.main.dialog_reply.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.recyclerview.widget.GridLayoutManager
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.uploadcc.UploadCC

class ReplyDialog: androidx.fragment.app.DialogFragment() {
    private var contentView: View? = null

    private fun getKeyBoardHeight(): Int{
        val rect = Rect()
        activity?.window?.decorView?.getWindowVisibleDisplayFrame(rect)
        val metrics = DisplayMetrics()
        (activity?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.getMetrics(metrics)
        return metrics.heightPixels - rect.bottom
    }

    private fun insertCode(code: String){
        val contentView = contentView?:return
        contentView.item_input.text.insert(contentView.item_input.selectionStart, "[$code]")
        contentView.item_input.text.insert(contentView.item_input.selectionEnd, "[/$code]")
        contentView.item_input.setSelection(contentView.item_input.selectionEnd-code.length-3)
    }

    private fun insertText(str: String){
        val contentView = contentView?:return
        contentView.item_input.text.replace(contentView.item_input.selectionStart, contentView.item_input.selectionEnd, str)
    }

    var hint: String = ""
    var callback: (String, Boolean)->Unit = {_, _->}
    var draft: String = ""
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val contentView = contentView?:inflater.inflate(R.layout.dialog_reply, container)
        this.contentView = contentView
        val emojiList = ArrayList<Pair<String,String>>()
        for(i in 1..23)
            emojiList.add(String.format("(bgm%02d)", i) to String.format("${Bangumi.SERVER}/img/smiles/bgm/%02d${if(i == 11 || i == 23)".gif" else ".png"}", i) )
        for(i in 1..100)
            emojiList.add(String.format("(bgm%02d)", i + 23) to String.format("${Bangumi.SERVER}/img/smiles/tv/%02d.gif", i) )
        "(=A=)|(=w=)|(-w=)|(S_S)|(=v=)|(@_@)|(=W=)|(TAT)|(T_T)|(='=)|(=3=)|(= =')|(=///=)|(=.,=)|(:P)|(LOL)".split("|").forEachIndexed {i, s->
            emojiList.add(s to "${Bangumi.SERVER}/img/smiles/${i+1}.gif") }
        val emojiAdapter = EmojiAdapter(emojiList)
        emojiAdapter.setOnItemChildClickListener { _, _, position ->
            insertText(emojiList[position].first)
        }
        contentView.item_btn_format.setOnClickListener {view->
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.list_format, popup.menu)
            popup.setOnMenuItemClickListener{
                insertCode(when(it.itemId){
                    R.id.format_bold -> "b"
                    R.id.format_italic -> "i"
                    R.id.format_strike -> "s"
                    R.id.format_underline -> "u"
                    R.id.format_mask -> "mask"
                    else -> return@setOnMenuItemClickListener true
                })
                true
            }
            popup.show()
        }
        contentView.item_emoji_list.adapter = emojiAdapter
        contentView.item_emoji_list.layoutManager = GridLayoutManager(context, 7)

        val updateEmojiList= {
            val softKeyboardHeight = getKeyBoardHeight()
            if(softKeyboardHeight > 200) {
                contentView.item_emoji_list.layoutParams.height = softKeyboardHeight
                contentView.item_emoji_list.layoutParams = contentView.item_emoji_list.layoutParams
            }
            contentView.item_emoji_list.visibility = if(softKeyboardHeight > 200 || contentView.item_btn_emoji.isSelected)
                View.VISIBLE else View.GONE
            (contentView.item_lock.layoutParams as LinearLayout.LayoutParams).weight = 1f
        }

        contentView.item_btn_image.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }
        contentView.item_btn_send.setOnClickListener {
            callback(contentView.item_input.text.toString(), true)
            callback = {_, _ -> }
            dismiss()
        }

        activity?.window?.decorView?.viewTreeObserver?.addOnGlobalLayoutListener {
            updateEmojiList()
            val softKeyboardHeight = getKeyBoardHeight()
            if(softKeyboardHeight > 200) {
                contentView.item_btn_emoji.isSelected = false
            }
        }
        val inputMethodManager = inflater.context.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        contentView.item_btn_emoji.setOnClickListener {
            contentView.item_btn_emoji.isSelected = !contentView.item_btn_emoji.isSelected
            val softKeyboardHeight = getKeyBoardHeight()
            if(softKeyboardHeight > 200 == contentView.item_btn_emoji.isSelected) {
                inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
            }
            updateEmojiList()
            if(contentView.item_emoji_list.visibility == View.GONE && softKeyboardHeight < 200){
                val layoutParams = contentView.item_lock.layoutParams as LinearLayout.LayoutParams
                layoutParams.height = contentView.item_lock.height
                layoutParams.weight = 0f
            }
        }

        dialog?.setOnKeyListener { _, keyCode, keyEvent -> if(keyEvent.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK && contentView.item_btn_emoji.isSelected){
            contentView.item_btn_emoji.isSelected = false
            updateEmojiList()
            true
        } else false}


        contentView.item_lock.setOnClickListener { dismiss() }
        contentView.item_hint.text = hint
        contentView.item_input.setText(draft)

        dialog?.window?.attributes?.let{
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
        if(contentView?.item_btn_emoji?.isSelected == false){
            contentView?.item_input?.postDelayed( {
                contentView?.item_input?.requestFocus()
                val inputMethodManager = (context?:return@postDelayed).applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
            }, 200)
        }
        val inputStream = activity?.contentResolver?.openInputStream(data?.data?:return)?:return
        val mimeType = activity?.contentResolver?.getType(data?.data?:return)?:"image/jpeg"
        val fileName = data?.data?.let { returnUri ->
            activity?.contentResolver?.query(returnUri, null, null, null, null)
        }?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }?:"image.jpg"
        val requestBody = RequestBody.create(MediaType.parse(mimeType),inputStream.readBytes())
        val body = MultipartBody.Part.createFormData("uploaded_file[]", fileName, requestBody)
        val call = UploadCC.createInstance().upload(body)
        call.enqueue(ApiHelper.buildCallback({
            insertText("[img]https://upload.cc/${it.success_image?.firstOrNull()?.url}[/img]")
        }, {}))
    }
}