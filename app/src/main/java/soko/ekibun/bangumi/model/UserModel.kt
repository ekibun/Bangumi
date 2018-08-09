package soko.ekibun.bangumi.model

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import soko.ekibun.bangumi.api.bangumi.bean.AccessToken
import soko.ekibun.bangumi.util.JsonUtil

class UserModel(context: Context){
    val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }

    fun saveToken(token: AccessToken?) {
        val editor = sp.edit()
        if(token == null){
            editor.remove(PREF_TOKEN)
        }else{
            editor.putString(PREF_TOKEN, JsonUtil.toJson(token))
        }
        editor.apply()
    }

    fun getToken():AccessToken?{
        try{
            return JsonUtil.toEntity(sp.getString(PREF_TOKEN, "")!!, AccessToken::class.java)
        }catch (e: Exception){ }
        return null
    }

    companion object {
        const val PREF_TOKEN="token"
    }
}