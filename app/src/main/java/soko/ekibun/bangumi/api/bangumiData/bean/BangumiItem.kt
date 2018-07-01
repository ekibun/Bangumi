package soko.ekibun.bangumi.api.bangumiData.bean

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
     * title : はねバド！
     * titleTranslate : {"zh-Hans":["轻羽飞扬"]}
     * type : tv
     * lang : ja
     * officialSite : http://hanebad.com/
     * begin : 2018-07-01T15:00:00.000Z
     * end :
     * comment :
     * sites : [{"site":"bangumi","id":"236590"},{"site":"bilibili","id":"24589","begin":"","official":true,"premuiumOnly":false,"censored":null,"exist":false,"comment":""}]
     */

    data class SitesBean(
            var site: String? = null,
            var id: String? = null,
            var begin: String? = null,
            var isOfficial: Boolean? = false,
            var isPremuiumOnly: Boolean? = false,
            var censored: Boolean? = null,
            var isExist: Boolean? = false,
            var comment: String? = null
    ) {
        /**
         * site : bangumi
         * id : 236590
         * begin :
         * official : true
         * premuiumOnly : false
         * censored : null
         * exist : false
         * comment :
         */

        fun color(): Int{
            return (0xff000000 + when(site){
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
                else-> 0
            }).toInt()
        }
        /*

    .avfun {
        background-color: #fd4c5b;
    }

    .bilibili {
        background-color: #24ace6;
    }

    .hulu {
        background-color: #2bbd99;
    }

    .iqiyi {
        background-color: #00be06;
    }

    .letv {
        background-color: #e42112;
    }

    .netflix {
        background-color: #e50914;
    }

    .niconico {
        background-color: #060102;
    }

    .pptv {
        background-color: #00a0e9;
    }

    .sohu {
        background-color: #d6000a;
    }

    .tencent {
        background-color: #ff820f;
    }

    .youku {
        background-color: #1ebeff;
    }

    .youtube {
        background-color: #ff0000;
    }
         */

        fun parseUrl(): String{
            return when(site){
                "bangumi" -> "http://bangumi.tv/subject/$id"
                "saraba1st" -> "https://bbs.saraba1st.com/2b/thread-$id-1-1.html"

                "acfun" -> "http://www.acfun.cn/v/ab$id"
                "bilibili" -> "https://bangumi.bilibili.com/anime/$id"
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
