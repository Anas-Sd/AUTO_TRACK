package com.autotrack.app.assistant

import android.content.Intent
import android.speech.RecognitionService

class AutoTrackRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {}
    override fun onCancel(listener: Callback?) {}
    override fun onStopListening(listener: Callback?) {}
}
