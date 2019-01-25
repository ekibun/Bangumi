package soko.ekibun.bangumi.ui.main.fragment.home.fragment.timeline

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.View
import kotlinx.android.synthetic.main.fragment_timeline.*
import okhttp3.FormBody
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.ui.main.MainActivity
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.HomeTabFragment
import soko.ekibun.bangumi.ui.topic.ReplyDialog

class TimeLineFragment: HomeTabFragment(R.layout.fragment_timeline){
    override val titleRes: Int = R.string.timeline
    override val iconRes: Int = R.drawable.ic_timelapse

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = TimeLinePagerAdapter(view.context, this, item_pager)
        item_pager?.adapter = adapter
        item_tabs?.setupWithViewPager(item_pager)
        onSelect()
    }

    override fun onSelect() {
        val adapter = (item_pager?.adapter as? TimeLinePagerAdapter)?:return
        val formhash = (activity as? MainActivity)?.formhash?:""
        if(formhash.isEmpty()){
            item_new?.hide()
        }else {
            item_new?.show()
            var draft = ""
            item_new?.setOnClickListener {
                val dialog = ReplyDialog()
                dialog.hint = "添加动态"
                dialog.draft = draft
                dialog.callback = {string, send ->
                    if(send){
                        ApiHelper.buildHttpCall("${Bangumi.SERVER}/update/user/say?ajax=1", body = FormBody.Builder()
                                .add("say_input", string)
                                .add("formhash", formhash)
                                .add("submit", "submit").build()){
                            it.body()?.string()?.contains("\"status\":\"ok\"") == true
                        }.enqueue(ApiHelper.buildCallback(context, {
                            if(it){
                                draft = ""
                                if(item_pager?.currentItem?:2 !in 0..1) item_pager?.currentItem = 1
                                adapter.pageIndex[item_pager?.currentItem?:0] = 0
                                adapter.loadTopicList()
                            }else Snackbar.make(item_pager, "提交出现了一些问题，请稍后再试", Snackbar.LENGTH_SHORT).show()
                        }){ })
                    }else draft = string
                }
                dialog.show(activity?.supportFragmentManager?:return@setOnClickListener, "say")
            }
            adapter.pageIndex[item_pager?.currentItem?:0] = adapter.pageIndex[item_pager?.currentItem?:0]?:0
            if(adapter.pageIndex[item_pager?.currentItem?:0] == 0) adapter.loadTopicList()
        }
    }
}