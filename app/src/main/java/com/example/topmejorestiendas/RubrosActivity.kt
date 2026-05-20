package com.example.topmejorestiendas

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.topmejorestiendas.databinding.ActivityRubrosBinding

class RubrosActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRubrosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRubrosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toRanking = {
            startActivity(Intent(this, RankingActivity::class.java))
        }

        binding.cardRestaurantes.setOnClickListener { toRanking() }
        binding.cardFerreterias.setOnClickListener { toRanking() }
        binding.cardCanchas.setOnClickListener { toRanking() }
        binding.cardFarmacias.setOnClickListener { toRanking() }
        binding.cardSupermercados.setOnClickListener { toRanking() }
        binding.cardTiendasRopa.setOnClickListener { toRanking() }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> {
                    startActivity(Intent(this, PerfilUsuarioActivity::class.java))
                    true
                }
                else -> true
            }
        }
    }
}