package com.gildongmu.app.domain.engine

import com.gildongmu.app.core.Clock
import com.gildongmu.app.core.speech.SttEvent
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * 한 번의 응답 결과 (졸음 케어의 원재료, 지시서 §5.4).
 *  - t0     = 문제 TTS 의 onDone 시각 (호출자가 넘긴다)
 *  - tFirst = 첫 Partial(text != blank) 또는 SpeechDetected 중 먼저 온 것
 *  - latencyMs = tFirst - t0. 8초 안에 말이 없으면 latencyMs = 8000, text = null (느린 응답으로 집계)
 */
data class AnswerResult(
    val text: String?,
    val latencyMs: Long,
    val timedOut: Boolean,
    val error: String? = null,
    val fatalError: Boolean = false,
)

object AnswerCollector {
    const val FIRST_SPEECH_TIMEOUT_MS = 8_000L
    const val FINAL_GRACE_MS = 6_000L

    suspend fun collect(
        events: Flow<SttEvent>,
        t0: Long,
        clock: Clock,
        firstSpeechTimeoutMs: Long = FIRST_SPEECH_TIMEOUT_MS,
        finalGraceMs: Long = FINAL_GRACE_MS,
    ): AnswerResult {
        var tFirst: Long? = null
        var finalText: String? = null
        var lastPartial: String? = null
        var error: String? = null
        var fatal = false

        coroutineScope {
            val collector = launch {
                events.collect { event ->
                    when (event) {
                        is SttEvent.Listening -> Unit
                        is SttEvent.SpeechDetected -> if (tFirst == null) tFirst = event.tMs
                        is SttEvent.Partial -> {
                            if (event.text.isNotBlank()) {
                                if (tFirst == null) tFirst = event.tMs
                                lastPartial = event.text
                            }
                        }
                        is SttEvent.Final -> {
                            if (event.text.isNotBlank()) {
                                if (tFirst == null) tFirst = event.tMs
                                finalText = event.text
                            }
                            cancel()
                        }
                        SttEvent.Timeout -> cancel()
                        is SttEvent.Error -> {
                            error = event.reason
                            fatal = event.fatal
                            cancel()
                        }
                    }
                }
            }
            val watchdog = launch {
                delay(firstSpeechTimeoutMs)
                if (tFirst == null) {
                    collector.cancel()
                } else {
                    delay(finalGraceMs)
                    collector.cancel()
                }
            }
            collector.join()
            watchdog.cancel()
        }

        val text = finalText ?: lastPartial
        val first = tFirst
        return if (text.isNullOrBlank() || first == null) {
            AnswerResult(text = null, latencyMs = firstSpeechTimeoutMs, timedOut = error == null, error = error, fatalError = fatal)
        } else {
            AnswerResult(text = text, latencyMs = (first - t0).coerceAtLeast(0), timedOut = false)
        }
    }
}
