package soko.ekibun.bangumi

import okhttp3.*
import org.junit.Test
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun saxTest() {
        var finished = false
        println("start")
        OkHttpClient().newCall(
                Request.Builder().url("http://bgm.tv/rakuen/topic/subject/892")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36")
                        .header("Cookie", "__cfduid=d6ba19febd00126f1058841dcb81d86461570963950; chii_theme=light; __utmc=1; __utmz=1.1570963955.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); chii_sid=v22Uc4; __utma=1.1178798330.1570963955.1570968508.1570971165.3; __utmt=1; chii_cookietime=2592000; chii_auth=rV88pgR2KrdKGuubAWYyezM7aiwNfTi4Gv8ozHGO4guEz2CQzxJ72RzH%2BnLRHpbH7pXyewa28yfQzE3LESd9wlZ4H%2F5Hz53kaHaZ; __utmb=1.2.10.1570971165").build()
        ).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                finished = true
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                println("onResponse")
                println("onResponse ${response.body()!!.string().length}")
                finished = true
                /*
                try{
                    val parser = XmlPullParserFactory.newInstance().newPullParser()
                    parser.setInput(response.body()!!.charStream())
                    var lastData = ""
                    var tagDepth = 0
                    val updateReply = {
                        val it = Jsoup.parse(lastData)
                        it.outputSettings().prettyPrint(false)

                        val user = it.selectFirst(".inner a")
                        val data = (it.selectFirst(".icons_cmt")?.attr("onclick") ?: "").split(",")
                        val relate = data.getOrNull(2)?.toIntOrNull() ?: 0
                        val post_id = data.getOrNull(3)?.toIntOrNull() ?: 0
                        println(TopicPost(
                                pst_id = (if (post_id == 0) relate else post_id).toString(),
                                pst_mid = data.getOrNull(1) ?: "",
                                pst_uid = data.getOrNull(5) ?: "",
                                pst_content = it.selectFirst(".topic_content")?.html()
                                        ?: it.selectFirst(".message")?.html()
                                        ?: it.selectFirst(".cmt_sub_content")?.html() ?: "",
                                username = Regex("""/user/([^/]*)""").find(user?.attr("href")
                                        ?: "")?.groupValues?.get(1) ?: "",
                                nickname = user?.text() ?: "",
                                sign = it.selectFirst(".inner .tip_j")?.text() ?: "",
                                avatar = Regex("""background-image:url\('([^']*)'\)""").find(it.selectFirst(".avatar")?.html()
                                        ?: "")?.groupValues?.get(1) ?: "",
                                dateline = it.selectFirst(".re_info")?.text()?.split("/")?.get(0)?.trim()?.substringAfter(" - ")
                                        ?: "",
                                is_self = it.selectFirst(".re_info")?.text()?.contains("/") == true,
                                isSub = it.selectFirst(".re_info a")?.text()?.contains("-") ?: false,
                                editable = it.selectFirst(".re_info")?.text()?.contains("/") == true,
                                relate = relate.toString(),
                                model = Regex("'([^']*)'").find(data.getOrNull(0) ?: "")?.groupValues?.get(1) ?: ""
                        ))
                    }
                    while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                        when(parser.eventType){
                            XmlPullParser.START_TAG -> {
                                if(parser.getAttributeValue("", "id")?.startsWith("post_") == true){
                                    if(tagDepth != 0){
                                        updateReply()
                                    }else {
                                        val doc = Jsoup.parse(lastData)
                                        println("group=${ doc.selectFirst("#pageHeader span")?.text() ?: ""}")
                                        println("title=${ doc.selectFirst("#pageHeader h1")?.ownText() ?: ""}")
                                        println("images=${ doc.selectFirst("#pageHeader img")?.ownText() ?: ""}")
                                    }
                                    tagDepth = parser.depth
                                    lastData = ""
                                }else if(tagDepth != 0 && parser.getAttributeValue("", "id")?.contains("reply_wrapper") == true){
                                    updateReply()
                                    lastData = ""
                                    tagDepth = 0
                                }
                                lastData += "<${parser.name} ${(0 until parser.attributeCount).joinToString(" ") { "${parser.getAttributeName(it)}=\"${parser.getAttributeValue(it)}\"" }}>"
                            }
                            XmlPullParser.END_TAG -> {
                                lastData += "</${parser.name}>"
                            }
                            XmlPullParser.TEXT -> {
                                lastData += parser.text
                            }
                        }
                        try {
                            parser.next()
                        } catch (e: XmlPullParserException) {
                        }
                    }

                    val rest = Jsoup.parse(lastData)
                    val error = rest.selectFirst("#reply_wrapper")?.selectFirst(".tip")
                    val form = rest.selectFirst("#ReplyForm")
                    println("error=${error?.text()}")
                    println("errorLink=${error?.selectFirst("a")?.attr("href") ?: ""}")
                    println("post=${"${form?.attr("action")}?ajax=1"}")
                    println("lastview=${form?.selectFirst("input[name=lastview]")?.attr("value")}")
                } finally {
                    finished = true
                }

                 */
            }
        })

        while (!finished) Thread.sleep(100)
    }

    @Test
    fun timeWrapper() {
        val time = "2019-06-22"

        val calendar = Calendar.getInstance()
        print("${calendar.time}\n")
        if (time.endsWith("ago")) {
            Regex("(\\d+)([dhm])").findAll(time).map { it.groupValues }.forEach {
                calendar.add(when (it[2]) {
                    "d" -> Calendar.DAY_OF_MONTH
                    "h" -> Calendar.HOUR
                    "m" -> Calendar.SECOND
                    else -> return@forEach
                }, -(it[1].toIntOrNull() ?: 0))
            }
            print(calendar.time)
        } else if (time.endsWith("前")) {
            Regex("(\\d+)(年|月|天|小时|分|秒)").findAll(time).map { it.groupValues }.forEach {
                calendar.add(when (it[2]) {
                    "年" -> Calendar.YEAR
                    "月" -> Calendar.MONTH
                    "天" -> Calendar.DAY_OF_MONTH
                    "小时" -> Calendar.HOUR
                    "分" -> Calendar.MINUTE
                    "秒" -> Calendar.SECOND
                    else -> return@forEach
                }, -(it[1].toIntOrNull() ?: 0))
            }
            print(calendar.time)
        }
        try {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(time)
        } catch (e: Exception) {
        }


    }
}
