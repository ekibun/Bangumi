package soko.ekibun.bangumi.api.bangumi.bean

import android.support.annotation.StringDef

object CollectionStatusType{
    const val WISH = "wish"
    const val COLLECT = "collect"
    const val DO = "do"
    const val ON_HOLD="on_hold"
    const val DROPPED = "dropped"
    @StringDef(WISH, COLLECT, DO, ON_HOLD, DROPPED)
    annotation class CollectionStatusType
    val status=arrayOf(WISH, COLLECT, DO, ON_HOLD, DROPPED)
}