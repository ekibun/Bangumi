package soko.ekibun.bangumi.api.bangumi.bean

import androidx.annotation.StringDef

object ResponseGroup{
    const val LARGE = "large"
    const val MEDIUM = "medium"
    const val SMALL = "small"
    @StringDef(LARGE, MEDIUM, SMALL)
    annotation class ResponseGroup
}