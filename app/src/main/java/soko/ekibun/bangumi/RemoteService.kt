package soko.ekibun.bangumi

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * 远程服务（plugin）
 */
class RemoteService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        (application as App).remoteAction(intent, flags, startId)
        return super.onStartCommand(intent, flags, startId)
    }
}
