package soko.ekibun.bangumi.api.tinygrail.bean

import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject

data class OnAir(
        val episode: Episode,
        val subject: Subject,
        val siteList: List<Site>
){
    data class Site(
            val label:String,
            val url:String,
            val active:Boolean
    )
}