package soko.ekibun.bangumi.ui.crash

import android.content.Context
import android.content.Intent
import android.view.View
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_crash.view.*
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.xxxlin.Xxxlin
import soko.ekibun.bangumi.api.xxxlin.bean.BaseResult
import soko.ekibun.bangumi.ui.splash.SplashActivity
import soko.ekibun.bangumi.ui.view.BaseFragmentActivity

/**
 * 错误报告
 * @property uploadCall Call<BaseResult>?
 */
class CrashActivity : BaseFragmentActivity(R.layout.fragment_crash) {

    private var uploadCall: Call<BaseResult>? = null
    override fun onViewCreated(view: View) {
        val content = intent.getStringExtra(EXTRA_CRASH) ?: ""
        view.item_content.text = content
        view.item_upload.setOnClickListener {
            uploadCall?.cancel()
            uploadCall = Xxxlin.createInstance().crashReport(content)
            uploadCall?.enqueue(ApiHelper.buildCallback({
                Snackbar.make(
                    view.item_upload,
                    if (it.code == 0) getString(R.string.crash_upload_ok) else getString(
                        R.string.crash_upload_failed,
                        it.msg ?: ""
                    ),
                    Snackbar.LENGTH_SHORT
                ).show()
                view.item_upload.setOnClickListener {}
            }, {}))
        }
        view.item_restart.setOnClickListener {
            SplashActivity.startActivity(this)
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    companion object {
        private const val EXTRA_CRASH = "extraCrash"
        /**
         * 启动activity
         * @param context Context
         * @param crash String
         */
        fun startActivity(context: Context, crash: String){
            val intent = Intent(context.applicationContext, CrashActivity::class.java)
            intent.putExtra(EXTRA_CRASH, crash)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
