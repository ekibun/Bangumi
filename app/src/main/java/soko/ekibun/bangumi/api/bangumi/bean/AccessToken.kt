package soko.ekibun.bangumi.api.bangumi.bean

data class AccessToken(
        var access_token: String? = null,
        var expires_in: Int = 0,
        var token_type: String? = null,
        var scope: String? = null,
        var refresh_token: String? = null,
        var user_id: Int = 0
): BaseRequest()