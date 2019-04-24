package soko.ekibun.bangumi.api.bangumi.bean

data class UserInfo(
        var id: Int = 0,
        var url: String? = null,
        var username: String? = null,
        var nickname: String? = null,
        var avatar: Images? = null,
        var sign: String? = null,
        var formhash: String? = null,
        var needReload: Boolean = false,
        var notify: Pair<Int, Int>? = null
): BaseRequest(){
    /**
     * id : 419012
     * url : http://bgm.tv/user/419012
     * username : 419012
     * nickname : ekibun
     * avatar : {"large":"http://lain.bgm.tv/pic/user/l/icon.jpg","medium":"http://lain.bgm.tv/pic/user/m/icon.jpg","small":"http://lain.bgm.tv/pic/user/s/icon.jpg"}
     * formhash :
     */
}