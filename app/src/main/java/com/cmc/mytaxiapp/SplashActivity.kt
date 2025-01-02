package com.cmc.mytaxiapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        // Délai avant de passer à l'activité principale
        val splashScreenDuration = 3000L // 3 secondes

        window.decorView.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish() // Pour fermer l'activité splash
        }, splashScreenDuration)
    }
}
