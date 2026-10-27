package com.mgs.greenlionbooster

import android.app.Activity
import android.graphics.Color
import android.media.audiofx.AudioEffect
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

class MainActivity : Activity() {
    private var enhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private var gainDb = 4
    private var preset = 0
    private var running = false
    private lateinit var status: TextView
    private lateinit var gainLabel: TextView
    private lateinit var presetLabel: TextView
    private val presets = arrayOf("Громкость", "Басы", "Чистый голос")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 28, 36, 36)
            setBackgroundColor(Color.rgb(16, 20, 30))
        }
        fun label(value: String, size: Float = 16f): TextView =
            TextView(this).apply {
                text = value
                textSize = size
                setTextColor(Color.WHITE)
                setPadding(0, 12, 0, 12)
            }
        root.addView(label("GREEN LION BOOSTER", 26f))
        root.addView(label("CL-HP51 · Honor Android 15 · Bluetooth"))
        root.addView(label("Экспериментальная обработка системной аудиосессии. Работоспособность зависит от прошивки.", 14f))
        gainLabel = label("Усиление: +4 дБ", 19f)
        root.addView(gainLabel)
        root.addView(SeekBar(this).apply {
            max = 18
            progress = gainDb
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    gainDb = progress
                    gainLabel.text = "Усиление: +$progress дБ" +
                        if (progress > 9) " · осторожно: возможны искажения" else ""
                    applyGain()
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
        })
        presetLabel = label("Пресет: ${presets[preset]}", 19f)
        root.addView(presetLabel)
        presets.forEachIndexed { index, name ->
            root.addView(Button(this).apply {
                text = name
                setOnClickListener {
                    preset = index
                    presetLabel.text = "Пресет: ${presets[preset]}"
                    if (running) applyEqualizer()
                }
            })
        }
        status = label("Ожидание запуска")
        root.addView(status)
        root.addView(Button(this).apply {
            text = "Включить усиление"
            setOnClickListener { startEffects() }
        })
        root.addView(Button(this).apply {
            text = "Выключить"
            setOnClickListener {
                stopEffects()
                status.text = "Обработка отключена"
            }
        })
        root.addView(Button(this).apply {
            text = "Диагностика"
            setOnClickListener { diagnose() }
        })
        root.addView(label("Защита: начинай с низкой громкости. При хрипах, боли или звоне в ушах сразу уменьшай уровень. Приложение не может гарантировать аппаратное ограничение пиков и не обходит лимиты Bluetooth.", 14f))
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun applyGain() {
        try { enhancer?.setTargetGain(gainDb * 100) } catch (_: Exception) {}
    }

    private fun applyEqualizer() {
        val eq = equalizer ?: return
        try {
            val range = eq.bandLevelRange
            val min = range[0].toInt()
            val max = range[1].toInt()
            val bands = eq.numberOfBands.toInt()
            for (i in 0 until bands) {
                val frequencyHz = eq.getCenterFreq(i.toShort()) / 1000
                val db = when (preset) {
                    0 -> when { frequencyHz < 180 -> 0; frequencyHz < 1500 -> 1; else -> 2 }
                    1 -> when { frequencyHz < 180 -> 4; frequencyHz < 600 -> 2; else -> 0 }
                    else -> when { frequencyHz < 220 -> -3; frequencyHz < 1000 -> 0; frequencyHz < 5000 -> 3; else -> 1 }
                }
                eq.setBandLevel(i.toShort(), (db * 100).coerceIn(min, max).toShort())
            }
        } catch (e: Exception) {
            status.text = "Эквалайзер: ${e.javaClass.simpleName}: ${e.message}"
        }
    }

    private fun startEffects() {
        stopEffects()
        val messages = mutableListOf<String>()
        try {
            enhancer = LoudnessEnhancer(0).apply {
                setTargetGain(gainDb * 100)
                enabled = true
            }
            messages.add("LoudnessEnhancer: ${if (enhancer?.enabled == true) "активен" else "не активен"}")
        } catch (e: Exception) {
            messages.add("LoudnessEnhancer: ${e.javaClass.simpleName}: ${e.message}")
        }
        try {
            equalizer = Equalizer(0, 0).apply { enabled = true }
            applyEqualizer()
            messages.add("Equalizer: ${if (equalizer?.enabled == true) "активен" else "не активен"}")
        } catch (e: Exception) {
            messages.add("Equalizer: ${e.javaClass.simpleName}: ${e.message}")
        }
        running = enhancer != null || equalizer != null
        status.text = messages.joinToString("\n") +
            "\nВажно: статус эффекта не доказывает обработку звука сторонних приложений."
    }

    private fun diagnose() {
        val names = try { AudioEffect.queryEffects().map { it.name } } catch (_: Exception) { emptyList() }
        status.text = "Обнаружено эффектов: ${names.size}\n" +
            "LoudnessEnhancer: ${enhancer?.enabled ?: false}\n" +
            "Equalizer: ${equalizer?.enabled ?: false}\n" +
            "Сессия: 0 (экспериментальная)\n" +
            "Пресет: ${presets[preset]}\n" +
            "Доступны: " + names.filter {
                it.contains("loudness", true) || it.contains("equalizer", true) ||
                it.contains("dynamic", true) || it.contains("volume", true)
            }.joinToString(", ")
    }

    private fun stopEffects() {
        running = false
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
