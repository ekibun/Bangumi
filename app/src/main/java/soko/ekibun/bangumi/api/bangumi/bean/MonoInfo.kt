package soko.ekibun.bangumi.api.bangumi.bean

/**
 * 人物信息
 * @property name String?
 * @property name_cn String?
 * @property image String?
 * @property summary String?
 * @property url String?
 * @constructor
 */
open class MonoInfo(
    val name: String? = null,
    val name_cn: String? = null,
    val image: String? = null,
    val summary: String? = null,
    val url: String? = null
)