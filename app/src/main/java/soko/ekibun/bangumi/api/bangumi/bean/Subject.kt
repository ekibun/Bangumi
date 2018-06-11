package soko.ekibun.bangumi.api.bangumi.bean

import com.google.gson.annotations.SerializedName


data class Subject(
        var id: Int = 0,
        var url: String? = null,
        var type: Int = 0,
        var name: String? = null,
        var name_cn: String? = null,
        var summary: String? = null,
        var eps_count: Int = 0,
        var air_date: String? = null,
        var air_weekday: Int = 0,
        var rating: RatingBean? = null,
        var rank: Int = 0,
        var images: Images? = null,
        var collection: CollectionBean? = null,
        var eps: Any? = null,
        var crt: List<Character>? = null,
        var staff: List<Person>? = null,
        var topic: List<TopicBean>? = null,
        var blog: List<BlogBean>? = null
){
    data class BlogBean(
            var id: Int = 0,
            var url: String? = null,
            var title: String? = null,
            var summary: String? = null,
            var image: String? = null,
            var replies: Int = 0,
            var timestamp: Int = 0,
            var dateline: String? = null,
            var user: UserInfo? = null
    ){
        /**
         * id : 273281
         * url : http://bgm.tv/blog/273281
         * title : 度没掌握好，方向也有偏离
         * summary : 度没掌握好，方向也有偏离。
         * 前面真的很棒，乡村微奇幻卖萌搞笑片，小町偶尔的卖蠢衬托出乡村少女的天真。
         * 可是随着片子的进展，（staff）欺负小町越来越过分，尤其是赤裸裸的表现出小町农村人的无知和自卑（被迫害妄想症），看着真是令人替她着急啊。
         * 如果说这些是为片子最后升华 ...
         * image :
         * replies : 7
         * timestamp : 1466485111
         * dateline : 2016-6-21 04:58
         * user : {"id":205577,"url":"http://bgm.tv/user/drawing","username":"drawing","nickname":"千叶铁矢","avatar":{"large":"http://lain.bgm.tv/pic/user/l/000/20/55/205577.jpg?r=1410168526","medium":"http://lain.bgm.tv/pic/user/m/000/20/55/205577.jpg?r=1410168526","small":"http://lain.bgm.tv/pic/user/s/000/20/55/205577.jpg?r=1410168526"},"sign":null}
         */
    }

    data class TopicBean(
            var id: Int = 0,
            var url: String? = null,
            var title: String? = null,
            var main_id: Int = 0,
            var timestamp: Int = 0,
            var lastpost: Int = 0,
            var replies: Int = 0,
            var user: UserInfo? = null
    ) {
        /**
         * id : 1
         * url : http://bgm.tv/subject/topic/1
         * title : 拿这个来测试
         * main_id : 1
         * timestamp : 1216020847
         * lastpost : 1497657984
         * replies : 57
         * user : {"id":2,"url":"http://bgm.tv/user/2","username":"2","nickname":"陈永仁","avatar":{"large":"http://lain.bgm.tv/pic/user/l/000/00/00/2.jpg?r=1322480632","medium":"http://lain.bgm.tv/pic/user/m/000/00/00/2.jpg?r=1322480632","small":"http://lain.bgm.tv/pic/user/s/000/00/00/2.jpg?r=1322480632"},"sign":null}
         */
    }

    data class RatingBean (
            var total: Int = 0,
            var count: CountBean? = null,
            var score: Double = 0.toDouble()
    ){
        /**
         * total : 3069
         * count : {"10":331,"9":335,"8":907,"7":977,"6":391,"5":97,"4":19,"3":3,"2":2,"1":7}
         * score : 7.6
         */

        data class CountBean(
                @SerializedName("10")
                var `_$10`: Int = 0,
                @SerializedName("9")
                var `_$9`: Int = 0,
                @SerializedName("8")
                var `_$8`: Int = 0,
                @SerializedName("7")
                var `_$7`: Int = 0,
                @SerializedName("6")
                var `_$6`: Int = 0,
                @SerializedName("5")
                var `_$5`: Int = 0,
                @SerializedName("4")
                var `_$4`: Int = 0,
                @SerializedName("3")
                var `_$3`: Int = 0,
                @SerializedName("2")
                var `_$2`: Int = 0,
                @SerializedName("1")
                var `_$1`: Int = 0
        ) {
            /**
             * 10 : 331
             * 9 : 335
             * 8 : 907
             * 7 : 977
             * 6 : 391
             * 5 : 97
             * 4 : 19
             * 3 : 3
             * 2 : 2
             * 1 : 7
             */
        }
    }

    data class CollectionBean (
            var wish: Int = 0,
            var collect: Int = 0,
            var doing: Int = 0,
            var on_hold: Int = 0,
            var dropped: Int = 0
    ){
        /**
         * wish : 171
         * collect : 2011
         * doing : 1880
         * on_hold : 941
         * dropped : 273
         */
    }
}