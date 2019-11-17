package soko.ekibun.bangumi.api.bangumi.bean

/**
 * 人物信息
 */
open class MonoInfo(
        val name: String? = null,
        val name_cn: String? = null,
        val images: Images? = null,
        val summary: String? = null,
        val url: String? = null
)