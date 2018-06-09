package soko.ekibun.bangumi.ui.video

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.entity.SectionEntity
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.subject_detail.*
import kotlinx.android.synthetic.main.subject_detail.view.*
import kotlinx.android.synthetic.main.video_player.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.bangumi.bean.SubjectProgress
import soko.ekibun.bangumi.api.bangumi.bean.SubjectType
import soko.ekibun.bangumi.ui.main.fragment.calendar.CalendarAdapter
import soko.ekibun.bangumi.util.JsonUtil

class SubjectView(private val context: VideoActivity){
    val episodeAdapter = EpisodeAdapter()
    val headerView = context.layoutInflater.inflate(R.layout.video_header, context.root_layout, false)!!

    init{
        context.episode_list.adapter = episodeAdapter
        context.episode_list.layoutManager = LinearLayoutManager(context)
        episodeAdapter.setHeaderView(headerView)
    }

    private fun updateEpisode(episodes: List<Episode>){
        val maps = HashMap<Int, List<Episode>>()
        episodes.forEach {
            maps[it.type] = (maps[it.type]?:ArrayList()).plus(it)
        }
        episodeAdapter.setNewData(null)
        maps.forEach {
            episodeAdapter.addData(object: SectionEntity<Episode>(true, Episode.getTypeName(it.key)){})
            it.value.forEach {
                episodeAdapter.addData(object: SectionEntity<Episode>(it){})
            }
        }
        progress = progress
    }

    private fun parseSubject(subject: Subject): String{
        var ret = subject.name + "\n" +
                "总集数：${subject.eps_count}\n" +
                "开播时间：${subject.air_date}\n" +
                "更新时间："
        subject.air_weekday.toString().forEach {
            ret += CalendarAdapter.weekSmall[it.toString().toInt()] + " "
        }
        return ret
    }

    var progress: SubjectProgress? = null
        set(value) {
            episodeAdapter.data.forEach { ep ->
                if (ep.t != null) {
                    ep.t.progress = null
                    value?.eps?.forEach {
                        if (ep.t.id == it.id) {
                            ep.t.progress = it
                            //return@loop
                        }
                    }
                }
            }
            episodeAdapter.notifyDataSetChanged()
            field = value
        }

    fun updateSubject(subject: Subject){
        if(context.isDestroyed) return
        //context.nested_scroll.tag = true
        //context.data_layout.visibility = View.VISIBLE
        context.title_text.text = if(subject.name_cn.isNullOrEmpty()) subject.name else subject.name_cn
        context.title_site.text = SubjectType.getDescription(subject.type)
        //item_title.text = title_text.text
        //context.item_summary.text = subject.summary
        headerView.item_info.text = parseSubject(subject)

        subject.rating?.let {
            headerView.item_score.text = it.score.toString()
            headerView.item_score_count.text = context.getString(R.string.rate_count, it.total)
        }
        Glide.with(context)
                .applyDefaultRequestOptions(RequestOptions.placeholderOf(headerView.item_cover.drawable))
                .load(subject.images?.common)
                .into(headerView.item_cover)

        Glide.with(context)
                .applyDefaultRequestOptions(RequestOptions.placeholderOf(context.item_cover_blur.drawable))
                .load(subject.images?.common)
                .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 8)))
                .into(context.item_cover_blur)
        ((subject.eps as? List<*>)?.map{ JsonUtil.toEntity(JsonUtil.toJson(it!!), Episode::class.java)})?.let{
            updateEpisode(it)
        }
    }
}