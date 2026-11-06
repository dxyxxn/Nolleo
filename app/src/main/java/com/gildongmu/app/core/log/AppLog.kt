package com.gildongmu.app.core.log

import android.util.Log
import com.gildongmu.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 앱 공통 로거. logcat 출력 + 디버그 패널용 최근 로그 링버퍼.
 *
 * PII 규칙 (지시서 §9 보안): 인식된 발화 텍스트와 위치는 [sensitive] 로만 남기고,
 * release 빌드에서는 logcat·디버그 패널 모두 마스킹한다.
 */
object AppLog {
    private const val PREFIX = "Gildongmu/"
    private const val MAX_LINES = 60

    private val _recent = MutableStateFlow<List<String>>(emptyList())
    val recent: StateFlow<List<String>> = _recent.asStateFlow()

    private val timeFormat = ThreadLocal.withInitial { SimpleDateFormat("HH:mm:ss", Locale.KOREA) }

    /** 단위 테스트(JVM)에서는 android.util.Log 가 stub 이므로 끌 수 있게 한다. */
    @Volatile
    var logcatEnabled: Boolean = true

    fun i(tag: String, message: String) = write(Log.INFO, tag, message)

    fun w(tag: String, message: String, error: Throwable? = null) =
        write(Log.WARN, tag, if (error != null) "$message (${error.javaClass.simpleName}: ${error.message})" else message)

    fun e(tag: String, message: String, error: Throwable? = null) =
        write(Log.ERROR, tag, if (error != null) "$message (${error.javaClass.simpleName}: ${error.message})" else message)

    /** 발화 텍스트·좌표 등 민감 정보. debug 빌드에서만 원문을 남긴다. */
    fun sensitive(tag: String, label: String, value: String) {
        val shown = if (BuildConfig.DEBUG) value else "<redacted>"
        write(Log.DEBUG, tag, "$label=$shown")
    }

    private fun write(priority: Int, tag: String, message: String) {
        if (logcatEnabled) {
            runCatching { Log.println(priority, PREFIX + tag, message) }
        }
        val time = timeFormat.get()?.format(Date()) ?: ""
        val level = when (priority) {
            Log.ERROR -> "E"
            Log.WARN -> "W"
            Log.DEBUG -> "D"
            else -> "I"
        }
        val line = "$time $level $tag: $message"
        _recent.update { (it + line).takeLast(MAX_LINES) }
    }
}
