package soko.ekibun.bangumi.api.uploadcc.bean

data class Response(
        var code: Int = 0,
        var total_success: Int = 0,
        var total_error: Int = 0,
        var success_image: List<DataBean>? = null
) {

    /**
    {"code":100,"total_success":1,"total_error":0,
    "success_image":[{"name":"a291a6d847474ae09ca7249efdfea65f.gif","url":"i1\/2019\/06\/03\/JkipGQ.gif","thumbnail":"i1\/2019\/06\/03\/JkipGQb.jpg","delete":"7$2y$10$cZalTTI5kFOqOnC96Tj\/Lu10NLJdmFQL12guRMj2yB6OJj9bcASPm5"}]}
     */

    data class DataBean(
            var name: String? = null,
            var url: String? = null,
            var thumbnail: String? = null,
            var delete: String? = null
    ) {
        /**
        {"name":"a291a6d847474ae09ca7249efdfea65f.gif","url":"i1\/2019\/06\/03\/JkipGQ.gif","thumbnail":"i1\/2019\/06\/03\/JkipGQb.jpg","delete":"7$2y$10$cZalTTI5kFOqOnC96Tj\/Lu10NLJdmFQL12guRMj2yB6OJj9bcASPm5"}
         */
    }
}