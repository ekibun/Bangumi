package soko.ekibun.bangumi.ui.main.fragment.index

import android.support.v7.widget.PopupMenu
import android.widget.TextView
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.SubjectType

class IndexTypeView(view: TextView, onChange:()->Unit){
    companion object {
        val typeList = mapOf(
                R.id.collection_type_anime_all to Pair(SubjectType.NAME_ANIME, ""),
                R.id.collection_type_anime_tv to Pair(SubjectType.NAME_ANIME, "tv"),
                R.id.collection_type_anime_web to Pair(SubjectType.NAME_ANIME, "web"),
                R.id.collection_type_anime_ova to Pair(SubjectType.NAME_ANIME, "ova"),
                R.id.collection_type_anime_movie to Pair(SubjectType.NAME_ANIME, "movie"),
                R.id.collection_type_anime_misc to Pair(SubjectType.NAME_ANIME, "misc"),
                R.id.collection_type_book_all to Pair(SubjectType.NAME_BOOK, ""),
                R.id.collection_type_book_comic to Pair(SubjectType.NAME_BOOK, "comic"),
                R.id.collection_type_book_novel to Pair(SubjectType.NAME_BOOK, "novel"),
                R.id.collection_type_book_illustration to Pair(SubjectType.NAME_BOOK, "illustration"),
                R.id.collection_type_book_misc to Pair(SubjectType.NAME_BOOK, "misc"),
                R.id.collection_type_game_all to Pair(SubjectType.NAME_GAME, ""),
                R.id.collection_type_game_pc to Pair(SubjectType.NAME_GAME, "pc"),
                R.id.collection_type_game_mac to Pair(SubjectType.NAME_GAME, "mac"),
                R.id.collection_type_game_ps4 to Pair(SubjectType.NAME_GAME, "ps4"),
                R.id.collection_type_game_xbox_one to Pair(SubjectType.NAME_GAME, "xbox_one"),
                R.id.collection_type_game_ns to Pair(SubjectType.NAME_GAME, "ns"),
                R.id.collection_type_game_wii_u to Pair(SubjectType.NAME_GAME, "wii_u"),
                R.id.collection_type_game_ps3 to Pair(SubjectType.NAME_GAME, "ps3"),
                R.id.collection_type_game_xbox360 to Pair(SubjectType.NAME_GAME, "xbox360"),
                R.id.collection_type_game_wii to Pair(SubjectType.NAME_GAME, "wii"),
                R.id.collection_type_game_psv to Pair(SubjectType.NAME_GAME, "psv"),
                R.id.collection_type_game_3ds to Pair(SubjectType.NAME_GAME, "3ds"),
                R.id.collection_type_game_iphone to Pair(SubjectType.NAME_GAME, "iphone"),
                R.id.collection_type_game_android to Pair(SubjectType.NAME_GAME, "android"),
                R.id.collection_type_game_arc to Pair(SubjectType.NAME_GAME, "arc"),
                R.id.collection_type_game_nds to Pair(SubjectType.NAME_GAME, "nds"),
                R.id.collection_type_game_psp to Pair(SubjectType.NAME_GAME, "psp"),
                R.id.collection_type_game_ps2 to Pair(SubjectType.NAME_GAME, "ps2"),
                R.id.collection_type_game_xbox to Pair(SubjectType.NAME_GAME, "xbox"),
                R.id.collection_type_game_gamecube to Pair(SubjectType.NAME_GAME, "gamecube"),
                R.id.collection_type_game_dreamcast to Pair(SubjectType.NAME_GAME, "dreamcast"),
                R.id.collection_type_game_n64 to Pair(SubjectType.NAME_GAME, "n64"),
                R.id.collection_type_game_ps to Pair(SubjectType.NAME_GAME, "ps"),
                R.id.collection_type_game_sfc to Pair(SubjectType.NAME_GAME, "sfc"),
                R.id.collection_type_game_fc to Pair(SubjectType.NAME_GAME, "fc"),
                R.id.collection_type_game_ws to Pair(SubjectType.NAME_GAME, "ws"),
                R.id.collection_type_game_wsc to Pair(SubjectType.NAME_GAME, "wsc"),
                R.id.collection_type_game_ngp to Pair(SubjectType.NAME_GAME, "ngp"),
                R.id.collection_type_game_GBA to Pair(SubjectType.NAME_GAME, "GBA"),
                R.id.collection_type_game_vb to Pair(SubjectType.NAME_GAME, "vb"),
                R.id.collection_type_music to Pair(SubjectType.NAME_MUSIC, ""),
                R.id.collection_type_real_all to Pair(SubjectType.NAME_REAL, ""),
                R.id.collection_type_real_jp to Pair(SubjectType.NAME_REAL, "jp"),
                R.id.collection_type_real_en to Pair(SubjectType.NAME_REAL, "en"),
                R.id.collection_type_real_cn to Pair(SubjectType.NAME_REAL, "cn"))
    }
    var selectedType = R.id.collection_type_anime_all

    init{
        val popup = PopupMenu(view.context, view)
        popup.menuInflater.inflate(R.menu.list_browser_type, popup.menu)
        view.text = popup.menu.findItem(selectedType)?.title?.toString()?.replace("全部", "")
        view.setOnClickListener {
            popup.setOnMenuItemClickListener{
                if(typeList.containsKey(it.itemId)){
                    selectedType = it.itemId
                    view.text = it.title.toString().replace("全部", "")
                    onChange()
                }
                true
            }
            popup.show()
        }
    }
}