package soko.ekibun.bangumi.model

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.google.gson.reflect.TypeToken
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.parser.ParseInfo
import soko.ekibun.bangumi.util.JsonUtil

class ParseInfoModel(context: Context){
    val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }

    fun saveInfo(subject: Subject, info: ParseInfo) {
        val editor = sp.edit()
        val list = JsonUtil.toEntity<Map<Int, ParseInfo>>(sp.getString(PREF_PARSE_INFO, JsonUtil.toJson(HashMap<Int, ParseInfo>())), object: TypeToken<Map<Int, ParseInfo>>(){}.type).toMutableMap()
        list[subject.id] = info
        editor.putString(PREF_PARSE_INFO, JsonUtil.toJson(list))
        editor.apply()
    }

    fun getInfo(subject: Subject): ParseInfo?{
        return JsonUtil.toEntity<Map<Int, ParseInfo>>(sp.getString(PREF_PARSE_INFO, JsonUtil.toJson(HashMap<Int, ParseInfo>())), object: TypeToken<Map<Int, ParseInfo>>(){}.type)[subject.id]
    }

    companion object {
        const val PREF_PARSE_INFO="parseInfo"
    }
}