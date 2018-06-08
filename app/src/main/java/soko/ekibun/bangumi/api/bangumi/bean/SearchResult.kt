package soko.ekibun.bangumi.api.bangumi.bean

data class SearchResult(
        var results: Int = 0,
        var list: List<Subject>? = null
): BaseRequest(){
    /**
     * results : 253
     * list : [{"id":6776,"url":"http://bgm.tv/subject/6776","type":2,"name":"名探偵コナン コナンvsキッドvsヤイバ 宝刀争奪大決戦!!","name_cn":"柯南对基德对铁剑 宝刀争夺大决战","summary":"","air_date":"","air_weekday":0,"images":{"large":"http://lain.bgm.tv/pic/cover/l/e5/ba/6776_bA2h2.jpg","common":"http://lain.bgm.tv/pic/cover/c/e5/ba/6776_bA2h2.jpg","medium":"http://lain.bgm.tv/pic/cover/m/e5/ba/6776_bA2h2.jpg","small":"http://lain.bgm.tv/pic/cover/s/e5/ba/6776_bA2h2.jpg","grid":"http://lain.bgm.tv/pic/cover/g/e5/ba/6776_bA2h2.jpg"}}]
     */
}