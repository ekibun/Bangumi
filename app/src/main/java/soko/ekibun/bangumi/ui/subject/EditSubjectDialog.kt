package soko.ekibun.bangumi.ui.subject

import android.app.Dialog
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import android.util.DisplayMetrics
import android.view.*
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.dialog_edit_subject.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.api.bangumi.bean.CollectionStatusType
import soko.ekibun.bangumi.api.bangumi.bean.Subject

class EditSubjectDialog(context: Context): Dialog(context, R.style.AppTheme_Dialog) {
    companion object {
        fun showDialog(context: Context, subject: Subject, status: Collection, formhash: String, ua: String, callback: ()->Unit){
            val dialog = EditSubjectDialog(context)
            dialog.subject = subject
            dialog.status = status
            dialog.formhash = formhash
            dialog.ua = ua
            dialog.setOnDismissListener { callback() }
            dialog.show()
        }
    }

    private fun getKeyBoardHeight(): Int{
        val rect = Rect()
        window?.decorView?.getWindowVisibleDisplayFrame(rect)
        val metrics = DisplayMetrics()
        (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.getMetrics(metrics)
        return metrics.heightPixels - rect.bottom
    }

    lateinit var subject: Subject
    lateinit var status: Collection
    lateinit var formhash: String
    lateinit var ua: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_subject, null)
        setContentView(view)

        val collectionStatusNames = context.resources.getStringArray(CollectionStatusType.getTypeNamesResId(subject.type))
        view.radio_wish.text = collectionStatusNames[0]
        view.radio_collect.text = collectionStatusNames[1]
        view.radio_do.text = collectionStatusNames[2]
        view.radio_hold.text = collectionStatusNames[3]
        view.radio_dropped.text = collectionStatusNames[4]

        val selectMap = mapOf(
                CollectionStatusType.WISH to R.id.radio_wish,
                CollectionStatusType.COLLECT to R.id.radio_collect,
                CollectionStatusType.DO to R.id.radio_do,
                CollectionStatusType.ON_HOLD to R.id.radio_hold,
                CollectionStatusType.DROPPED to R.id.radio_dropped)
        val adapter = EditTagAdapter()
        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = RecyclerView.HORIZONTAL
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
                    .setTitle(R.string.subject_dialog_add_tag)
                    .setPositiveButton(R.string.submit){ _, _ ->
                        adapter.addData(editText.text.split(" ").filter { it.isNotEmpty() })
                    }.show()
        }

        view.item_remove.setOnClickListener {
            AlertDialog.Builder(context).setTitle(R.string.collection_dialog_remove)
                    .setNegativeButton(R.string.cancel) { _, _ -> }.setPositiveButton(R.string.ok) { _, _ ->
                        ApiHelper.buildHttpCall("${Bangumi.SERVER}/subject/${subject.id}/remove?gh=$formhash", mapOf("User-Agent" to ua)){ it.code() == 200 }
                                .enqueue(ApiHelper.buildCallback(context, {
                                    if(it) subject.interest = Collection()
                                    dismiss()
                                }, {}))
                    }.show()
        }
        view.item_outside.setOnClickListener {
            dismiss()
        }
        view.item_submit.setOnClickListener {
            val newStatus = selectMap.toList().first { it.second == view.item_subject_status.checkedRadioButtonId }.first
            val newRating = view.item_rating.rating.toInt()
            val newComment = view.item_comment.text.toString()
            val newPrivacy = if(view.item_private.isChecked) 1 else 0
            val newTags = if(adapter.data.isNotEmpty()) adapter.data.reduce { acc, s -> "$acc $s" } else ""
            Bangumi.updateCollectionStatus(subject, formhash,ua,
                    newStatus, newTags, newComment, newRating, newPrivacy).enqueue(ApiHelper.buildCallback(context,{
                subject.interest = it
                dismiss()
            },{}))
        }

        window?.decorView?.viewTreeObserver?.addOnGlobalLayoutListener{
            view.item_keyboard.layoutParams?.let{
                it.height = getKeyBoardHeight()
                view.item_keyboard.layoutParams = it
            }
        }

        window?.attributes?.let{
            it.dimAmount = 0.6f
            window?.attributes = it
        }
        window?.setWindowAnimations(R.style.AnimDialog)
        window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
}