package soko.ekibun.bangumi.util

import android.app.Application
import android.util.Log
import soko.ekibun.bangumi.ui.crash.CrashActivity
import java.io.PrintWriter
import java.io.StringWriter

/**
 * 崩溃拦截
 * @property application Application
 * @property mDefaultHandler UncaughtExceptionHandler?
 * @constructor
 */
class CrashHandler(private val application: Application) : Thread.UncaughtExceptionHandler {
    private val mDefaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()

    private fun writeCrash(exc: Throwable): String {
        val sb = StringBuffer()
        val writer = StringWriter()
        val pw = PrintWriter(writer)
        exc.printStackTrace(pw)
        var excCause: Throwable? = exc.cause
        while (excCause != null) {
            excCause.printStackTrace(pw)
            excCause = excCause.cause
        }
        pw.close()
        val result = writer.toString()
        sb.append(result)
        return sb.toString()
    }

    override fun uncaughtException(thread: Thread, exc: Throwable) {
        Log.e("crash", Log.getStackTraceString(exc))
        val content = writeCrash(exc)
        if (content.isEmpty() && mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, exc)
        } else {
            CrashActivity.startActivity(application, content)
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}