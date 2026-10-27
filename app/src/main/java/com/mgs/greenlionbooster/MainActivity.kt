package com.mgs.greenlionbooster

import android.app.Activity
import android.media.audiofx.AudioEffect
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import android.graphics.Color
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.ScrollView

class MainActivity : Activity() {
    private var enhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private lateinit var status: TextView
    private lateinit var gainText: TextView
    private var gain = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 40, 32, 24)
            setBackgroundColor(Color.rgb(16, 20, 30))
        }
        fun label(value: String, size: Float = 17f): TextView =
            TextView(this).apply { text = value; textSize = size; setTextColor(Color.WHITE); setPadding(0, 12, 0, 12) }
        root.addView(label("Green Lion Audio Booster", 25f))
        root.addView(label("Honor Android 15 · Bluetooth · экспериментальный режим"))
        status = label("Глобальная обработка пока не активирована")
        root.addView(status)
        gainText = label("Усиление: 0 дБ")
        root.addView(gainText)
        root.addView(SeekBar(this).apply {
            max = 12
            progress = 0
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    gain = progress
                    gainText.text = "Усиление: +$progress дБ"
                    try { enhancer?.setTargetGain(progress * 100) } catch (_: Exception) {}
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
        })
        root.addView(Button(this).apply {
            text = "Попробовать системное усиление"
            setOnClickListener { startEffects() }
        })
        root.addView(Button(this).apply {
            text = "Выключить усиление"
            setOnClickListener { stopEffects(); status.text = "Выключено" }
        })
        root.addView(Button(this).apply {
            text = "Проверить аудиоэффекты"
            setOnClickListener {
                val effects = AudioEffect.queryEffects()
                status.text = "Доступно эффектов: ${effects.size}\n" +
                    effects.joinToString("\n") { it.name }
            }
        })
        root.addView(label("Важно: Android может запретить обработку session 0. Успешное создание эффекта не подтверждает усиление YouTube, Spotify и других приложений.", 14f))
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun startEffects() {
        stopEffects()
        val results = mutableListOf<String>()
        try {
            enhancer = LoudnessEnhancer(0).apply {
                setTargetGain(gain * 100)
                enabled = true
            }
            results.add("LoudnessEnhancer запущен")
        } catch (e: Exception) {
            results.add("LoudnessEnhancer: ${e.javaClass.simpleName}: ${e.message}")
        }
        try {
            equalizer = Equalizer(0, 0).apply { enabled = true }
            results.add("Equalizer запущен")
        } catch (e: Exception) {
            results.add("Equalizer: ${e.javaClass.simpleName}: ${e.message}")
        }
        status.text = results.joinToString("\n") + "\nПроверь громкость на слух на низком уровне."
    }

    private fun stopEffects() {
        try { enhancer?.enabled = false; enhancer?.release() } catch (_: Exception) {}
        try { equalizer?.enabled = false; equalizer?.release() } catch (_: Exception) {}
        enhancer = null
        equalizer = null
    }

    override fun onDestroy() {
        stopEffects()
        super.onDestroy()
    }
}
