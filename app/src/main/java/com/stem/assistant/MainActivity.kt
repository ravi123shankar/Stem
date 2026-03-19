package com.stem.assistant

import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tvResult: TextView
    private lateinit var btnActivate: Button
    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnActivate = findViewById(R.id.btnActivate)
        tvResult = findViewById(R.id.tvResult)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                btnActivate.text = "Listening..."
                tvResult.text = "Speak now..."
                muteAllStreams()
            }
            override fun onResults(results: Bundle?) {
                unmuteAllStreams()
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val command = matches[0].lowercase().trim()
                    tvResult.text = "You said: ${matches[0]}"
                    btnActivate.text = "Activate Stem"
                    handleCommand(command)
                }
            }
            override fun onError(error: Int) {
                unmuteAllStreams()
                tvResult.text = "Sorry, I didn't catch that. Try again!"
                btnActivate.text = "Activate Stem"
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        btnActivate.setOnClickListener {
            startListening()
        }

        val btnDebug = findViewById<Button>(R.id.btnDebug)
        btnDebug.setOnClickListener {
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val packages = packageManager.queryIntentActivities(intent, 0)
            val appList = packages.take(50).map {
                it.loadLabel(packageManager).toString()
            }.joinToString("\n")
            tvResult.text = "Apps:\n$appList"
        }
    }
    private fun muteAllStreams() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_MUTE, 0)
        audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION,
            AudioManager.ADJUST_MUTE, 0)
        audioManager.adjustStreamVolume(AudioManager.STREAM_RING,
            AudioManager.ADJUST_MUTE, 0)
        audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM,
            AudioManager.ADJUST_MUTE, 0)
    }
    private fun unmuteAllStreams() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_UNMUTE, 0)
        audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION,
            AudioManager.ADJUST_UNMUTE, 0)
        audioManager.adjustStreamVolume(AudioManager.STREAM_RING,
            AudioManager.ADJUST_UNMUTE, 0)
        audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM,
            AudioManager.ADJUST_UNMUTE, 0)
    }
    private fun handleCommand(command: String) {
        when {
            command.contains("time") -> {
                val cal = Calendar.getInstance()
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val min = cal.get(Calendar.MINUTE)
                val ampm = if (hour >= 12) "PM" else "AM"
                val hour12 = if (hour % 12 == 0) 12 else hour % 12
                tvResult.text = "Current time: $hour12:${String.format("%02d", min)} $ampm"
            }
            command.contains("date") || command.contains("today") -> {
                val cal = Calendar.getInstance()
                val day = cal.get(Calendar.DAY_OF_MONTH)
                val month = cal.getDisplayName(Calendar.MONTH,
                    Calendar.LONG, java.util.Locale.getDefault())
                val year = cal.get(Calendar.YEAR)
                tvResult.text = "Today is: $day $month $year"
            }
            command.contains("alarm") -> {
                val numbers = Regex("\\d+").findAll(command).map {
                    it.value.toInt() }.toList()
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_MESSAGE, "Stem Alarm")
                    if (numbers.size >= 2) {
                        putExtra(AlarmClock.EXTRA_HOUR, numbers[0])
                        putExtra(AlarmClock.EXTRA_MINUTES, numbers[1])
                    } else if (numbers.size == 1) {
                        putExtra(AlarmClock.EXTRA_HOUR, numbers[0])
                        putExtra(AlarmClock.EXTRA_MINUTES, 0)
                    }
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                tvResult.text = "Setting alarm..."
            }
            command.contains("setting") -> {
                startActivity(Intent(Settings.ACTION_SETTINGS))
                tvResult.text = "Opening Settings..."
            }
            command.contains("home") -> {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
            command.contains("call") -> {
                val name = command.replace("call", "").trim()
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = android.net.Uri.parse("tel:")
                }
                startActivity(intent)
                tvResult.text = "Opening dialer for: $name"
            }
            command.contains("open") -> {
                val appName = command.replace("open", "").trim()
                val launched = launchApp(appName)
                if (!launched) {
                    tvResult.text = "App '$appName' not found on your phone"
                }
            }
            else -> {
                tvResult.text = "I heard: $command\nI don't understand this yet!"
            }
        }
    }
    private fun launchApp(appName: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val packages = packageManager.queryIntentActivities(intent, 0)
        for (pkg in packages) {
            val label = pkg.loadLabel(packageManager).toString().lowercase()
            if (label.replace(" ", "")
                    .contains(appName.replace(" ", "").lowercase())) {
                val launchIntent = packageManager.getLaunchIntentForPackage(
                    pkg.activityInfo.packageName)
                if (launchIntent != null) {
                    startActivity(launchIntent)
                    tvResult.text = "Opening ${pkg.loadLabel(packageManager)}..."
                    return true
                }
            }
        }
        return false
    }
    private fun startListening() {
        muteAllStreams()
        android.os.Handler(mainLooper).postDelayed({
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer.startListening(intent)
        }, 100)
    }
    override fun onDestroy() {
        super.onDestroy()
        unmuteAllStreams()
        speechRecognizer.destroy()
    }
}