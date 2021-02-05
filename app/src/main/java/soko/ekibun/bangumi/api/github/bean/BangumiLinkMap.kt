package soko.ekibun.bangumi.api.github.bean

data class BangumiLinkMap(
    val id: Int,
    val node: List<BangumiLinkSubject>?,
    val relate: List<BangumiLinkRelate>?
) {
    data class BangumiLinkSubject(
        val id: Int,
        val name: String,
        val nameCN: String,
        val image: String,
        var visit: Boolean? = null
    )

    data class BangumiLinkRelate(
        val relate: String,
        val src: Int,
        val dst: Int,
    )
}
