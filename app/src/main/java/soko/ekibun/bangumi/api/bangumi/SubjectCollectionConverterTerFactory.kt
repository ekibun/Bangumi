package soko.ekibun.bangumi.api.bangumi

import okhttp3.ResponseBody
import org.jsoup.Jsoup
import retrofit2.Converter
import retrofit2.Retrofit
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.bangumi.bean.SubjectCollection
import java.lang.reflect.Type

class SubjectCollectionConverterTerFactory: Converter.Factory() {
    override fun responseBodyConverter(type: Type, annotations: Array<out Annotation>, retrofit: Retrofit): Converter<ResponseBody, *>? {
        return SubjectCollectionBodyConverter()
    }

    class SubjectCollectionBodyConverter: Converter<ResponseBody, List<SubjectCollection>>{
        override fun convert(value: ResponseBody): List<SubjectCollection>? {
            val doc = Jsoup.parse(value.string())
            val ret = ArrayList<SubjectCollection>()
            doc.select(".item").forEach {
                it.attr("id").split('_').getOrNull(1)?.toIntOrNull()?.let{id->
                    val nameCN = it.selectFirst("h3").selectFirst("a").text()
                    val name = it.selectFirst("h3").selectFirst("small")?.text()?:nameCN
                    val img = "http:" + it.selectFirst("img").attr("src").replace("cover/s/", "cover/m/")
                    val subject = Subject(id,
                            Bangumi.SERVER + it.selectFirst("a").attr("href"),
                            0,
                            name,
                            nameCN,
                            it.selectFirst(".info").text(),
                            images = Images(img, img, img, img, img)
                    )
                    ret += SubjectCollection(name, id, -1, -1, subject = subject)
                }
                //[{"id":6776,"url":"http://bgm.tv/subject/6776","type":2,"name":"名探偵コナン コナンvsキッドvsヤイバ 宝刀争奪大決戦!!","name_cn":"柯南对基德对铁剑 宝刀争夺大决战","summary":"","air_date":"","air_weekday":0,"images":{"large":"http://lain.bgm.tv/pic/cover/l/e5/ba/6776_bA2h2.jpg","common":"http://lain.bgm.tv/pic/cover/c/e5/ba/6776_bA2h2.jpg","medium":"http://lain.bgm.tv/pic/cover/m/e5/ba/6776_bA2h2.jpg","small":"http://lain.bgm.tv/pic/cover/s/e5/ba/6776_bA2h2.jpg","grid":"http://lain.bgm.tv/pic/cover/g/e5/ba/6776_bA2h2.jpg"}}]
            }
            return ret
        }
    }
}