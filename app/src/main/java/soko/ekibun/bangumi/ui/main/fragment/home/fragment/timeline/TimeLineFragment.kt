package soko.ekibun.bangumi.ui.main.fragment.home.fragment.timeline

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.View
import android.webkit.CookieManager
import kotlinx.android.synthetic.main.fragment_timeline.*
import okhttp3.FormBody
import org.jsoup.Jsoup
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
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
        item_new?.hide()

        ApiHelper.buildHttpCall(Bangumi.SERVER,  mapOf("cookie" to CookieManager.getInstance().getCookie(Bangumi.SERVER))){
            val doc = Jsoup.parse(it.body()?.string()?:"")
            doc.selectFirst("input[name=formhash]")?.attr("value")
        }.enqueue(ApiHelper.buildCallback(view.context, {formhash->
            if(formhash.isNullOrEmpty()) return@buildCallback
            item_new?.show()
            var draft = ""
            item_new?.setOnClickListener {
                val dialog = ReplyDialog()
                dialog.hint = "添加动态"
                dialog.draft = draft
                dialog.callback = {string, send ->
                    if(send){
                        ApiHelper.buildHttpCall("${Bangumi.SERVER}/update/user/say?ajax=1",  mapOf("cookie" to CookieManager.getInstance().getCookie(Bangumi.SERVER)), FormBody.Builder()
                                .add("say_input", string)
                                .add("formhash", formhash?:"")
                                .add("submit", "submit").build()){
                            it.body()?.string()?.contains("\"status\":\"ok\"") == true
                        }.enqueue(ApiHelper.buildCallback(view.context, {
                            if(it){
                                draft = ""
                                if(item_pager?.currentItem?:2 !in 0..1) item_pager?.currentItem = 1
                                adapter.pageIndex[item_pager?.currentItem?:0] = 0
                                adapter.loadTopicList()
                            }else Snackbar.make(view, "提交出现了一些问题，请稍后再试", Snackbar.LENGTH_SHORT).show()
                        }){ })
                    }else draft = string
                }
                dialog.show(activity?.supportFragmentManager?:return@setOnClickListener, "say")
            }
            adapter.pageIndex[item_pager?.currentItem?:0] = adapter.pageIndex[item_pager?.currentItem?:0]?:0
            adapter.loadTopicList()
        }))
    }

    override fun onSelect() {
        //TODO
    }
}