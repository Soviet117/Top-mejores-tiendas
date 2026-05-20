package com.example.topmejorestiendas

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.topmejorestiendas.databinding.ActivityRegistroUsuarioBinding

class RegistroUsuarioActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistroUsuarioBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroUsuarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegistrarUsuario.setOnClickListener {
            finish() // Regresa al login
        }

        binding.tvBackFromRegister.setOnClickListener {
            finish()
        }
    }
}