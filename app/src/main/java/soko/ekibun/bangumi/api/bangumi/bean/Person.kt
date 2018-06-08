package soko.ekibun.bangumi.api.bangumi.bean

class Person(
        var id: Int = 0,
        var url: String? = null,
        var name: String? = null,
        var name_cn: String? = null,
        var role_name: String? = null,
        var images: Images? = null,
        var comment: Int = 0,
        var collects: Int = 0,
        var info: MonoInfo? = null,
        var jobs: List<String>? = null
) {
    /**
     * id : 681
     * url : http://bgm.tv/person/681
     * name : 青山剛昌
     * name_cn : 青山刚昌
     * role_name :
     * images : {"large":"http://lain.bgm.tv/pic/crt/l/15/95/681_prsn_anidb.jpg","medium":"http://lain.bgm.tv/pic/crt/m/15/95/681_prsn_anidb.jpg","small":"http://lain.bgm.tv/pic/crt/s/15/95/681_prsn_anidb.jpg","grid":"http://lain.bgm.tv/pic/crt/g/15/95/681_prsn_anidb.jpg"}
     * comment : 13
     * collects : 0
     * info : {"name_cn":"青山刚昌","alias":{"jp":"青山剛昌","kana":"あおやま　ごうしょう","romaji":"Aoyama Goushou"},"gender":"男","birth":"1963年6月21日"}
     * jobs : ["原作"]
     */
}