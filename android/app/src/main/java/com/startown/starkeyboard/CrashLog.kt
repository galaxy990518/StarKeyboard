package com.startown.starkeyboard

import android.app.Application
import android.content.Context
import com.startown.starkeyboard.ime.BuildInfo
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** App 和鍵盤一啟動就裝好當機紀錄 */
class StarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
    }
}

/** 當機或出錯時把原因寫到檔案，App 首頁會顯示出來，方便截圖回報 */
object CrashLog {
    private const val MAX_SIZE = 20_000

    fun file(context: Context) = File(context.filesDir, "crash.txt")

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            record(app, error, "當機")
            previous?.uncaughtException(thread, error)
        }
    }

    /** 記下一個錯誤（當機，或是被接住、鍵盤沒關掉的錯誤） */
    fun record(context: Context, error: Throwable, kind: String = "出錯") {
        runCatching {
            val f = file(context)
            val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
            val time = SimpleDateFormat("MM/dd HH:mm:ss", Locale.TAIWAN).format(Date())
            val entry = "【$kind】$time 第 ${BuildInfo.current(context)} 版\n$trace\n"
            val old = if (f.exists()) f.readText() else ""
            f.writeText((entry + old).take(MAX_SIZE))
        }
    }
}
