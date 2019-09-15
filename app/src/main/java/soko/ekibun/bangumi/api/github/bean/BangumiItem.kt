package soko.ekibun.bangumi.api.github.bean

/**
 * bangumi-data条目数据
 * @property title 番组原始标题
 * @property titleTranslate 番组标题翻译
 * @property type 番组类型
 * @property lang 番组语言
 * @property officialSite 官网
 * @property begin tv/web：番组开始时间；movie：上映日期；ova：首话发售时间
 * @property end tv/web：番组完结时间；movie：无意义；ova：则为最终话发售时间（未确定则置空）
 * @property comment 备注
 * @property sites 站点
 * @constructor
 */
data class BangumiItem(
        var title: String? = null,
        var titleTranslate: Map<String,List<String>>? = null,
        var type: String? = null,
        var lang: String? = null,
        var officialSite: String? = null,
        var begin: String? = null,
        var end: String? = null,
        var comment: String? = null,
        var sites: List<SitesBean>? = null
) {

    /**
     * 站点数据
     * @property site 站点名称
     * @property id 站点 id，可用于替换模板中相应的字段
     * @property url 如果当前url不符合urlTemplate中的规则时使用，优先级高于id
     * @property begin 放送开始时间
     * @property comment 备注
     * @constructor
     */
    data class SitesBean(
            var site: String? = null,
            var id: String? = null,
            var url: String? = null,
            var begin: String? = null,
            var comment: String? = null
    ) {

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

        /**
         * 按urlTemplate返回站点网址
         */
        fun parseUrl(): String{
            if(!url.isNullOrEmpty()) return url.toString()
            return when(site){
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
                else-> id?:""
            }
        }
    }
}
