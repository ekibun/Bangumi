package soko.ekibun.bangumi.ui.say

import kotlinx.android.synthetic.main.activity_topic.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Say
import soko.ekibun.bangumi.model.DataCacheModel

class SayPresenter(private val context: SayActivity, say: Say) {
    val sayView = SayView(context)

    var say: Say
    val dataCacheModel by lazy { App.get(context).dataCacheModel }

    init {
        val self = say.self
        DataCacheModel.merge(say, dataCacheModel.get(say.cacheKey))
        say.self = self
        this.say = say
        sayView.processSay(say)
        getSay()

        context.item_swipe.setOnRefreshListener {
            getSay()
        }
    }

    fun getSay() {
        context.item_swipe.isRefreshing = true
        Say.getSay(say).enqueue(ApiHelper.buildCallback({ say ->
            sayView.processSay(say)
            dataCacheModel.set(say.cacheKey, say)
        }) {
            context.item_swipe.isRefreshing = false
        })
    }
}