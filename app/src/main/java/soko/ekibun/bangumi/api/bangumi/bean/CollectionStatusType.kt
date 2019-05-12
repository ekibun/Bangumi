package soko.ekibun.bangumi.api.bangumi.bean

import androidx.annotation.StringDef
import soko.ekibun.bangumi.R

object CollectionStatusType{
    const val WISH = "wish"
    const val COLLECT = "collect"
    const val DO = "do"
    const val ON_HOLD="on_hold"
    const val DROPPED = "dropped"
    @StringDef(WISH, COLLECT, DO, ON_HOLD, DROPPED)
    annotation class CollectionStatusType
    val status=arrayOf(WISH, COLLECT, DO, ON_HOLD, DROPPED)

    fun getTypeNamesResId(@SubjectType.SubjectType type: Int): Int{
        return when(type){
            SubjectType.BOOK -> R.array.collection_status_book
            SubjectType.MUSIC -> R.array.collection_status_music
            SubjectType.GAME -> R.array.collection_status_game
            SubjectType.REAL -> R.array.collection_status_real
            else -> R.array.collection_status_anime
        }
    }
}