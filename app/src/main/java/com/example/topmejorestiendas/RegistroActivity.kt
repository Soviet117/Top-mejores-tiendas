package com.example.topmejorestiendas

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.topmejorestiendas.databinding.ActivityRegistroBinding

class RegistroActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnUsuarioCliente.setOnClickListener {
            startActivity(Intent(this, RegistroUsuarioActivity::class.java))
        }

        binding.btnDuenoNegocio.setOnClickListener {
            startActivity(Intent(this, RegistroLocalActivity::class.java))
        }

        binding.tvBackToLogin.setOnClickListener {
            finish()
        }
    }
}