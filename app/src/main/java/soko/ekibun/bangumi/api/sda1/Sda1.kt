package soko.ekibun.bangumi.api.sda1

import io.reactivex.Observable
import okhttp3.RequestBody
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.sda1.bean.Response
import soko.ekibun.bangumi.util.JsonUtil

/**
 * 流浪图库
 */
object Sda1 {

    /**
     * 上传图片
     * @param requestBody RequestBody
     * @param fileName String
     * @return Call<String>
     */
    fun uploadImage(requestBody: RequestBody, fileName: String): Observable<String> {
        return ApiHelper.createHttpObservable(
            url = "https://p.sda1.dev/api/v1/upload_external_noform?fileName=$fileName",
            body = requestBody
        ).map { rsp ->
            JsonUtil.toEntity<Response>(rsp.body?.string() ?: "")?.data?.url ?: ""
        }
    }
}