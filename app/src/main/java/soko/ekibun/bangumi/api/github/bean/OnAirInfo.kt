package soko.ekibun.bangumi.api.github.bean

/**
 * 放送信息
 * @property id Int
 * @property name String
 * @property sites List<SubjectSite>
 * @property eps List<Ep>
 * @constructor
 */
data class OnAirInfo (
        val id: Int,
        val name: String,
        val sites: List<SubjectSite>,
        val eps: List<Ep>
){
    /**
     * 剧集信息
     * @property id Int
     * @property name String
     * @property name_cn String
     * @property sites List<Site>
     * @constructor
     */
    data class Ep(
            val id: Int,
            val name: String,
            val name_cn: String,
            val sites: List<Site>
    )

    /**
     * 站点信息
     * @property site String
     * @property title String
     * @property url String
     * @property color Int
     * @constructor
     */
    open class Site(
            val site: String,
            private val title: String = "",
            private val url: String = ""
    ) {
        open fun title() = title
        open fun url() = url
        val color
            get() = (0xff000000 + when (site) {
                "offical" -> 0x888888

                "bangumi" -> 0xf09199
                "saraba1st" -> 0x76cec9

                "acfun" -> 0xfd4c5b
                "bilibili" -> 0xf25d8e
                "tucao" -> 0xe71158
                "sohu" -> 0xd6000a
                "youku" -> 0x1ebeff
                "tudou" -> 0xff6600
                "qq" -> 0xff820f
                "iqiyi" -> 0x00be06
                "letv" -> 0xe42112
                "pptv" -> 0x00a0e9
                "kankan" -> 0x24baf1
                "mgtv" -> 0xff5f00
                "nicovideo" -> 0x060102
                "netflix" -> 0xe50914

                "dmhy" -> 0x224477
                "nyaa" -> 0x99daa9
                else -> 0
            }).toInt()
    }

    /**
     * 站点信息
     * @property id String
     * @property week Int
     * @property time String?
     * @constructor
     */
    class SubjectSite(
            site: String,
            val id: String,
            val week: Int = 0,
            val time: String? = null
    ) : Site(site) {
        override fun title() = if (week > 0) "每周${"一二三四五六日"[week - 1]} ${time?.replace(Regex("""(\d{2})(\d{2})"""), "$1:$2")}更新" else id
        override fun url() = when (site) {
            "bangumi" -> "http://bangumi.tv/subject/$id"
            "saraba1st" -> "https://bbs.saraba1st.com/2b/thread-$id-1-1.html"

            "acfun" -> "https://www.acfun.cn/bangumi/aa$id"
            "bilibili" -> "https://www.bilibili.com/bangumi/media/md$id"
            "tucao" -> "http://www.tucao.tv/index.php?m=search&c=index&a=init2&q=$id"
            "sohu" -> "https://tv.sohu.com/$id"
            "youku" -> "https://list.youku.com/show/id_z$id.html"
            "tudou" -> "https://www.tudou.com/albumcover/$id.html"
            "qq" -> "https://v.qq.com/detail/$id.html"
            "iqiyi" -> "http://www.iqiyi.com/$id.html"
            "letv" -> "https://www.le.com/comic/$id.html"
            "pptv" -> "http://v.pptv.com/page/$id.html"
            "kankan" -> "http://movie.kankan.com/movie/$id"
            "mgtv" -> "https://www.mgtv.com/h/$id.html"
            "nicovideo" -> "http://ch.nicovideo.jp/$id"
            "netflix" -> "https://www.netflix.com/title/$id"

            "dmhy" -> "https://share.dmhy.org/topics/list?keyword=$id"
            "nyaa" -> "https://www.nyaa.se/?page=search&term=$id"
            else -> id
        }
    }
}