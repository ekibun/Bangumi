package soko.ekibun.bangumi.api.bangumi.bean

import android.annotation.SuppressLint
import android.support.annotation.IntDef
import android.support.annotation.StringDef
import soko.ekibun.bangumi.R

object SubjectType{
    const val ALL = 0
    const val BOOK = 1
    const val ANIME = 2
    const val MUSIC = 3
    const val GAME = 4
    const val REAL = 6
    @IntDef(ALL, BOOK, ANIME, MUSIC, GAME, REAL)
    annotation class SubjectType

    const val NAME_BOOK = "book"
    const val NAME_ANIME = "anime"
    const val NAME_MUSIC = "music"
    const val NAME_GAME = "game"
    const val NAME_REAL = "real"
    @StringDef(NAME_BOOK,NAME_ANIME,NAME_MUSIC,NAME_GAME,NAME_REAL)
    annotation class SubjectTypeName

    val typeNameMap = mapOf(
            ANIME to NAME_ANIME,
            MUSIC to NAME_MUSIC,
            GAME to NAME_GAME,
            REAL to NAME_REAL,
            BOOK to NAME_BOOK
    )

    @SuppressLint("SwitchIntDef")
    fun getDescription(@SubjectType type: Int): Int{
        return when (type) {
            BOOK -> R.string.book
            ANIME -> R.string.anime
            MUSIC -> R.string.music
            GAME ->  R.string.game
            REAL ->  R.string.real
            else -> R.string.subject
        }
    }
}