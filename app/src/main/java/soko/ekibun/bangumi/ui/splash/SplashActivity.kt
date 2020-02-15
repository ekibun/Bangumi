package soko.ekibun.bangumi.ui.splash

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import soko.ekibun.bangumi.ui.main.MainActivity

/**
 * 欢迎Activity
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MainActivity.startActivity(this)
        finish()
    }

    companion object {
        /**
         * 启动
         * @param context Context
         */
        fun startActivity(context: Context){
            val intent = Intent(context.applicationContext, SplashActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
