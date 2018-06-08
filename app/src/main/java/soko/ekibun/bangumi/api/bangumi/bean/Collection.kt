package soko.ekibun.bangumi.api.bangumi.bean

data class Collection(
        val status : StatusBean? = null,
        val rating : Int = 0,
        val comment : String? = null,
        val private : Int = 0,
        val tag : List<String>? = null,
        val ep_status: Int = 0,
        val lasttouch: Int = 0
): BaseRequest(){
    data class StatusBean(
            val id : Int = 0,
            val type: String? = null,
            val name: String? = null
    )
}