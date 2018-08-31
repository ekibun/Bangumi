package soko.ekibun.bangumi.util

import android.app.*
import android.os.Build
import android.util.Log
import java.io.*
import soko.ekibun.bangumi.ui.crash.CrashActivity

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
        Log.w("Crash", exc.message)
        val content = writeCrash(exc)
        if (content.isEmpty() && mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, exc)
        } else {
            CrashActivity.startActivity(application, content)
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}