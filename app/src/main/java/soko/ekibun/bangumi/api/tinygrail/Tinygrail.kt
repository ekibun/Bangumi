package soko.ekibun.bangumi.api.tinygrail

import org.jsoup.Jsoup
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.bangumi.bean.SubjectType
import soko.ekibun.bangumi.api.tinygrail.bean.OnAir

interface Tinygrail{
    companion object {
        fun onAirList(): Call<Map<Int, Map<String,List<OnAir>>>> {
            return ApiHelper.buildHttpCall("https://www.tinygrail.com/api/onair/"){
                val doc = Jsoup.parse(it.body()?.string()?:"")
                val ret = HashMap<Int, HashMap<String,ArrayList<OnAir>>>()
                doc.select(".daily_on_air").forEach {
                    val dateList = ret.getOrPut(it.id()?.toIntOrNull()?:0) { HashMap() }
                    it.select(".subject_on_air").forEach {
                        val timeList = dateList.getOrPut(it.selectFirst(".subject_title").selectFirst("span").text()){ ArrayList() }
                        val subject = it.selectFirst(".subject_title")?.selectFirst("a")
                        val url = subject?.attr("href")
                        val img = it.selectFirst(".subject_icon")?.attr("data-src")
                        val subjectId = Regex("""/subject/([0-9]*)""").find(url?:"")?.groupValues?.get(1)?.toIntOrNull()?:0
                        val subjectName = subject?.text()
                        val episodeId = it.id().substring(2).toIntOrNull()?:0
                        val episodeTitle = it.selectFirst(".subject_info")?.selectFirst("a")?.text()?:""
                        val sort = Regex("""ep ([0-9]*)""").find(episodeTitle)?.groupValues?.get(1)?.toFloatOrNull()?:0f
                        val siteList = ArrayList<OnAir.Site>()
                        it.select(".episode_tag").forEach {
                            val a = it.selectFirst("a")
                            siteList+=OnAir.Site(a.text(), a.attr("href"),
                                    !it.classNames().contains("deactive")) }
                        timeList.add(OnAir(
                                Episode(episodeId, "${Bangumi.SERVER}/ep/$episodeId", sort = sort, name = episodeTitle),
                                Subject(subjectId, url, SubjectType.ANIME, subjectName, images = Images(img, img, img, img, img)),
                                siteList))
                    }
                }
                return@buildHttpCall ret
            }
        }
    }
}