package soko.ekibun.bangumi.api.smms.bean

data class Response(
    var success: Boolean = false,
    var code: String? = null,
    var data: DataBean? = null
) {

    data class DataBean(
        var url: String? = null
    )
}