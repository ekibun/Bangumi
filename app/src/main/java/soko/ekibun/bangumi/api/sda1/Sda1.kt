package soko.ekibun.bangumi.api.sda1

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.RequestBody
import soko.ekibun.bangumi.api.sda1.bean.Response
import soko.ekibun.bangumi.util.HttpUtil
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
    suspend fun uploadImage(requestBody: RequestBody, fileName: String): String {
        return withContext(Dispatchers.IO) {
            JsonUtil.toEntity<Response>(
                HttpUtil.getCall(
                    url = "https://p.sda1.dev/api/v1/upload_external_noform?fileName=$fileName",
                    body = requestBody
                ).execute().body?.string() ?: ""
            )?.data?.url ?: ""
        }
    }
}