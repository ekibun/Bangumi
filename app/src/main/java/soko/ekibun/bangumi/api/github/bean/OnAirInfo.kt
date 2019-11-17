package soko.ekibun.bangumi.api.github.bean

/**
 * 放送信息
 */
data class OnAirInfo (
        val id: Int,
        val name: String,
        val eps: List<Ep>
){
    /**
     * 剧集信息
     */
    data class Ep(
            val id: Int,
            val name: String,
            val name_cn: String,
            val sites: List<Site>
    )

    /**
     * 站点信息
     */
    data class Site(
            val site: String,
            val title: String,
            val url: String
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
    }
}