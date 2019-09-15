package soko.ekibun.bangumi.api.bangumi.bean

import soko.ekibun.bangumi.api.bangumi.Bangumi

class Person(
        var id: Int = 0,
        var name: String? = null,
        var name_cn: String? = null,
        var images: Images? = null
) {
    val url = "${Bangumi.SERVER}/person/$id"
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