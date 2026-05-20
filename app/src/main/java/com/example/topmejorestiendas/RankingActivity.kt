package com.example.topmejorestiendas

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.topmejorestiendas.databinding.ActivityRankingBinding

class RankingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRankingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRankingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toDetail = {
            startActivity(Intent(this, DetalleLocalActivity::class.java))
        }

        binding.cardLocal1.setOnClickListener { toDetail() }
        binding.cardLocal2.setOnClickListener { toDetail() }
        binding.cardLocal3.setOnClickListener { toDetail() }
    }
}