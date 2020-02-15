package soko.ekibun.bangumi.api.github.bean

/**
 * 版本信息
 * @property tag_name String?
 * @property assets List<Assets>?
 * @property body String?
 * @constructor
 */
data class Release(
        val tag_name: String? = null,
        val assets: List<Assets>? = null,
        val body: String? = null
){
    /**
     * 版本附件信息
     * @property name String?
     * @property download_count String?
     * @property browser_download_url String?
     * @constructor
     */
    data class Assets(
            val name: String? = null,
            val download_count: String? = null,
            val browser_download_url: String? = null
    )
}