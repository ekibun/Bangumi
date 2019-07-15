package soko.ekibun.bangumi.api.github.bean

data class OnAirInfo (
        val id: Int,
        val name: String,
        val eps: List<Ep>
){
    data class Ep(
            val id: Int,
            val name: String,
            val name_cn: String,
            val sites: List<Site>
    )
    data class Site(
            val site: String,
            val title: String,
            val url: String
    )
}