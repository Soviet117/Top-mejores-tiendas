package com.example.topmejorestiendas

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.topmejorestiendas.databinding.ActivityRegistroLocalBinding

class RegistroLocalActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistroLocalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroLocalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegistrarLocal.setOnClickListener {
            finish() // Regresa al login
        }

        binding.tvBackFromLocal.setOnClickListener {
            finish()
        }
    }
}