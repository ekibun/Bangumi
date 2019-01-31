package soko.ekibun.bangumi.api.github.bean

data class Release(
        val tag_name: String? = null,
        val assets: List<Release.Assets>? = null,
        val body: String? = null
){
    data class Assets(
            val name: String? = null,
            val download_count: String? = null,
            val browser_download_url: String? = null
    )
}