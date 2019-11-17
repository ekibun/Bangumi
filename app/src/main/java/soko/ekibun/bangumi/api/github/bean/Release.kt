package soko.ekibun.bangumi.api.github.bean

/**
 * 版本信息
 */
data class Release(
        val tag_name: String? = null,
        val assets: List<Assets>? = null,
        val body: String? = null
){
    /**
     * 版本附件信息
     */
    data class Assets(
            val name: String? = null,
            val download_count: String? = null,
            val browser_download_url: String? = null
    )
}