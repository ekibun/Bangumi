package soko.ekibun.bangumi.ui.splash

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import soko.ekibun.bangumi.ui.main.MainActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MainActivity.startActivity(this)
        finish()
    }
}
