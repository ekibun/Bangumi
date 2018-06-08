package soko.ekibun.bangumi.api

import android.app.Activity
import android.content.Context
import android.support.design.widget.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object ApiCallback {
    fun <T> build(context: Context?, callback: (T)->Unit, finish:()->Unit={}): Callback<T> {
        return object:Callback<T>{
            override fun onFailure(call: Call<T>?, t: Throwable?) {
                if(!t.toString().contains("Canceled") && context is Activity)
                    Snackbar.make(context.window.decorView, "出错啦\n" + t.toString(), Snackbar.LENGTH_SHORT).show()
                finish()
            }

            override fun onResponse(call: Call<T>, response: Response<T>) {
                finish()
                response.body()?.let { callback(it) }
            }
        }
    }
}