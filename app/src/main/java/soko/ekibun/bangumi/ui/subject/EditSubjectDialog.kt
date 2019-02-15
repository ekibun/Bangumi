package soko.ekibun.bangumi.ui.subject

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.DisplayMetrics
import android.view.*
import android.widget.EditText
import kotlinx.android.synthetic.main.dialog_edit_subject.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.api.bangumi.bean.CollectionStatusType
import soko.ekibun.bangumi.api.bangumi.bean.Subject

class EditSubjectDialog: DialogFragment() {
    companion object {
        fun showDialog(context:AppCompatActivity, subject: Subject, status: Collection, formhash: String, ua: String, callback: (Boolean)->Unit){
            val dialog = EditSubjectDialog()
            dialog.subject = subject
            dialog.status = status
            dialog.status = status
            dialog.formhash = formhash
            dialog.callback = callback
            dialog.ua = ua
            dialog.show(context.supportFragmentManager, "edit")
        }
    }

    private fun getKeyBoardHeight(): Int{
        val rect = Rect()
        activity?.window?.decorView?.getWindowVisibleDisplayFrame(rect)
        val metrics = DisplayMetrics()
        (activity?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.getMetrics(metrics)
        return metrics.heightPixels - rect.bottom
    }

    lateinit var subject: Subject
    lateinit var status: Collection
    lateinit var formhash: String
    lateinit var ua: String
    lateinit var callback: (Boolean)->Unit
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_edit_subject, container)

        val selectMap = mapOf(
                CollectionStatusType.WISH to R.id.radio_wish,
                CollectionStatusType.COLLECT to R.id.radio_collect,
                CollectionStatusType.DO to R.id.radio_do,
                CollectionStatusType.ON_HOLD to R.id.radio_hold,
                CollectionStatusType.DROPPED to R.id.radio_dropped)
        val adapter = EditTagAdapter()
        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        view.item_tag_list.layoutManager = layoutManager
        view.item_tag_list.adapter = adapter
        if(status.status != null){
            view.item_subject_status.check(selectMap[status.status!!.type]!!)
            view.item_rating.rating = status.rating.toFloat()
            view.item_comment.setText(status.comment)
            view.item_private.isChecked = status.private == 1
            adapter.setNewData(status.tag?.filter { it.isNotEmpty() })
        }
        view.item_remove.visibility = if(formhash.isEmpty()) View.INVISIBLE else View.VISIBLE
        view.item_tag_add.setOnClickListener {
            val editText = EditText(view.context)
            AlertDialog.Builder(view.context)
                    .setView(editText)
                    .setTitle("添加标签")
                    .setPositiveButton("提交"){ _, _ ->
                        adapter.addData(editText.text.split(" ").filter { it.isNotEmpty() })
                    }.show()
        }

        view.item_remove.setOnClickListener {
            dialog.dismiss()
            callback(true)
        }
        view.item_outside.setOnClickListener {
            dialog.dismiss()
        }
        view.item_submit.setOnClickListener {
            dialog.dismiss()
            val newStatus = selectMap.toList().first { it.second == view.item_subject_status.checkedRadioButtonId }.first
            val newRating = view.item_rating.rating.toInt()
            val newComment = view.item_comment.text.toString()
            val newPrivacy = if(view.item_private.isChecked) 1 else 0
            val newTags = if(adapter.data.isNotEmpty()) adapter.data.reduce { acc, s -> "$acc $s" } else ""
            Bangumi.updateCollectionStatus(subject, formhash,ua,
                    newStatus, newTags, newComment, newRating, newPrivacy).enqueue(ApiHelper.buildCallback(context,{
                subject.interest = it
                callback(false)
            },{}))
        }

        activity?.window?.decorView?.viewTreeObserver?.addOnGlobalLayoutListener{
            view.item_keyboard.layoutParams?.let{
                it.height = getKeyBoardHeight()
                view.item_keyboard.layoutParams = it
            }
        }

        dialog.window?.attributes?.let{
            it.dimAmount = 0.6f
            dialog.window?.attributes = it
        }
        dialog.window?.setWindowAnimations(R.style.AnimDialog)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_Dialog)
    }
}