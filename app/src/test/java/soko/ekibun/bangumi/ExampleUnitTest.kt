package soko.ekibun.bangumi

import okhttp3.*
import org.jsoup.Jsoup
import org.junit.Test
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
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
                try{
                    val parser = XmlPullParserFactory.newInstance().newPullParser()
                    parser.setInput(response.body()!!.charStream())
                    var lastData = ""
                    var tagDepth = 0
                    val updateData = {
                        println(lastData)
                    }
                    while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                        when(parser.eventType){
                            XmlPullParser.START_TAG -> {
                                if (parser.getAttributeValue("", "id")?.startsWith("subjectPanel_") == true) {
                                    if (parser.getAttributeValue("", "id")?.startsWith("post_") == true) {
                                        if (tagDepth != 0) {
                                            updateData()
                                        } else {
                                            val doc = Jsoup.parse(lastData)
                                            println("group=${doc.selectFirst("#pageHeader span")?.text() ?: ""}")
                                            println("title=${doc.selectFirst("#pageHeader h1")?.ownText() ?: ""}")
                                            println("images=${doc.selectFirst("#pageHeader img")?.ownText() ?: ""}")
                                        }
                                        tagDepth = parser.depth
                                        lastData = ""
                                    } else if (tagDepth != 0 && parser.getAttributeValue("", "id")?.contains("reply_wrapper") == true) {
                                        updateData()
                                        lastData = ""
                                        tagDepth = 0
                                    }
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
                } finally {
                    finished = true
                }
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
