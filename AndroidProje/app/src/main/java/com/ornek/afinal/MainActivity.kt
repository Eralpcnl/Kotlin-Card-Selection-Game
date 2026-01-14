package com.ornek.afinal

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity
import com.ornek.afinal.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val allSymbols = listOf("🐶", "🐱", "🐰", "🦊", "🐸", "🐵", "🐯", "🐼")

    private lateinit var cards: MutableList<String>
    private lateinit var buttons: MutableList<Button>

    private var firstSelectedIndex: Int? = null
    private var secondSelectedIndex: Int? = null

    private val handler = Handler(Looper.getMainLooper())

    private var matchedPairs = 0
    private var gameOver = false

    private var difficultyLevel = 4

    private var countdownTime: Int = 0
    private val countdownHandler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Beyaz arka planda status bar ikonlarını görünür yap
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Başlat butonu görünür başta
        binding.buttonStart.visibility = View.VISIBLE
        binding.buttonRestart.visibility = View.GONE

        binding.buttonStart.setOnClickListener {
            binding.buttonStart.visibility = View.GONE
            startLevel(difficultyLevel)
        }

        binding.buttonRestart.setOnClickListener {
            binding.buttonRestart.visibility = View.GONE
            difficultyLevel = 4
            startLevel(difficultyLevel)
        }
    }

    private fun startLevel(level: Int) {
        difficultyLevel = level
        setupGame()
        startCountdownTimer()
    }

    private fun setupGame() {
        val symbols = allSymbols.take(difficultyLevel)
        cards = (symbols + symbols).shuffled().toMutableList()

        buttons = mutableListOf()
        binding.gridLayout.removeAllViews()

        gameOver = false
        matchedPairs = 0
        firstSelectedIndex = null
        secondSelectedIndex = null
        binding.textViewStatus.text = "Kartları eşleştir! Seviye: $difficultyLevel"

        binding.gridLayout.columnCount = 4
        val rows = (cards.size + 3) / 4
        binding.gridLayout.rowCount = rows

        val displayMetrics = resources.displayMetrics
        val screenWidthPx = displayMetrics.widthPixels
        val density = displayMetrics.density

        val totalMarginDp = 32 * (binding.gridLayout.columnCount + 1)
        val totalMarginPx = (totalMarginDp * density).toInt()

        val cardWidth = (screenWidthPx - totalMarginPx) / binding.gridLayout.columnCount

        for (i in cards.indices) {
            val button = Button(this)
            val params = GridLayout.LayoutParams().apply {
                width = cardWidth
                height = cardWidth
                setMargins(
                    (16 * density).toInt(),
                    (16 * density).toInt(),
                    (16 * density).toInt(),
                    (16 * density).toInt()
                )
            }
            button.layoutParams = params
            button.textSize = 32f
            button.text = "❓"
            button.id = i

            button.setOnClickListener {
                onCardClicked(i)
            }

            buttons.add(button)
            binding.gridLayout.addView(button)
        }
    }

    private fun onCardClicked(index: Int) {
        if (gameOver) return
        if (firstSelectedIndex == index || secondSelectedIndex == index) return
        if (secondSelectedIndex != null) return

        val button = buttons[index]
        button.text = cards[index]

        if (firstSelectedIndex == null) {
            firstSelectedIndex = index
        } else {
            secondSelectedIndex = index

            if (cards[firstSelectedIndex!!] == cards[secondSelectedIndex!!]) {
                matchedPairs++
                binding.textViewStatus.text = "Doğru eşleştirme! Pairs: $matchedPairs / $difficultyLevel"

                firstSelectedIndex = null
                secondSelectedIndex = null

                if (matchedPairs == difficultyLevel) {
                    gameOver = true
                    binding.textViewStatus.text = "Tebrikler! Seviye $difficultyLevel tamamlandı."

                    countdownRunnable?.let { countdownHandler.removeCallbacks(it) }

                    // Seviye arttır ama max sınır allSymbols.size
                    difficultyLevel++
                    if (difficultyLevel > allSymbols.size) difficultyLevel = allSymbols.size

                    // 1 saniye sonra yeni seviyeyi başlat
                    handler.postDelayed({
                        setupGame()
                        startCountdownTimer()
                    }, 1000)
                }
            } else {
                binding.textViewStatus.text = "Yanlış eşleştirme, tekrar dene."

                handler.postDelayed({
                    buttons[firstSelectedIndex!!].text = "❓"
                    buttons[secondSelectedIndex!!].text = "❓"
                    firstSelectedIndex = null
                    secondSelectedIndex = null
                    binding.textViewStatus.text = "Kartları eşleştir! Seviye: $difficultyLevel"
                }, 1000)
            }
        }
    }

    private fun startCountdownTimer() {
        countdownRunnable?.let { countdownHandler.removeCallbacks(it) }

        // Süre = (8 - difficulty + 4) * 5 gibi, kolayda daha uzun, zorla kısalır
        countdownTime = (allSymbols.size - difficultyLevel + 4) * 5
        binding.textViewTimer.text = "Süre: $countdownTime sn"

        countdownRunnable = object : Runnable {
            override fun run() {
                if (countdownTime > 0 && !gameOver) {
                    countdownTime--
                    binding.textViewTimer.text = "Süre: $countdownTime sn"
                    countdownHandler.postDelayed(this, 1000)
                } else if (!gameOver) {
                    // Süre doldu
                    gameOver = true
                    binding.textViewStatus.text = "⏰ Süre doldu! Oyun bitti."
                    binding.buttonRestart.visibility = View.VISIBLE

                    countdownRunnable?.let { countdownHandler.removeCallbacks(it) }

                    // Süre dolunca seviye sıfırlanacak (başlangıç)
                    difficultyLevel = 4
                }
            }
        }
        countdownHandler.post(countdownRunnable!!)
    }
}
