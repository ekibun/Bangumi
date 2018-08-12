package soko.ekibun.bangumi.api.tinygrail.bean

import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject

data class OnAir(
        var episode: Episode?,
        var subject: Subject,
        var siteList: List<Site>?
){
    data class Site(
            val label:String,
            val url:String,
            val active:Boolean
    )
}