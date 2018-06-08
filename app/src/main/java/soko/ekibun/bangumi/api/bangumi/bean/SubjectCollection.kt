package soko.ekibun.bangumi.api.bangumi.bean

data class SubjectCollection(
        var name: String? = null,
        var subject_id: Int = 0,
        var ep_status: Int = 0,
        var vol_status: Int = 0,
        var lasttouch: Int = 0,
        var subject: Subject? = null
) {

    /**
     * name : こみっくがーるず
     * subject_id : 217249
     * ep_status : 0
     * vol_status : 0
     * lasttouch : 1526093488
     * subject : {"id":217249,"url":"http://bgm.tv/subject/217249","type":2,"name":"こみっくがーるず","name_cn":"Comic Girls","summary":"","eps":12,"eps_count":12,"air_date":"2018-04-05","air_weekday":4,"images":{"large":"http://lain.bgm.tv/pic/cover/l/27/97/217249_nwcHE.jpg","common":"http://lain.bgm.tv/pic/cover/c/27/97/217249_nwcHE.jpg","medium":"http://lain.bgm.tv/pic/cover/m/27/97/217249_nwcHE.jpg","small":"http://lain.bgm.tv/pic/cover/s/27/97/217249_nwcHE.jpg","grid":"http://lain.bgm.tv/pic/cover/g/27/97/217249_nwcHE.jpg"},"collection":{"doing":758}}
     */
}