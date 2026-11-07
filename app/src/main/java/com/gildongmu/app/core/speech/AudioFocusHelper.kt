package com.gildongmu.app.core.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import com.gildongmu.app.core.log.AppLog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

enum class FocusChange { GAIN, LOSS, LOSS_TRANSIENT, LOSS_TRANSIENT_CAN_DUCK }

/**
 * 오디오 포커스 (DEV_GUIDE §7.2–7.3, 지시서 §5.4).
 * 게임 음성은 USAGE_MEDIA / CONTENT_TYPE_SPEECH 로 음성 세션 시작 시 AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK 을 1회 요청하고,
 * 세션 종료·일시정지·Suspend(백그라운드) 시 abandon 한다. 포커스 변경 콜백은 [changes] 로 내보내고
 * 처리(일시정지·재출제)는 GameCoordinator 가 한다.
 */
class AudioFocusHelper(context: Context) {

    private val audioManager = context.applicationContext.getSystemService(AudioManager::class.java)

    private val _hasFocus = MutableStateFlow(false)
    val hasFocus: StateFlow<Boolean> = _hasFocus.asStateFlow()

    private val _changes = MutableSharedFlow<FocusChange>(extraBufferCapacity = 8)
    val changes: SharedFlow<FocusChange> = _changes.asSharedFlow()

    private val listener = AudioManager.OnAudioFocusChangeListener { change ->
        val mapped = when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> FocusChange.GAIN
            AudioManager.AUDIOFOCUS_LOSS -> FocusChange.LOSS
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> FocusChange.LOSS_TRANSIENT
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> FocusChange.LOSS_TRANSIENT_CAN_DUCK
            else -> null
        } ?: return@OnAudioFocusChangeListener
        AppLog.i(TAG, "focus change: ${mapped.name}")
        _hasFocus.value = mapped == FocusChange.GAIN || mapped == FocusChange.LOSS_TRANSIENT_CAN_DUCK
        _changes.tryEmit(mapped)
    }

    private val request: AudioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        .setWillPauseWhenDucked(false)
        .setOnAudioFocusChangeListener(listener, Handler(Looper.getMainLooper()))
        .build()

    /** @return 포커스를 얻었으면 true. 거부되면 재생하지 않는다. */
    fun request(): Boolean {
        if (_hasFocus.value) return true
        val result = runCatching { audioManager?.requestAudioFocus(request) }.getOrNull()
        val granted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        AppLog.i(TAG, "request GAIN_TRANSIENT_MAY_DUCK (MEDIA/SPEECH) → ${if (granted) "GRANTED" else "FAILED($result)"}")
        _hasFocus.value = granted
        return granted
    }

    /**
     * @param force 일시 손실(LOSS_TRANSIENT)로 [hasFocus] 가 이미 false 여도 요청을 거둔다 — 다시 GAIN 을 받지 않을 때
     *  (화면 게임은 손실 뒤 저절로 이어 읽지 않으므로 요청을 남겨 둘 이유가 없다)
     */
    fun abandon(force: Boolean = false) {
        if (!_hasFocus.value && !force) return
        runCatching { audioManager?.abandonAudioFocusRequest(request) }
        AppLog.i(TAG, "abandon focus")
        _hasFocus.value = false
    }

    /** 포커스 손실 후 상태만 정리 (요청은 유지 — GAIN 을 다시 받기 위해) */
    fun markLost() {
        _hasFocus.value = false
    }

    companion object {
        private const val TAG = "AudioFocus"
    }
}
