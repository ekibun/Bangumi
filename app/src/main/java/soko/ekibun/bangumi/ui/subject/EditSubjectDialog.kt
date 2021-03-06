package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.base_dialog.view.*
import kotlinx.android.synthetic.main.dialog_edit_subject.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.ui.view.BaseActivity
import soko.ekibun.bangumi.ui.view.BaseDialog
import soko.ekibun.bangumi.ui.view.ShadowDecoration
import soko.ekibun.bangumi.util.HttpUtil

/**
 * 收藏对话框
 * @property title String
 * @property subject Subject
 * @property collection Collection
 * @property callback Function0<Unit>
 */
class EditSubjectDialog : BaseDialog(R.layout.base_dialog) {
    override val title: String get() = context!!.getString(R.string.dialog_subject_edit_title)

    companion object {
        /**
         * 显示对话框
         * @param fragmentManager FragmentManager
         * @param subject Subject
         * @param status Collection
         * @param callback Function0<Unit>
         */
        fun showDialog(fragmentManager: FragmentManager, subject: Subject, status: Collection, callback: () -> Unit) {
            val dialog = EditSubjectDialog()
            dialog.subject = subject
            dialog.collection = status
            dialog.callback = callback
            dialog.show(fragmentManager, "collect")
        }
    }

    lateinit var subject: Subject
    lateinit var collection: Collection
    lateinit var callback: () -> Unit

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View) {
        LayoutInflater.from(context).inflate(R.layout.dialog_edit_subject, view.layout_content)
        val collectionStatusNames = view.context.resources.getStringArray(Collection.getStatusNamesRes(subject.type))
        view.radio_wish.text = collectionStatusNames[0]
        view.radio_collect.text = collectionStatusNames[1]
        view.radio_do.text = collectionStatusNames[2]
        view.radio_hold.text = collectionStatusNames[3]
        view.radio_dropped.text = collectionStatusNames[4]

        val selectMap = mapOf(
            Collection.STATUS_WISH to R.id.radio_wish,
            Collection.STATUS_COLLECT to R.id.radio_collect,
            Collection.STATUS_DO to R.id.radio_do,
            Collection.STATUS_ON_HOLD to R.id.radio_hold,
            Collection.STATUS_DROPPED to R.id.radio_dropped
        )
        view.findViewById<RadioButton>(selectMap[collection.status] ?: R.id.radio_collect)?.isChecked = true
        view.item_rating.rating = collection.rating.toFloat()
        view.item_tags.setText(collection.tag?.joinToString(" "))
        view.item_comment.setText(collection.comment)

        val hasTag = { tag: String ->
            view.item_tags.editableText.split(" ").contains(tag)
        }
        val myTagAdapter = TagAdapter(null, hasTag)
        myTagAdapter.setNewInstance(collection.myTag?.map { it to 0 }?.toMutableList())
        view.item_my_tag_list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        ShadowDecoration.set(view.item_my_tag_list, drawEnd = true)
        view.item_my_tag_list.adapter = myTagAdapter
        myTagAdapter.setOnItemClickListener { _, _, position ->
            val tag = myTagAdapter.data[position].first
            if (!hasTag(tag)) view.item_tags.setText("${view.item_tags.editableText.trim()} $tag".trim())
            else view.item_tags.setText(
                view.item_tags.editableText.trim().toString().replace(tag, "").replace("  ", " ")
            )
        }
        val userTagAdapter = TagAdapter(null, hasTag)
        userTagAdapter.setNewInstance(subject.tags?.toMutableList())
        view.item_user_tag_list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        ShadowDecoration.set(view.item_user_tag_list, drawEnd = true)
        view.item_user_tag_list.adapter = userTagAdapter
        userTagAdapter.setOnItemClickListener { _, _, position ->
            val tag = userTagAdapter.data[position].first
            if (!hasTag(tag)) view.item_tags.setText("${view.item_tags.editableText.trim()} $tag".trim())
            else view.item_tags.setText(
                view.item_tags.editableText.trim().toString().replace(tag, "").replace("  ", " ")
            )
        }
        view.item_tags.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                myTagAdapter.notifyDataSetChanged()
                userTagAdapter.notifyDataSetChanged()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { /* no-op */
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { /* no-op */
            }
        })

        view.item_remove.visibility = if (HttpUtil.formhash.isEmpty()) View.INVISIBLE else View.VISIBLE
        view.item_remove.setOnClickListener {
            AlertDialog.Builder(view.context).setTitle(R.string.collection_dialog_remove)
                .setNegativeButton(R.string.cancel) { _, _ -> }.setPositiveButton(R.string.ok) { _, _ ->
                    (activity as BaseActivity).subscribe {
                        Collection.remove(subject)
                        subject.collect = Collection()
                        dismiss()
                    }
                }.show()
        }
        view.item_submit.setOnClickListener {
            (activity as BaseActivity).subscribe {
                val collection = Collection(
                    status = selectMap.toList()
                        .first { it.second == view.item_subject_status.checkedRadioButtonId }.first,
                    rating = view.item_rating.rating.toInt(),
                    comment = view.item_comment.text.toString(),
                    private = if (view.item_private.isChecked) 1 else 0,
                    tag = view.item_tags.editableText.split(" ")
                )
                Collection.updateStatus(subject, collection)
                subject.collect = collection
                callback()
                dismiss()
            }
        }

        val paddingBottom = view.item_buttons.paddingBottom
        view.setOnApplyWindowInsetsListener { _, insets ->
            view.setPadding(
                view.item_buttons.paddingLeft,
                view.item_buttons.paddingTop,
                view.item_buttons.paddingRight,
                paddingBottom + insets.systemWindowInsetBottom
            )
            insets.consumeSystemWindowInsets()
        }
    }
}