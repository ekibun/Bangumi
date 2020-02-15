package soko.ekibun.bangumi.api.uploadcc.bean

/**
 * uploadcc返回数据
 * @property code Int
 * @property total_success Int
 * @property total_error Int
 * @property success_image List<DataBean>?
 * @constructor
 */
data class Response(
        var code: Int = 0,
        var total_success: Int = 0,
        var total_error: Int = 0,
        var success_image: List<DataBean>? = null
) {

    /**
     * 图片链接
     * @property name String?
     * @property url String?
     * @property thumbnail String?
     * @property delete String?
     * @constructor
     */
    data class DataBean(
            var name: String? = null,
            var url: String? = null,
            var thumbnail: String? = null,
            var delete: String? = null
    )
}