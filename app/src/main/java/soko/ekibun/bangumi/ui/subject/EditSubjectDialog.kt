package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.dialog_edit_subject.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.util.HttpUtil

class EditSubjectDialog(context: Context) : Dialog(context, R.style.AppTheme_Dialog) {
    companion object {
        fun showDialog(context: Context, subject: Subject, status: Collection, callback: () -> Unit) {
            val dialog = EditSubjectDialog(context)
            dialog.subject = subject
            dialog.collection = status
            dialog.setOnDismissListener { callback() }
            dialog.show()
        }
    }

    lateinit var subject: Subject
    lateinit var collection: Collection
    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_subject, null)
        setContentView(view)

        val collectionStatusNames = context.resources.getStringArray(Collection.getTypeNamesRes(subject.type))
        view.radio_wish.text = collectionStatusNames[0]
        view.radio_collect.text = collectionStatusNames[1]
        view.radio_do.text = collectionStatusNames[2]
        view.radio_hold.text = collectionStatusNames[3]
        view.radio_dropped.text = collectionStatusNames[4]

        val selectMap = mapOf(
                Collection.TYPE_WISH to R.id.radio_wish,
                Collection.TYPE_COLLECT to R.id.radio_collect,
                Collection.TYPE_DO to R.id.radio_do,
                Collection.TYPE_ON_HOLD to R.id.radio_hold,
                Collection.TYPE_DROPPED to R.id.radio_dropped)
        val adapter = EditTagAdapter()
        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = RecyclerView.HORIZONTAL
        view.item_tag_list.layoutManager = layoutManager
        view.item_tag_list.adapter = adapter
        if (collection.status != null) {
            view.item_subject_status.check(selectMap.getValue(collection.status!!))
            view.item_rating.rating = collection.rating.toFloat()
            view.item_comment.setText(collection.comment)
            view.item_private.isChecked = collection.private == 1
            adapter.setNewData(collection.tag?.filter { it.isNotEmpty() })
        }
        view.item_remove.visibility = if (HttpUtil.formhash.isEmpty()) View.INVISIBLE else View.VISIBLE
        view.item_tag_add.setOnClickListener {
            val editText = EditText(view.context)
            AlertDialog.Builder(view.context)
                    .setView(editText)
                    .setTitle(R.string.subject_dialog_add_tag)
                    .setPositiveButton(R.string.submit) { _, _ ->
                        adapter.addData(editText.text.split(" ").filter { it.isNotEmpty() })
                    }.show()
        }

        view.item_remove.setOnClickListener {
            AlertDialog.Builder(context).setTitle(R.string.collection_dialog_remove)
                    .setNegativeButton(R.string.cancel) { _, _ -> }.setPositiveButton(R.string.ok) { _, _ ->
                        Bangumi.removeCollection(subject).enqueue(ApiHelper.buildCallback({
                            if (it) subject.collect = Collection()
                            dismiss()
                        }, {}))
                    }.show()
        }
        view.item_outside.setOnClickListener {
            dismiss()
        }
        view.item_submit.setOnClickListener {
            Bangumi.updateCollectionStatus(subject, Collection(
                    status = selectMap.toList().first { it.second == view.item_subject_status.checkedRadioButtonId }.first,
                    rating = view.item_rating.rating.toInt(),
                    comment = view.item_comment.text.toString(),
                    private = if (view.item_private.isChecked) 1 else 0,
                    tag = adapter.data
            )).enqueue(ApiHelper.buildCallback({
                subject.collect = it
                dismiss()
            }, {}))
        }

        val paddingBottom = view.item_container.paddingBottom
        view.setOnApplyWindowInsetsListener { _, insets ->
            view.item_container.setPadding(view.item_container.paddingLeft, view.item_container.paddingTop, view.item_container.paddingRight, paddingBottom + insets.systemWindowInsetBottom)
            insets.consumeSystemWindowInsets()
        }

        window?.let { ThemeModel.updateNavigationTheme(it, view.context) }

        window?.attributes?.let {
            it.dimAmount = 0.6f
            window?.attributes = it
        }
        window?.setWindowAnimations(R.style.AnimDialog)
        window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
}