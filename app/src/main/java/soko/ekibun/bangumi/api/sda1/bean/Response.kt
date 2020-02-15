package soko.ekibun.bangumi.api.sda1.bean

/**
 * 图片上传返回数据
 * @property success Boolean
 * @property code String?
 * @property data DataBean?
 * @constructor
 */
data class Response(
    var success: Boolean = false,
    var code: String? = null,
    var data: DataBean? = null
) {

    /**
     * 图片数据
     * @property url String?
     * @property delete_url String?
     * @constructor
     */
    data class DataBean(
        var url: String? = null,
        var delete_url: String? = null
    )
}