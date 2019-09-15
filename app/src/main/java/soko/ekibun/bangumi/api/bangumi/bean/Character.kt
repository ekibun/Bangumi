package soko.ekibun.bangumi.api.bangumi.bean

import soko.ekibun.bangumi.api.bangumi.Bangumi

data class Character(
        var id: Int = 0,
        var name: String? = null,
        var name_cn: String? = null,
        var role_name: String? = null,
        var images: Images? = null,
        var comment: Int = 0,
        var collects: Int = 0,
        var info: MonoInfo? = null,
        var actors: List<Person>? = null
){
    val url = "${Bangumi.SERVER}/character/$id"
    /**
     * id : 3453
     * url : http://bgm.tv/character/3453
     * name : 江戸川コナン
     * name_cn : 江户川柯南
     * role_name : 主角
     * images : {"large":"http://lain.bgm.tv/pic/crt/l/e7/f9/3453_crt_836v3.jpg?r=1444795097","medium":"http://lain.bgm.tv/pic/crt/m/e7/f9/3453_crt_836v3.jpg?r=1444795097","small":"http://lain.bgm.tv/pic/crt/s/e7/f9/3453_crt_836v3.jpg?r=1444795097","grid":"http://lain.bgm.tv/pic/crt/g/e7/f9/3453_crt_836v3.jpg?r=1444795097"}
     * comment : 29
     * collects : 96
     * info : {"name_cn":"江户川柯南","alias":{"0":"バーロー","jp":"江戸川コナン","kana":"えどがわこなん","romaji":"Edogawa Conan","nick":"柯南"},"gender":"男","birth":"5月4日","bloodtype":"A型","height":"121CM","weight":"18kg"}
     * actors : [{"id":3933,"url":"http://bgm.tv/person/3933","name":"高山みなみ","images":{"large":"http://lain.bgm.tv/pic/crt/l/29/8f/3933_seiyu_anidb.jpg","medium":"http://lain.bgm.tv/pic/crt/m/29/8f/3933_seiyu_anidb.jpg","small":"http://lain.bgm.tv/pic/crt/s/29/8f/3933_seiyu_anidb.jpg","grid":"http://lain.bgm.tv/pic/crt/g/29/8f/3933_seiyu_anidb.jpg"}}]
     */
}