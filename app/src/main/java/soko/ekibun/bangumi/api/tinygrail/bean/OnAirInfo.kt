package soko.ekibun.bangumi.api.tinygrail.bean

data class OnAirInfo(
        val State: Int = 0,
        val Value: List<EpisodeOnAirInfo>? = null
){
    data class EpisodeOnAirInfo(
            val EpisodeId: Int = 0,
            val Id: Int = 0,
            val Link: String = "",
            val Name: String = "",
            val Site: String = ""
    )
}