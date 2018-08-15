package soko.ekibun.bangumi.api.bangumi.bean

data class Rakuen(
        val img: String,
        val topic: String,
        val group: String?,
        val time: String,
        val plus: String?,
        val url: String,
        val groupUrl: String?
)