package soko.ekibun.bangumi.parser

import android.support.annotation.IntDef

data class ParseInfo(
        var api: String?=null,
        var video: ParseItem ?= null,
        var danmaku: ParseItem ?= null
){
    data class ParseItem(
            @ParseType var type: Int = 0,
            var id: String = ""
    )

    companion object {
        @IntDef(IQIYI,YOUKU,PPTV,TENCENT)
        annotation class ParseType
        const val IQIYI = 0
        const val YOUKU = 1
        const val PPTV = 2
        const val TENCENT = 3
        const val DILIDLILI = 4
    }
}